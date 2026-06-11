package com.thingclips.sdk.aistream.ai.call

import com.thingclips.sdk.aistream.ai.AiIdentityConfig
import com.thingclips.sdk.aistream.ai.chat.AudioAmplitudeView
import com.thingclips.sdk.aistream.ai.chat.ChatAdapter
import com.thingclips.sdk.aistream.ai.chat.ChatMessage

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.thingclips.sdk.aistream.AudioAmplitudesCallback
import com.thingclips.sdk.aistream.ConnectCallback
import com.thingclips.sdk.aistream.EventStartCallback
import com.thingclips.sdk.aistream.IThingAiStream
import com.thingclips.sdk.aistream.SessionCallback
import com.thingclips.sdk.aistream.ThingAiStream
import com.thingclips.sdk.aistream.ThingAiStreamConstant
import com.thingclips.sdk.aistream.ThingAiStreamListener
import com.thingclips.sdk.aistream.audio.AudioDetectManager
import com.thingclips.sdk.aistream.audio.AudioPlayCallback
import com.thingclips.sdk.aistream.bean.RecordParams
import com.thingclips.sdk.aistream.business.AgentTokenRequestParams
import com.thingclips.sdk.aistream.helper.EventStartOptions
import com.thingclips.smart.android.aistream.Constants
import com.thingclips.smart.android.aistream.ThingStreamManager
import com.thingclips.smart.android.aistream.data.StreamAudio
import com.thingclips.smart.android.aistream.data.StreamEvent
import com.thingclips.smart.android.aistream.data.StreamFile
import com.thingclips.smart.android.aistream.data.StreamImage
import com.thingclips.smart.android.aistream.data.StreamText
import com.thingclips.smart.android.aistream.data.StreamVideo
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Hands-free "phone call" engine in cloud long-event mode, mirroring the
 * production AiFreeLongVadEngine / devkit AiLongEventEngine configuration:
 *
 *  - One session, one long event with enableVad + enableInterrupt: the cloud
 *    does VAD segmentation and barge-in, so no local VAD/AEC tflite models
 *    are required (RecordParams only enables ANC + rnnoise).
 *  - Audio streams continuously into the long event while the mic is open.
 *  - The cloud drives the dialog via events: START marks a new NLG reply,
 *    CHAT_BREAK interrupts the playing reply, 1004 suspends audio uplink.
 *
 * App identity connects via connectWithApp; device identity connects via
 * connectWithDevice(devId) and adds deviceId to the agent token request.
 */
class AiCallEngine(
    private val identity: Int,
    private val devId: String?,
    private val ownerId: String,
    private val aiSolutionCode: String,
    private val miniProgramId: String,
    private val listener: CallListener
) {

    companion object {
        private const val TAG = "ai_stream_Call"
        const val AMPLITUDES_LENGTH = 50
        private const val API_GET_TOKEN = "m.life.ai.token.get"
        private const val API_VERSION = "1.0"
        private const val AUDIO_SUSPEND_EVENT_TYPE = 1004
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val aiStream: IThingAiStream = ThingAiStream.newInstance()

    private var sessionId: String? = null

    @Volatile
    private var currentEventId: String? = null

    @Volatile
    private var currentNlgEventId: String? = null

    @Volatile
    private var isCreatingEvent = false

    /** Mic switch: audio capture keeps running, uplink pauses when false. */
    @Volatile
    private var isMicOpen = false

    private var isAudioInit = false

    /** Cloud asked us to pause the uplink (event 1004). Mic toggle resumes. */
    private val isSuspendAudioUplink = AtomicBoolean(false)

    var currentState: State = State.IDLE
        private set

    init {
        ThingStreamManager.getInstance().enableDebugLog(true)
    }

    enum class State {
        IDLE, CONNECTING, CREATING_SESSIONS, READY, LISTENING, ERROR
    }

    enum class PlaybackState {
        STARTED, FINISHED, ERROR
    }

    interface CallListener {
        fun onStateChanged(newState: State)
        fun onListeningStarted()
        fun onListeningStopped()
        fun onNlgInterrupted()
        fun onAsrResult(text: String, bizId: String)
        fun onNlgResult(eventId: String?, json: String)
        fun onAudioAmplitudeUpdate(amplitudes: DoubleArray)
        fun onAudioPlaybackStateChanged(state: PlaybackState, code: Int, msg: String?)
        fun onError(errorMessage: String)
    }

    private val isAppIdentity: Boolean
        get() = identity == AiIdentityConfig.IDENTITY_APP

    // --- Public API ---

    fun connect() {
        aiStream.setStreamListener(aiStreamListener)
        val connected = if (isAppIdentity) {
            aiStream.isConnected(Constants.ClientType.APP, null)
        } else {
            aiStream.isConnected(Constants.ClientType.DEVICE, devId)
        }
        if (connected) {
            createSession()
            return
        }
        setState(State.CONNECTING)
        val callback = object : ConnectCallback {
            override fun onSuccess(id: String) { /* Handled by listener */ }

            override fun onError(code: Int, error: String) {
                setState(State.ERROR)
                listener.onError("Connect failed: $error")
            }
        }
        if (isAppIdentity) {
            aiStream.connectWithApp(callback)
        } else {
            aiStream.connectWithDevice(devId ?: "", callback)
        }
    }

    /** Opens the mic: creates the long event on first use, then just resumes. */
    fun startListening() {
        if (currentState != State.READY) return

        if (currentEventId.isNullOrEmpty()) {
            createLongEvent()
        } else {
            resumeUplink()
        }
    }

    /** Closes the mic (pauses uplink); capture and the long event stay alive. */
    fun stopListeningAndStopPlayAudio(
        needListening: Boolean = false,
        needStopPlayAudio: Boolean = false
    ) {
        if (needStopPlayAudio) {
            // Break the in-flight reply, not just the local playback.
            interruptNlgResponse()
        }
        if (currentState != State.LISTENING) return

        Log.i(TAG, "Pausing call uplink (mic closed).")
        isMicOpen = false
        setState(State.READY)
        listener.onListeningStopped()
        if (needListening) {
            mainHandler.post { startListening() }
        }
    }

    fun destroy() {
        isMicOpen = false
        interruptNlgResponse()
        closeLongEvent()
        if (isAudioInit) {
            AudioDetectManager.getInstance().stopRecord()
        }
        AudioDetectManager.getInstance().destroyDetector()
        sessionId?.let { aiStream.closeSession(it, null) }
        sessionId = null
        aiStream.unregisterRecordAmplitudesCallback()
        aiStream.destroy()
        mainHandler.removeCallbacksAndMessages(null)
        audioHandlerThread.quitSafely()
    }

    // --- State and session management ---

    private fun setState(newState: State) {
        if (currentState != newState) {
            currentState = newState
            listener.onStateChanged(newState)
        }
    }

    private fun createSession() {
        if (!sessionId.isNullOrEmpty()) {
            setState(State.READY)
            startListening()
            return
        }
        setState(State.CREATING_SESSIONS)
        aiStream.createSession(buildAgentParams(), null, object : SessionCallback {
            override fun onError(errorCode: Int, errorMessage: String) {
                Log.e(TAG, "Session creation error: $errorMessage")
                setState(State.ERROR)
                listener.onError("Session creation failed: $errorMessage")
            }

            override fun onSuccess(
                sessionId: String,
                sendDataChannels: Map<String, Int>,
                revDataChannels: Map<String, Int>
            ) {
                this@AiCallEngine.sessionId = sessionId
                Log.i(TAG, "Call session created: $sessionId")
                setState(State.READY)
                startListening()
            }
        })
    }

    // --- Long event lifecycle ---

    private fun createLongEvent() {
        if (isCreatingEvent || !currentEventId.isNullOrEmpty()) {
            Log.w(TAG, "Long event already active or creating, skipping.")
            return
        }
        isCreatingEvent = true

        val session = sessionId
        if (session.isNullOrEmpty()) {
            Log.e(TAG, "Session ID is null, cannot start long event.")
            isCreatingEvent = false
            createSession()
            return
        }
        // Cloud-side VAD + barge-in: the one event lives for the whole call.
        val options = EventStartOptions.Builder(session)
            .enableVad(true)
            .enableInterrupt(true)
            .build()
        aiStream.sendEventStart(options, object : EventStartCallback {
            override fun onSuccess(eventId: String) {
                Log.i(TAG, "Long event started: $eventId")
                currentEventId = eventId
                isCreatingEvent = false
                startAudioCapture()
            }

            override fun onError(errorCode: Int, errorMessage: String) {
                isCreatingEvent = false
                Log.e(TAG, "Failed to start long event: $errorMessage")
                listener.onError("Error: Could not start call event.")
            }
        })
    }

    private fun closeLongEvent() {
        val eventToClose = currentEventId ?: return
        currentEventId = null
        currentNlgEventId = null
        sessionId?.let {
            Log.i(TAG, "Closing long event: $eventToClose")
            aiStream.sendEventEnd(eventToClose, it, null, null)
        }
    }

    private fun startAudioCapture() {
        if (!isAudioInit) {
            aiStream.registerRecordAmplitudesCallback(AMPLITUDES_LENGTH, audioAmplitudesCallback)
            // No local VAD/AEC models — the cloud segments speech; the
            // recorder only applies ANC + rnnoise (production parity).
            val params = RecordParams.Builder().sampleRate(16000)
                .enableANC(true)
                .enableRnnoise(true)
                .ancLevel(2)
                .build()
            AudioDetectManager.getInstance().startDetector(params, audioDetectionListener)
            isAudioInit = true
        }
        resumeUplink()
    }

    private fun resumeUplink() {
        isMicOpen = true
        isSuspendAudioUplink.set(false)
        setState(State.LISTENING)
        listener.onListeningStarted()
    }

    /**
     * Actively breaks the agent's in-flight reply: stops local playback and
     * sends a chat-break event for the current NLG event so the cloud stops
     * generating/streaming it (production stopNlgRecord parity).
     */
    private fun interruptNlgResponse() {
        aiStream.stopPlayAudio()
        val eventToBreak = currentNlgEventId ?: return
        currentNlgEventId = null
        Log.i(TAG, "Sending chat break for NLG event: $eventToBreak")
        sessionId?.let {
            aiStream.sendEventChatBreak(eventToBreak, it, null, null)
        }
        listener.onNlgInterrupted()
    }

    // --- Data parsing ---

    private fun parseAndProcessText(sessionId: String?, jsonText: String) {
        if (sessionId != this.sessionId) return
        try {
            val jsonObject = JSONObject(jsonText)
            val bizType = jsonObject.optString("bizType")
            val bizId = jsonObject.optString("bizId")
            val eof = jsonObject.optInt("eof", 0)
            val data = jsonObject.optJSONObject("data") ?: return

            when {
                "ASR".equals(bizType, ignoreCase = true) -> {
                    val asrText = data.optString("text", "").trim()
                    // The user talking over the agent breaks its reply.
                    if (asrText.isNotEmpty()) {
                        interruptNlgResponse()
                    }
                    if (asrText.isNotEmpty() && eof == 1) {
                        listener.onAsrResult(asrText, bizId)
                    }
                }

                "NLG".equals(bizType, ignoreCase = true) -> {
                    listener.onNlgResult(currentNlgEventId ?: bizId, jsonText)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON text: $jsonText", e)
        }
    }

    // --- Callbacks and listeners ---

    private val aiStreamListener = object : ThingAiStreamListener {
        override fun onConnectStateChanged(id: String, state: Int, code: Int) {
            if (state == Constants.ConnectState.CONNECTED) {
                createSession()
            } else {
                setState(State.IDLE)
                sessionId = null
                currentEventId = null
            }
        }

        override fun onSessionStateChanged(sessionId: String, state: Int, code: Int) {
            Log.d(TAG, "Session $sessionId state changed to $state")
            if (state == Constants.SessionState.CLOSED_BY_SERVER ||
                state == Constants.SessionState.AGENT_TOKEN_EXPIRED
            ) {
                listener.onError("Session $sessionId closed. Re-initializing.")
                setState(State.IDLE)
                this@AiCallEngine.sessionId = null
                currentEventId = null
                createSession()
            }
        }

        override fun onTextReceived(textData: StreamText) {
            if (textData.text.isNullOrEmpty()) return
            parseAndProcessText(textData.sessionId, textData.text)
        }

        override fun onVideoReceived(data: StreamVideo) {}

        override fun onAudioReceived(audioData: StreamAudio) {
            if (audioData.streamFlag == Constants.StreamFlag.START &&
                audioData.sessionId == sessionId
            ) {
                audioHandler.post {
                    aiStream.startPlayAudio(audioData, audioPlayCallback)
                }
            }
        }

        override fun onEventReceived(event: StreamEvent) {
            if (event.sessionId != sessionId) return
            Log.i(TAG, "Event ${event.eventId} type ${event.eventType} received.")

            when (event.eventType) {
                // Cloud pauses the uplink while the agent replies; toggling
                // the mic (or the existing LISTENING state) resumes it.
                AUDIO_SUSPEND_EVENT_TYPE -> {
                    Log.i(TAG, "Audio uplink suspended by cloud: ${event.eventId}")
                    isSuspendAudioUplink.set(true)
                }

                Constants.EventType.START -> {
                    // Each NLG reply is announced with a START event.
                    currentNlgEventId = event.eventId
                }

                Constants.EventType.CHAT_BREAK -> {
                    Log.i(TAG, "Chat break from cloud: ${event.eventId}")
                    if (currentNlgEventId == event.eventId) {
                        currentNlgEventId = null
                    }
                    aiStream.stopPlayAudio()
                    listener.onNlgInterrupted()
                }

                Constants.EventType.END -> {
                    if (event.eventId == currentNlgEventId) {
                        Log.i(TAG, "NLG event ${event.eventId} finished.")
                        currentNlgEventId = null
                    }
                    // Reply finished: lift any uplink suspension.
                    isSuspendAudioUplink.set(false)
                }
            }
        }

        override fun onFileReceived(data: StreamFile) {}
        override fun onImageReceived(data: StreamImage) {}
    }

    // Dedicated thread serializing audio playback control.
    private val audioHandlerThread = HandlerThread("AiCallAudioThread").apply { start() }
    private val audioHandler = Handler(audioHandlerThread.looper)

    private val audioDetectionListener = object : AudioDetectManager.AudioDetectionListener {
        override fun onVoiceDetected() {}

        override fun onStreamAudioData(streamAudio: StreamAudio) {
            if (!isMicOpen || isSuspendAudioUplink.get()) return
            val session = sessionId ?: return
            if (currentEventId.isNullOrEmpty()) return
            aiStream.sendAudioData(session, streamAudio, null)
        }

        override fun onVoiceData(voice: ByteArray?, voiceLength: Int, pcmType: Int) {
            if (!isMicOpen) return
            if (voice != null && voiceLength > 0) {
                handleAudioAmplitudes(voice)
            }
        }

        override fun onVoiceEnd() {}

        override fun onVoiceDetectError(error: Int, errorMessage: String?) {
            Log.e(TAG, "Audio capture error: $errorMessage")
            mainHandler.post {
                listener.onError("Audio capture error: $errorMessage")
                stopListeningAndStopPlayAudio()
            }
        }
    }

    private fun handleAudioAmplitudes(voice: ByteArray) {
        val frequencyMagnitudes = ThingStreamManager.getInstance().getFrequencyMagnitudesNative(
            voice,
            0,
            voice.size,
            Constants.AudioSampleRate.SAMPLE_RATE_16000,
            ThingAiStreamConstant.DEFAULT_BIT_DEPTH,
            ThingAiStreamConstant.DEFAULT_FFT_SIZE,
            50
        )
        audioAmplitudesCallback.onSuccess(frequencyMagnitudes)
    }

    private val audioAmplitudesCallback = object : AudioAmplitudesCallback {
        override fun onSuccess(amplitudes: DoubleArray?) {
            if (amplitudes != null) {
                listener.onAudioAmplitudeUpdate(amplitudes)
            }
        }

        override fun onError(code: Int, msg: String) {
            Log.w(TAG, "Amplitude callback error: $msg")
        }
    }

    private val audioPlayCallback = object : AudioPlayCallback {
        override fun onPlayStart() {
            listener.onAudioPlaybackStateChanged(PlaybackState.STARTED, 0, null)
        }

        override fun onPlayFinish() {
            listener.onAudioPlaybackStateChanged(PlaybackState.FINISHED, 0, null)
        }

        override fun onPlayError(code: Int, msg: String) {
            listener.onAudioPlaybackStateChanged(PlaybackState.ERROR, code, msg)
        }
    }

    // --- Helper methods ---

    private fun buildAgentParams(): AgentTokenRequestParams {
        val extParams = mutableMapOf<String, String>()
        extParams["miniProgramId"] = miniProgramId
        extParams["needTts"] = "true"
        if (!isAppIdentity && !devId.isNullOrEmpty()) {
            extParams["deviceId"] = devId
        }

        return AgentTokenRequestParams.Builder()
            .api(API_GET_TOKEN)
            .apiVersion(API_VERSION)
            .ownerId(ownerId)
            .aiSolutionCode(aiSolutionCode)
            .addExtParams(extParams)
            .build()
    }
}
