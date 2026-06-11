package com.thingclips.sdk.aistream.ai

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tuya.appsdk.sample.user.R
import org.json.JSONException
import org.json.JSONObject

/**
 * Phone-call style hands-free chat ("打电话"), driven by [AiCallEngine]:
 * VAD picks up speech automatically, ASR captions show as user bubbles,
 * the streamed NLG reply shows as agent bubbles and plays back as TTS.
 * Speaking over the agent interrupts it. Extras: identity (+ devId for
 * device identity), ownerId, aiSolutionCode, miniProgramId.
 */
class AiCallActivity : AppCompatActivity(), AiCallEngine.CallListener {

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }

    private lateinit var tvStatus: TextView
    private lateinit var ivMicToggle: ImageView
    private lateinit var audioAmplitudeView: AudioAmplitudeView
    private lateinit var chatAdapter: ChatAdapter
    private val messageList = mutableListOf<ChatMessage>()

    private var engine: AiCallEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ai_activity_call)

        val identity = intent.getIntExtra(
            AiIdentityConfig.EXTRA_IDENTITY, AiIdentityConfig.IDENTITY_DEVICE
        )
        val devId = intent.getStringExtra("devId")
        val ownerId = intent.getStringExtra("ownerId") ?: ""
        val aiSolutionCode = intent.getStringExtra("aiSolutionCode") ?: ""
        val miniProgramId = intent.getStringExtra("miniProgramId") ?: ""
        if (ownerId.isEmpty() || aiSolutionCode.isEmpty() || miniProgramId.isEmpty() ||
            (identity == AiIdentityConfig.IDENTITY_DEVICE && devId.isNullOrEmpty())
        ) {
            Toast.makeText(this, "Missing call parameters", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<ImageView>(R.id.iv_back).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.iv_hang_up).setOnClickListener { finish() }
        tvStatus = findViewById(R.id.tv_status)
        ivMicToggle = findViewById(R.id.iv_mic_toggle)
        audioAmplitudeView = findViewById(R.id.audio_amplitude_view)
        ivMicToggle.setOnClickListener { toggleListening() }

        val rv = findViewById<RecyclerView>(R.id.rv_chat_messages)
        chatAdapter = ChatAdapter(this, messageList)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = chatAdapter

        engine = AiCallEngine(
            identity, devId, ownerId, aiSolutionCode, miniProgramId, this
        )
        if (checkAndRequestAudioPermission()) {
            engine?.connect()
        }
    }

    private fun toggleListening() {
        val engine = engine ?: return
        when (engine.currentState) {
            AiCallEngine.State.READY -> engine.startListening()
            AiCallEngine.State.LISTENING -> engine.stopListeningAndStopPlayAudio(
                needListening = false, needStopPlayAudio = true
            )

            else -> Toast.makeText(this, R.string.ai_call_status_connecting, Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun updateUiForState(state: AiCallEngine.State) {
        tvStatus.setText(
            when (state) {
                AiCallEngine.State.IDLE -> R.string.ai_call_status_idle
                AiCallEngine.State.CONNECTING -> R.string.ai_call_status_connecting
                AiCallEngine.State.CREATING_SESSIONS -> R.string.ai_call_status_creating
                AiCallEngine.State.READY -> R.string.ai_call_status_ready
                AiCallEngine.State.LISTENING -> R.string.ai_call_status_listening
                AiCallEngine.State.ERROR -> R.string.ai_call_status_error
            }
        )

        when (state) {
            AiCallEngine.State.LISTENING -> {
                ivMicToggle.isEnabled = true
                ivMicToggle.setImageResource(R.drawable.ai_ic_mic)
                ivMicToggle.setColorFilter(ContextCompat.getColor(this, R.color.ai_primary))
                audioAmplitudeView.visibility = View.VISIBLE
            }

            AiCallEngine.State.READY -> {
                ivMicToggle.isEnabled = true
                ivMicToggle.setImageResource(R.drawable.ai_ic_mic)
                ivMicToggle.clearColorFilter()
                audioAmplitudeView.visibility = View.INVISIBLE
            }

            else -> {
                ivMicToggle.isEnabled = false
                ivMicToggle.setImageResource(R.drawable.ai_ic_mic_off)
                ivMicToggle.clearColorFilter()
                audioAmplitudeView.visibility = View.INVISIBLE
            }
        }
    }

    private fun addMessage(message: ChatMessage) {
        if (message.text.isNullOrEmpty() && message.imageUri == null) return
        messageList.add(message)
        chatAdapter.notifyItemInserted(messageList.size - 1)
        findViewById<RecyclerView>(R.id.rv_chat_messages)
            .scrollToPosition(messageList.size - 1)
    }

    // --- Lifecycle and permission ---

    override fun onPause() {
        super.onPause()
        engine?.stopListeningAndStopPlayAudio(needListening = false, needStopPlayAudio = false)
    }

    override fun onDestroy() {
        super.onDestroy()
        engine?.destroy()
        engine = null
    }

    private fun checkAndRequestAudioPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                engine?.connect()
            } else {
                Toast.makeText(this, R.string.ai_call_mic_permission, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // --- AiCallEngine.CallListener ---

    override fun onStateChanged(newState: AiCallEngine.State) {
        runOnUiThread { updateUiForState(newState) }
    }

    override fun onListeningStarted() {}

    override fun onListeningStopped() {}

    override fun onNlgInterrupted() {
        runOnUiThread {
            addMessage(
                ChatMessage(
                    text = "—— ${getString(R.string.ai_call_interrupted)} ——",
                    isSentByUser = false,
                    messageType = ChatMessage.MessageType.NLG_TEXT
                )
            )
        }
    }

    override fun onAsrResult(text: String, bizId: String) {
        runOnUiThread {
            addMessage(
                ChatMessage(
                    text = text,
                    isSentByUser = true,
                    messageType = ChatMessage.MessageType.VOICE_TO_TEXT,
                    bizId = bizId
                )
            )
        }
    }

    override fun onNlgResult(eventId: String?, json: String) {
        runOnUiThread {
            try {
                val jsonObject = JSONObject(json)
                val data = jsonObject.optJSONObject("data") ?: return@runOnUiThread
                val bizId = jsonObject.optString("bizId")
                val content = data.optString("content")
                if (content.isEmpty()) return@runOnUiThread
                val appendMode = data.optString("appendMode", "")
                if ("append".equals(appendMode, ignoreCase = true)) {
                    val index = messageList.indexOfLast {
                        !it.isSentByUser && it.bizId == bizId &&
                            it.messageType == ChatMessage.MessageType.NLG_TEXT
                    }
                    if (index != -1) {
                        messageList[index].text += content
                        chatAdapter.notifyItemChanged(index)
                        findViewById<RecyclerView>(R.id.rv_chat_messages)
                            .scrollToPosition(messageList.size - 1)
                        return@runOnUiThread
                    }
                }
                addMessage(
                    ChatMessage(
                        text = content,
                        isSentByUser = false,
                        messageType = ChatMessage.MessageType.NLG_TEXT,
                        bizId = bizId
                    )
                )
            } catch (e: JSONException) {
                // Malformed chunk, drop it.
            }
        }
    }

    override fun onAudioAmplitudeUpdate(amplitudes: DoubleArray) {
        runOnUiThread {
            if (audioAmplitudeView.visibility == View.VISIBLE &&
                amplitudes.size == AiCallEngine.AMPLITUDES_LENGTH
            ) {
                audioAmplitudeView.setAmplitudes(amplitudes)
            }
        }
    }

    override fun onAudioPlaybackStateChanged(
        state: AiCallEngine.PlaybackState,
        code: Int,
        msg: String?
    ) {
        // Playback state is informational here; errors surface via onError.
    }

    override fun onError(errorMessage: String) {
        runOnUiThread {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }
}
