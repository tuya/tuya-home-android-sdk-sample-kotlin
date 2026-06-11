package com.thingclips.sdk.aistream.ai

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.widget.LinearLayout
import androidx.appcompat.widget.PopupMenu
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thingclips.sdk.aistream.AudioAmplitudesCallback
import com.thingclips.sdk.aistream.ConnectCallback
import com.thingclips.sdk.aistream.EventStartCallback
import com.thingclips.sdk.aistream.IThingAiStream
import com.thingclips.sdk.aistream.SessionCallback
import com.thingclips.sdk.aistream.StreamResultCallback
import com.thingclips.sdk.aistream.ThingAiStream
import com.thingclips.sdk.aistream.ThingAiStreamListener
import com.thingclips.sdk.aistream.audio.AudioPlayCallback
import com.thingclips.sdk.aistream.business.AgentTokenRequestParams
import com.thingclips.sdk.aistream.helper.EventStartOptions
import com.thingclips.smart.android.aistream.Constants
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.android.aistream.ThingStreamManager
import com.thingclips.smart.android.aistream.data.StreamAudio
import com.thingclips.smart.android.aistream.data.StreamEvent
import com.thingclips.smart.android.aistream.data.StreamImage
import com.thingclips.smart.android.aistream.data.StreamText
import com.thingclips.smart.android.aistream.data.StreamVideo
import com.tuya.appsdk.sample.user.R
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class AiChatActivity : AppCompatActivity() {

    companion object {
        const val TAG = "ai_stream_ChatActivity"
        const val AMPLITUDES_LENGTH = 50

        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
        private const val REQUEST_READ_STORAGE_PERMISSION = 201
        private const val REQUEST_PICK_IMAGE = 202
        private const val REQUEST_SWITCH_ROLE = 203
        private const val REQUEST_MEMORY = 204

        private const val MENU_MEMORY = 303
        private const val MENU_EMOTION = 306
        private const val MENU_TEST_ALL = 307

        private const val API_GET_TOKEN = "m.life.ai.token.get"
        private const val API_VERSION = "1.0"

        private const val PREFS_ROLE_CACHE = "ai_role_cache"
    }

    private var aiStream: IThingAiStream? = null

    private lateinit var tvStatus: TextView
    private lateinit var rvChatMessages: RecyclerView
    private lateinit var etMessageInput: EditText
    private lateinit var ivSelectImage: ImageView
    private lateinit var ivSendOrVoice: ImageView
    private lateinit var flImagePreviewContainer: FrameLayout
    private lateinit var ivImagePreview: ImageView
    private lateinit var ivClosePreview: ImageView
    private lateinit var btnHoldToTalk: Button
    private lateinit var flVoiceInputContainer: FrameLayout
    private lateinit var audioAmplitudeView: AudioAmplitudeView
    private lateinit var tvEmoji: TextView

    // Role header card views
    private lateinit var llRoleExpanded: LinearLayout
    private lateinit var llRoleCollapsed: LinearLayout
    private lateinit var ivRoleAvatar: ImageView
    private lateinit var ivRoleAvatarSmall: ImageView
    private lateinit var tvRoleName: TextView
    private lateinit var tvRoleNameSmall: TextView
    private lateinit var tvChipTag: TextView
    private lateinit var tvChipLang: TextView
    private lateinit var tvChipTimbre: TextView
    private lateinit var btnEditRole: TextView
    private lateinit var btnSwitchRole: TextView

    private lateinit var chatAdapter: ChatAdapter
    private val messageList = mutableListOf<ChatMessage>()

    private var currentSessionId: String? = null
    private var mCurrentEventId: String? = null // Represents the active event being constructed

    private var selectedImageUri: Uri? = null
    private var tempAsrResult: String = ""
    private var isVoiceMode: Boolean = false
    private var isRecordingAudio: Boolean = false
    private var isConnecting: Boolean = false
    private var isSessionCreating: Boolean = false

    private val audioPermissions = arrayOf(Manifest.permission.RECORD_AUDIO)
    private val storagePermissions_LEGACY = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    private val emojiSteps = mutableListOf<SkillEmojiStep>()
    private val emojiHandler = Handler(Looper.getMainLooper())
    private var currentEmojiIndex = 0
    private var emojiRunnable: Runnable? = null

    private lateinit var recordingHintPopup: PopupWindow
    private lateinit var tvPopupHint: TextView
    private var isFingerOutsideButton = false
    private var originalButtonBackground: Drawable? = null
    private var initialTouchY: Float = 0f
    private lateinit var mOwnerId: String
    private lateinit var mAiSolutionCode: String
    private lateinit var mMiniProgramId: String
    private lateinit var mDevId: String

    private lateinit var agent: AiAgentManager
    private var dbHelper: AiChatRecordDbHelper? = null
    private var currentRoleId: String = "default"
    private var currentBindRoleType: Int = BindRoleType.DEFAULT
    private var currentRoleDetail: RoleDetail? = null

    private data class SkillEmojiStep(val emoji: String, val startTime: Long, val endTime: Long)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_chat)
        val ownerId = intent.getStringExtra("ownerId")
        val aiSolutionCode = intent.getStringExtra("aiSolutionCode")
        val miniProgramId = intent.getStringExtra("miniProgramId")
        if (ownerId.isNullOrEmpty()) {
            Log.e(TAG, "Owner ID is required to initialize AI Stream.")
            Toast.makeText(this, "Owner ID is required", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        mOwnerId = ownerId
        if (aiSolutionCode.isNullOrEmpty() || miniProgramId.isNullOrEmpty()) {
            Log.e(TAG, "AI Solution Code and Mini Program ID are required.")
            Toast.makeText(
                this,
                "AI Solution Code and Mini Program ID are required",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }
        mAiSolutionCode = aiSolutionCode
        mMiniProgramId = miniProgramId
        mDevId = intent.getStringExtra("devId") ?: ""
        if (mDevId.isEmpty()) {
            Log.e(TAG, "devId is required for device-identity connection.")
            Toast.makeText(this, "devId is required", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        agent = AiAgentManager(mDevId)
        val uid = ThingHomeSdk.getUserInstance().user?.uid ?: "0"
        dbHelper = AiChatRecordDbHelper(this, uid)

        initViews()
        setupHeader()
        initAiStream()
        setupChatRecyclerView()
        setupInputControls()
        initRecordingPopup()

        connectToAiStream()
        updateUiForSessionState()
        // Render the cached role + local history immediately; the network
        // chain (connect -> session -> getBindRole) only refreshes later.
        loadCachedRole()
        loadLocalHistory()
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tv_status)
        rvChatMessages = findViewById(R.id.rv_chat_messages)
        etMessageInput = findViewById(R.id.et_message_input)
        ivSelectImage = findViewById(R.id.iv_select_image)
        ivSendOrVoice = findViewById(R.id.iv_send_or_voice)
        flImagePreviewContainer = findViewById(R.id.fl_image_preview_container)
        ivImagePreview = findViewById(R.id.iv_image_preview)
        ivClosePreview = findViewById(R.id.iv_close_image_preview)
        flVoiceInputContainer = findViewById(R.id.fl_voice_input_container)
        btnHoldToTalk = findViewById(R.id.btn_hold_to_talk)
        audioAmplitudeView = findViewById(R.id.audio_amplitude_view)
        originalButtonBackground = btnHoldToTalk.background

        tvEmoji = findViewById(R.id.tv_emoji)
        llRoleExpanded = findViewById(R.id.ll_role_expanded)
        llRoleCollapsed = findViewById(R.id.ll_role_collapsed)
        ivRoleAvatar = findViewById(R.id.iv_role_avatar)
        ivRoleAvatarSmall = findViewById(R.id.iv_role_avatar_small)
        tvRoleName = findViewById(R.id.tv_role_name)
        tvRoleNameSmall = findViewById(R.id.tv_role_name_small)
        tvChipTag = findViewById(R.id.tv_chip_tag)
        tvChipLang = findViewById(R.id.tv_chip_lang)
        tvChipTimbre = findViewById(R.id.tv_chip_timbre)
        btnEditRole = findViewById(R.id.btn_edit_role)
        btnSwitchRole = findViewById(R.id.btn_switch_role)
    }

    private fun setupHeader() {
        findViewById<ImageView>(R.id.iv_back).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.iv_more).setOnClickListener { showMoreMenu(it) }
        findViewById<ImageView>(R.id.iv_role_collapse).setOnClickListener {
            setRoleCardExpanded(false)
        }
        llRoleCollapsed.setOnClickListener { setRoleCardExpanded(true) }

        btnSwitchRole.setOnClickListener {
            if (!isSessionActive()) {
                showToast("Session not active")
                return@setOnClickListener
            }
            startActivityForResult(
                Intent(this, RoleListActivity::class.java)
                    .putExtra(RoleListActivity.EXTRA_DEV_ID, mDevId)
                    .putExtra(RoleListActivity.EXTRA_CURRENT_ROLE_ID, currentRoleId),
                REQUEST_SWITCH_ROLE
            )
        }
        btnEditRole.setOnClickListener {
            if (!requireRole()) return@setOnClickListener
            if (currentBindRoleType != BindRoleType.CUSTOM) {
                showToast("Only custom roles can be edited")
                return@setOnClickListener
            }
            startActivity(
                Intent(this, RoleEditActivity::class.java)
                    .putExtra("devId", mDevId)
                    .putExtra("roleId", currentRoleId)
            )
        }
    }

    private fun setRoleCardExpanded(expanded: Boolean) {
        llRoleExpanded.visibility = if (expanded) View.VISIBLE else View.GONE
        llRoleCollapsed.visibility = if (expanded) View.GONE else View.VISIBLE
    }

    private fun initAiStream() {
        ThingStreamManager.getInstance().enableDebugLog(true)
        aiStream = ThingAiStream.newInstance().apply {
            setStreamListener(aiStreamListener)
        }
    }

    private fun setupChatRecyclerView() {
        chatAdapter = ChatAdapter(this, messageList)
        rvChatMessages.layoutManager = LinearLayoutManager(this)
        rvChatMessages.adapter = chatAdapter
    }

    private fun initRecordingPopup() {
        val popupView = layoutInflater.inflate(R.layout.popup_recording_hint, null)
        tvPopupHint = popupView.findViewById(R.id.tv_popup_text)
        recordingHintPopup = PopupWindow(
            popupView,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            false
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupInputControls() {
        etMessageInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (etMessageInput.visibility == View.VISIBLE) {
                    updateSendButtonIcon()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        ivSelectImage.setOnClickListener {
            hideKeyboard()
            pickImageFromGallery()
        }

        ivClosePreview.setOnClickListener { handleImageCloseClick() }
        ivSendOrVoice.setOnClickListener { handleSendOrVoiceClick() }

        btnHoldToTalk.setOnTouchListener { v, event ->
            if (!checkAndRequestAudioPermission()) return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> handleVoiceTouchDown(event)
                MotionEvent.ACTION_MOVE -> handleVoiceTouchMove(v, event)
                MotionEvent.ACTION_UP -> handleVoiceTouchUp()
                else -> false
            }
        }
    }

    private fun handleImageCloseClick() {
        Log.d(TAG, "Image preview close clicked.")
        if (!mCurrentEventId.isNullOrEmpty()) {
            cancelAndClearCurrentEvent() // This will cancel the event and clear related UI
        } else {
            clearImagePreviewUI() // Only clear UI if no event was associated
        }
        updateSendButtonIcon()
    }

    private fun handleSendOrVoiceClick() {
        hideKeyboard()
        if (!isVoiceMode) {
            val hasContent =
                etMessageInput.text.toString().trim().isNotEmpty() || selectedImageUri != null
            if (hasContent) sendMessageFlow() else updateInputMode(true)
        } else {
            updateInputMode(false) // Back to keyboard input
        }
    }

    private fun handleVoiceTouchDown(event: MotionEvent): Boolean {
        if (!isSessionActive()) {
            showToast("Session not active, cannot start recording.")
            return false
        }
        isFingerOutsideButton = false
        initialTouchY = event.rawY
        showVoiceRecordingUI()
        startAudioRecordingFlow()
        showOrUpdateRecordingPopup(flVoiceInputContainer, "Release to Send, Slide Up to Cancel")
        return true
    }

    private fun handleVoiceTouchMove(v: View, event: MotionEvent): Boolean {
        if (!isRecordingAudio) return true // Consume if not yet recording
        val currentY = event.rawY
        if (initialTouchY - currentY > v.height * 0.8) { // Slide up threshold
            if (!isFingerOutsideButton) {
                isFingerOutsideButton = true
                showOrUpdateRecordingPopup(flVoiceInputContainer, "Release to Cancel")
            }
        } else {
            if (isFingerOutsideButton) {
                isFingerOutsideButton = false
                showOrUpdateRecordingPopup(
                    flVoiceInputContainer,
                    "Release to Send, Slide Up to Cancel"
                )
                audioAmplitudeView.setBackgroundColor(Color.TRANSPARENT)
            }
        }
        return true
    }

    private fun handleVoiceTouchUp(): Boolean {
        if (!isRecordingAudio && mCurrentEventId.isNullOrEmpty() && !isFingerOutsideButton) {
            dismissRecordingPopup()
            hideVoiceRecordingUI()
            return true
        }

        dismissRecordingPopup()
        hideVoiceRecordingUI()

        if (isFingerOutsideButton) {
            Toast.makeText(this, "Recording Cancelled", Toast.LENGTH_SHORT).show()
            cancelAudioRecordingSession()
        } else {
            if (isRecordingAudio) {
                stopAndFinalizeAudioRecording()
            } else {
                Log.w(
                    TAG,
                    "ACTION_UP for audio but not in recording state or mCurrentEventId missing."
                )
                if (!mCurrentEventId.isNullOrEmpty() && selectedImageUri == null) {
                    cancelAndClearCurrentEvent()
                }
            }
        }
        isFingerOutsideButton = false
        return true
    }

    private fun updateSendButtonIcon() {
        if (isVoiceMode) {
            ivSendOrVoice.setImageResource(R.drawable.ai_ic_keyboard)
            return
        }
        val hasText = etMessageInput.text.toString().trim().isNotEmpty()
        val hasImage = selectedImageUri != null

        if (hasText || hasImage) {
            ivSendOrVoice.setImageResource(R.drawable.ai_ic_send)
        } else {
            ivSendOrVoice.setImageResource(R.drawable.ai_ic_mic)
        }
    }

    private fun updateInputMode(enableVoice: Boolean) {
        isVoiceMode = enableVoice
        if (enableVoice) {
            etMessageInput.visibility = View.GONE
            flVoiceInputContainer.visibility = View.VISIBLE
            btnHoldToTalk.visibility = View.VISIBLE
            audioAmplitudeView.visibility = View.GONE
        } else {
            etMessageInput.visibility = View.VISIBLE
            flVoiceInputContainer.visibility = View.GONE
        }
        updateSendButtonIcon()
    }

    private fun showVoiceRecordingUI() {
        btnHoldToTalk.visibility = View.GONE
        audioAmplitudeView.visibility = View.VISIBLE
        audioAmplitudeView.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun hideVoiceRecordingUI() {
        btnHoldToTalk.visibility = View.VISIBLE
        audioAmplitudeView.visibility = View.GONE
        originalButtonBackground?.let { btnHoldToTalk.background = it }
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun showOrUpdateRecordingPopup(anchor: View, text: String) {
        if (!::tvPopupHint.isInitialized || !::recordingHintPopup.isInitialized) return

        if (recordingHintPopup.isShowing) {
            recordingHintPopup.dismiss()
        }

        tvPopupHint.text = text

        recordingHintPopup.contentView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        val popupWidth = recordingHintPopup.contentView.measuredWidth
        val popupHeight = recordingHintPopup.contentView.measuredHeight

        val anchorPos = IntArray(2)
        anchor.getLocationOnScreen(anchorPos)

        val xPos = anchorPos[0] + (anchor.width / 2) - (popupWidth / 2)
        val yPos = anchorPos[1] - popupHeight - 20

        recordingHintPopup.width = popupWidth
        recordingHintPopup.height = popupHeight

        recordingHintPopup.showAtLocation(anchor.rootView, Gravity.NO_GRAVITY, xPos, yPos)
    }

    private fun dismissRecordingPopup() {
        if (::recordingHintPopup.isInitialized && recordingHintPopup.isShowing) {
            recordingHintPopup.dismiss()
        }
    }

    // --- AI Stream Connection & Session Management ---
    private fun connectToAiStream() {
        if (aiStream?.isConnected(Constants.ClientType.DEVICE, mDevId) == true) {
            Log.i(TAG, "Already connected.")
            if (currentSessionId.isNullOrEmpty()) createNewSession()
            return
        }
        if (isConnecting) {
            Log.w(TAG, "Already connecting, ignoring new connect request.")
            return
        }
        isConnecting = true
        tvStatus.text = "Status: Connecting..."
        aiStream?.connectWithDevice(mDevId, object : ConnectCallback {
            override fun onSuccess(connectionId: String) {
                isConnecting = false
                Log.i(TAG, "connectWithDevice onSuccess, connectionId: $connectionId")
                // State change will be handled by listener
            }

            override fun onError(code: Int, error: String) {
                isConnecting = false
                Log.e(TAG, "connectWithDevice onError, code: $code, error: $error")
                runOnUiThread {
                    tvStatus.text = "Status: Connect failed ($error)"
                    Toast.makeText(
                        this@AiChatActivity,
                        "Connect failed: $error",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    private fun createNewSession() {
        if (!isStreamConnected()) {
            showToast("Not connected. Attempting to reconnect...")
            connectToAiStream()
            return
        }
        if (!currentSessionId.isNullOrEmpty()) {
            Log.i(TAG, "Session already exists: $currentSessionId")
            updateStatusText("Status: Session Active ($currentSessionId)")
            updateUiForSessionState()
            return
        }
        updateStatusText("Status: Creating session...")
        if (isSessionCreating) {
            Log.w(TAG, "Session creation already in progress, ignoring new request.")
            return
        }
        isSessionCreating = true
        // Device-identity session: token request carries deviceId in extParams.
        val params = AgentTokenRequestParams.Builder()
            .api(API_GET_TOKEN)
            .apiVersion(API_VERSION)
            .ownerId(mOwnerId)
            .aiSolutionCode(mAiSolutionCode)
            .addExtParam("miniProgramId", mMiniProgramId)
            .addExtParam("deviceId", mDevId)
            .addExtParam("needTts", "true")
            .build()

        aiStream?.createSession(params, null, object : SessionCallback {
            override fun onSuccess(
                sessionId: String,
                sendDataCodes: Map<String, Int>,
                revDataCodes: Map<String, Int>
            ) {
                isSessionCreating = false
                Log.i(TAG, "createSession onSuccess: $sessionId")
                // State change will be handled by listener
            }

            override fun onError(code: Int, message: String) {
                isSessionCreating = false
                Log.e(TAG, "createSession onError: $code, message: $message")
                runOnUiThread {
                    updateStatusText("Status: Session Failed (Error: $code)")
                    Toast.makeText(
                        this@AiChatActivity,
                        "Session creation failed: $message",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    private fun isStreamConnected(): Boolean =
        aiStream?.isConnected(Constants.ClientType.DEVICE, mDevId) == true

    private fun isSessionActive(): Boolean =
        isStreamConnected() && !currentSessionId.isNullOrEmpty()

    private fun updateStatusText(status: String) {
        tvStatus.text = status
    }

    // --- AI Stream Event Lifecycle ---
    private fun startNewEvent(callback: EventStartCallback) {
        if (!isSessionActive()) {
            showToast("Session not active. Cannot start event")
            callback.onError(-1, "Session not active")
            return
        }
        val options = EventStartOptions.Builder(currentSessionId!!).build()
        aiStream?.sendEventStart(options, callback)
    }

    private fun finalizeEvent(eventIdToFinalize: String, onCompletion: (() -> Unit)?) {
        if (eventIdToFinalize.isEmpty() || currentSessionId.isNullOrEmpty()) {
            Log.w(
                TAG,
                "Cannot finalize event, eventId or sessionId is missing. EventId: $eventIdToFinalize"
            )
            onCompletion?.invoke()
            return
        }
        Log.i(TAG, "Finalizing event: $eventIdToFinalize")
        aiStream?.sendEventEnd(
            eventIdToFinalize,
            currentSessionId!!,
            null,
            object : StreamResultCallback {
                override fun onSuccess() {
                    Log.i(TAG, "sendEventEnd success for event: $eventIdToFinalize")
                    if (eventIdToFinalize == mCurrentEventId) { // Clear only if it's the current one
                        mCurrentEventId = null
                    }
                    onCompletion?.invoke()
                }

                override fun onError(errorCode: Int, errorMessage: String) {
                    Log.e(
                        TAG,
                        "sendEventEnd failed for event $eventIdToFinalize: $errorMessage , errorCode $errorCode"
                    )
                    if (eventIdToFinalize == mCurrentEventId) {
                        mCurrentEventId = null // Still clear it to avoid stale state
                    }
                    onCompletion?.invoke()
                }
            })
    }

    private fun cancelAndClearCurrentEvent() {
        val eventIdToCancel = mCurrentEventId
        if (eventIdToCancel.isNullOrEmpty() || currentSessionId.isNullOrEmpty()) {
            Log.w(TAG, "Cannot cancel event, mCurrentEventId or sessionId missing.")
            mCurrentEventId = null
            clearImagePreviewUI()
            return
        }

        mCurrentEventId = null // Clear immediately to prevent reuse
        clearImagePreviewUI()

        Log.i(TAG, "Attempting to sendEventChatBreak for eventId: $eventIdToCancel")
        aiStream?.sendEventChatBreak(
            eventIdToCancel,
            currentSessionId!!,
            null,
            object : StreamResultCallback {
                override fun onSuccess() {
                    Log.i(TAG, "sendEventChatBreak success for event: $eventIdToCancel")
                }

                override fun onError(errorCode: Int, errorMessage: String) {
                    Log.e(
                        TAG,
                        "sendEventChatBreak failed for event $eventIdToCancel: $errorMessage , errorCode $errorCode"
                    )
                }
            })
    }

    // --- Image Handling ---
    private fun pickImageFromGallery() {
        if (!checkAndRequestStoragePermission()) return
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_PICK_IMAGE)
    }

    @Deprecated("This method is deprecated in favor of the Activity Result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK && data?.data != null) {
            handleImageSelectionResult(data.data!!)
        }
        if (requestCode == REQUEST_SWITCH_ROLE && resultCode == RESULT_OK && data != null) {
            val bindRoleType =
                data.getIntExtra(RoleListActivity.EXTRA_BIND_ROLE_TYPE, BindRoleType.TEMPLATE)
            val roleId = data.getStringExtra(RoleListActivity.EXTRA_ROLE_ID)
            if (!roleId.isNullOrEmpty() && roleId != currentRoleId) {
                switchRole(bindRoleType, roleId)
            }
        }
        if (requestCode == REQUEST_MEMORY && resultCode == RESULT_OK &&
            data?.getBooleanExtra(MemoryActivity.EXTRA_HISTORY_CLEARED, false) == true
        ) {
            messageList.clear()
            chatAdapter.notifyDataSetChanged()
        }
    }

    private fun handleImageSelectionResult(imageUri: Uri) {
        selectedImageUri = imageUri
        showImagePreviewUI(imageUri)

        if (!isSessionActive()) {
            showToast("Session not active. Image staged.")
            return
        }

        if (!mCurrentEventId.isNullOrEmpty()) {
            cancelAndClearCurrentEvent()
        }

        startNewEvent(object : EventStartCallback {
            override fun onSuccess(eventId: String) {
                mCurrentEventId = eventId
                Log.i(TAG, "sendEventStart for image selection success: $eventId")
                sendImageDataInternal(imageUri, mCurrentEventId!!) {
                    showToast("Image ready. Add text or send.")
                }
            }

            override fun onError(errorCode: Int, errorMessage: String) {
                Log.e(TAG, "sendEventStart for image selection failed: $errorCode $errorMessage")
                showToast("Failed to start event for image: $errorMessage")
                clearImagePreviewUI()
            }
        })
    }

    private fun sendImageDataInternal(imageUri: Uri, eventId: String, onCompletion: (() -> Unit)?) {
        val imagePath = getPathFromUri(imageUri)
        if (imagePath.isNullOrEmpty()) {
            showToast("Failed to get image path.")
            onCompletion?.invoke()
            return
        }

        aiStream?.sendImageData(currentSessionId!!, imagePath, null, object : StreamResultCallback {
            override fun onSuccess() {
                Log.i(TAG, "sendImageData success for event: $eventId")
                Log.i(TAG, "sendEventPayloadsEnd for IMAGE success for event: $eventId")
                onCompletion?.invoke()
            }

            override fun onError(code: Int, message: String) {
                Log.e(TAG, "sendImageData failed for event $eventId: $code $message")
                showToast("Send image data failed: $message")
                onCompletion?.invoke()
            }
        })
    }

    private fun showImagePreviewUI(imageUri: Uri) {
        Glide.with(this).load(imageUri).into(ivImagePreview)
        flImagePreviewContainer.visibility = View.VISIBLE
        updateSendButtonIcon()
    }

    private fun clearImagePreviewUI() {
        selectedImageUri = null
        flImagePreviewContainer.visibility = View.GONE
        ivImagePreview.setImageDrawable(null) // Clear image
        updateSendButtonIcon()
    }

    // --- Audio Recording ---
    private fun startAudioRecordingFlow() {
        if (!isSessionActive() || isRecordingAudio) return

        if (!mCurrentEventId.isNullOrEmpty()) {
            Log.d(TAG, "Using existing eventId for audio: $mCurrentEventId")
            proceedWithAudioRecordingInternal(mCurrentEventId!!)
        } else {
            Log.d(TAG, "Starting new event for audio")
            startNewEvent(object : EventStartCallback {
                override fun onSuccess(eventId: String) {
                    mCurrentEventId = eventId
                    Log.i(TAG, "Audio EventStart success: $eventId")
                    proceedWithAudioRecordingInternal(eventId)
                }

                override fun onError(errorCode: Int, errorMessage: String) {
                    Log.e(TAG, "Audio EventStart failed: $errorCode $errorMessage")
                    showToast("Failed to start audio event: $errorMessage")
                    runOnUiThread {
                        dismissRecordingPopup()
                        hideVoiceRecordingUI()
                    }
                }
            })
        }
    }

    private lateinit var loadingDialog: AlertDialog

    private fun showLoadingDialog() {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
            val progressBar = ProgressBar(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.CENTER
                }
            }

            val container = FrameLayout(this).apply {
                setPadding(40, 40, 40, 40)
                addView(progressBar)
            }

            builder.setView(container)
            builder.setCancelable(false)
            loadingDialog = builder.create()
            loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            loadingDialog.show()
        }
    }

    private fun dismissLoadingDialog() {
        runOnUiThread {
            if (::loadingDialog.isInitialized && loadingDialog.isShowing) {
                loadingDialog.dismiss()
            }
        }
    }

    private fun proceedWithAudioRecordingInternal(eventIdToUse: String) {
        isRecordingAudio = true
        tempAsrResult = ""

        aiStream?.registerRecordAmplitudesCallback(AMPLITUDES_LENGTH, audioAmplitudesCallback)
        showLoadingDialog()
        aiStream?.initAudioRecorder(object : StreamResultCallback {
            override fun onError(errorCode: Int, errorMessage: String) {
                showToast("Init audio recorder failed.")
                dismissLoadingDialog()
            }

            override fun onSuccess() {
                dismissLoadingDialog()
                aiStream?.startRecordAndSendAudioData(
                    currentSessionId!!,
                    null,
                    null,
                    object : StreamResultCallback {
                        override fun onSuccess() {
                            Log.d(
                                TAG,
                                "startRecordAndSendAudioData success for event: $eventIdToUse"
                            )
                        }

                        override fun onError(code: Int, message: String) {
                            Log.e(TAG, "startRecordAndSendAudioData failed: $code $message")
                            showToast("Start recording failed: $message")
                            isRecordingAudio = false
                            aiStream?.unregisterRecordAmplitudesCallback() // Ensure unregister

                            if (eventIdToUse == mCurrentEventId && selectedImageUri == null) {
                                cancelAndClearCurrentEvent()
                            } else if (eventIdToUse == mCurrentEventId && selectedImageUri != null) {
                                Log.w(
                                    TAG,
                                    "Audio part of image event failed to start recording. Image event $mCurrentEventId remains."
                                )
                            }
                            runOnUiThread {
                                dismissRecordingPopup()
                                hideVoiceRecordingUI()
                            }
                        }
                    })
            }
        })
    }

    private fun stopAndFinalizeAudioRecording() {
        if (!isRecordingAudio || mCurrentEventId.isNullOrEmpty()) {
            Log.w(TAG, "stopAndFinalizeAudioRecording: Not recording or mCurrentEventId is null.")
            isRecordingAudio = false
            return
        }
        val eventIdForAudio = mCurrentEventId!! // Capture before it might be cleared
        isRecordingAudio = false
        aiStream?.unregisterRecordAmplitudesCallback()

        selectedImageUri?.let { uri ->
            addMessage(
                ChatMessage(
                    isSentByUser = true,
                    messageType = ChatMessage.MessageType.IMAGE,
                    imageUri = uri
                )
            )
        }

        aiStream?.stopRecordAndSendAudioData(
            currentSessionId!!,
            null,
            null,
            object : StreamResultCallback {
                override fun onSuccess() {
                    Log.d(TAG, "stopRecordAndSendAudioData success for event: $eventIdForAudio")
                    Log.i(
                        TAG,
                        "sendEventPayloadsEnd for AUDIO success for event: $eventIdForAudio"
                    )
                    finalizeEvent(eventIdForAudio) {
                        Log.d(TAG, "Event $eventIdForAudio finalized after voice.")
                        clearImagePreviewUI()
                    }
                }

                override fun onError(code: Int, message: String) {
                    Log.e(
                        TAG,
                        "stopRecordAndSendAudioData failed for $eventIdForAudio: $code $message"
                    )
                    finalizeEvent(eventIdForAudio) { clearImagePreviewUI() }
                }
            })
    }

    private fun cancelAudioRecordingSession() {
        Log.d(
            TAG,
            "cancelAudioRecordingSession called. mCurrentEventId: $mCurrentEventId, isRecording: $isRecordingAudio"
        )
        val wasRecording = isRecordingAudio
        isRecordingAudio = false
        aiStream?.unregisterRecordAmplitudesCallback()
        tempAsrResult = ""

        if (!mCurrentEventId.isNullOrEmpty()) {
            Log.i(TAG, "Cancelling current event due to audio cancel: $mCurrentEventId")
            cancelAndClearCurrentEvent()
        } else if (wasRecording) {
            Log.w(
                TAG,
                "Audio recording cancelled but mCurrentEventId was null. State might be inconsistent."
            )
        }

        hideVoiceRecordingUI()
        dismissRecordingPopup()
        isFingerOutsideButton = false
    }

    // --- Text Message Sending ---
    private fun sendMessageFlow() {
        if (!isSessionActive()) {
            showToast("Session not active. Please wait or reconnect.")
            connectToAiStream()
            return
        }
        val textMessage = etMessageInput.text.toString().trim()
        val hasText = textMessage.isNotEmpty()
        val isImageStaged = selectedImageUri != null && !mCurrentEventId.isNullOrEmpty()

        if (!hasText && !isImageStaged) {
            showToast("Nothing to send.")
            return
        }

        if (hasText) {
            addMessage(
                ChatMessage(
                    text = textMessage,
                    isSentByUser = true,
                    messageType = ChatMessage.MessageType.TEXT
                )
            )
        }
        etMessageInput.setText("")

        if (isImageStaged) {
            addMessage(
                ChatMessage(
                    isSentByUser = true,
                    messageType = ChatMessage.MessageType.IMAGE,
                    imageUri = selectedImageUri
                )
            )
            Log.d(
                TAG,
                "Continuing event $mCurrentEventId ${if (hasText) " with additional text." else " for staged image."}"
            )
            sendTextDataAndFinalizeIfNeeded(
                if (hasText) textMessage else null,
                mCurrentEventId!!,
                true
            )
        } else {
            Log.d(TAG, "Starting new event for text message.")
            startNewEvent(object : EventStartCallback {
                override fun onSuccess(newTextEventId: String) {
                    mCurrentEventId = newTextEventId
                    Log.i(TAG, "sendEventStart for text success: $newTextEventId")
                    sendTextDataAndFinalizeIfNeeded(textMessage, newTextEventId, false)
                }

                override fun onError(errorCode: Int, errorMessage: String) {
                    Log.e(TAG, "sendEventStart for text failed: $errorCode $errorMessage")
                    showToast("Failed to start event for text: $errorMessage")
                    if (hasText && messageList.isNotEmpty() && messageList.last().text == textMessage) {
                        messageList.removeAt(messageList.size - 1)
                        chatAdapter.notifyItemRemoved(messageList.size)
                    }
                }
            })
        }
    }

    private fun sendTextDataAndFinalizeIfNeeded(
        textMessage: String?,
        eventId: String,
        wasImageStaged: Boolean
    ) {
        val hasTextPayload = !textMessage.isNullOrEmpty()

        val finalizeLogic = {
            finalizeEvent(eventId) {
                Log.d(TAG, "Event $eventId handling completed in sendTextDataAndFinalizeIfNeeded.")
                if (wasImageStaged) {
                    clearImagePreviewUI()
                }
            }
        }

        if (hasTextPayload) {
            val streamText = StreamText().apply { text = textMessage }
            aiStream?.sendTextData(currentSessionId!!, streamText, object : StreamResultCallback {
                override fun onSuccess() {
                    Log.i(TAG, "sendTextData success for event: $eventId")
                    Log.i(
                        TAG,
                        "sendEventPayloadsEnd for TEXT success for event: $eventId"
                    )
                    finalizeLogic()
                }


                override fun onError(code: Int, message: String) {
                    Log.e(TAG, "sendTextData failed for $eventId: $code $message")
                    showToast("Send text failed: $message")
                    finalizeLogic()
                }
            })
        } else if (wasImageStaged) {
            Log.d(TAG, "No text payload for event $eventId (image only). Finalizing.")
            finalizeLogic()
        } else {
            Log.w(
                TAG,
                "sendTextDataAndFinalizeIfNeeded called with no text and no staged image for event $eventId"
            )
            finalizeLogic()
        }
    }

    // --- Chat Message Display ---
    private fun addMessage(message: ChatMessage) {
        runOnUiThread {
            messageList.add(message)
            chatAdapter.notifyItemInserted(messageList.size - 1)
            rvChatMessages.scrollToPosition(messageList.size - 1)
        }
        persistMessage(message)
    }

    private fun persistMessage(message: ChatMessage) {
        val helper = dbHelper ?: return
        // Streaming NLG text arrives in append chunks sharing one bizId; it is
        // upserted via persistNlg() instead so the full reply is saved, not just
        // the first chunk.
        if (message.messageType == ChatMessage.MessageType.NLG_TEXT && !message.bizId.isNullOrEmpty()) return
        val record = ChatMessageRecord(
            devId = mDevId,
            roleId = currentRoleId,
            bizId = message.bizId,
            sender = if (message.isSentByUser) 0 else 1,
            msgType = message.messageType.name.lowercase(),
            content = message.text,
            imageUri = message.imageUri?.toString() ?: message.imageUrl,
            ts = message.timestamp
        )
        Thread { helper.insert(record) }.start()
    }

    private fun persistNlg(bizId: String) {
        val helper = dbHelper ?: return
        if (bizId.isEmpty()) return
        val msg = messageList.lastOrNull {
            !it.isSentByUser && it.bizId == bizId && it.messageType == ChatMessage.MessageType.NLG_TEXT
        } ?: return
        val content = msg.text
        val ts = msg.timestamp
        val dev = mDevId
        val role = currentRoleId
        Thread { helper.upsertNlg(dev, role, bizId, content, ts) }.start()
    }

    private fun loadLocalHistory() {
        val helper = dbHelper ?: return
        Thread {
            val records = helper.query(mDevId, currentRoleId)
            val restored = records.map { it.toChatMessage() }
            runOnUiThread {
                if (restored.isEmpty()) return@runOnUiThread
                messageList.clear()
                messageList.addAll(restored)
                chatAdapter.notifyDataSetChanged()
                rvChatMessages.scrollToPosition(messageList.size - 1)
            }
        }.start()
    }

    private fun ChatMessageRecord.toChatMessage(): ChatMessage {
        val type = runCatching {
            ChatMessage.MessageType.valueOf(msgType.uppercase())
        }.getOrDefault(ChatMessage.MessageType.TEXT)
        val isImageUrl = type == ChatMessage.MessageType.NLG_IMAGE
        return ChatMessage(
            text = content,
            imageUri = if (!isImageUrl && imageUri != null) Uri.parse(imageUri) else null,
            imageUrl = if (isImageUrl) imageUri else null,
            isSentByUser = sender == 0,
            messageType = type,
            bizId = bizId
        )
    }

    // --- Role management ---
    private fun resolveBoundRole() {
        agent.initAgentRoleBinding(object : Cb<RoleDetail> {
            override fun onOk(data: RoleDetail?) {
                agent.getBindRole(object : Cb<RoleDetail> {
                    override fun onOk(bind: RoleDetail?) {
                        applyRole(bind ?: data)
                    }

                    override fun onErr(code: Int, msg: String?) {
                        applyRole(data)
                    }
                })
            }

            override fun onErr(code: Int, msg: String?) {
                Log.e(TAG, "initAgentRoleBinding failed: $code $msg")
            }
        })
    }

    private fun applyRole(detail: RoleDetail?) {
        if (detail?.roleId.isNullOrEmpty()) return
        // Same role as the one rendered from cache: refresh the header and
        // merge cloud history without clearing the visible conversation.
        val sameRole = detail!!.roleId == currentRoleId
        currentRoleId = detail.roleId!!
        currentBindRoleType = detail.bindRoleType
        currentRoleDetail = detail
        saveRoleCache(detail)
        runOnUiThread {
            updateRoleHeader(detail)
            if (!sameRole) {
                messageList.clear()
                chatAdapter.notifyDataSetChanged()
            }
        }
        if (!sameRole) loadLocalHistory()
        loadCloudHistory()
    }

    // --- Role cache: render last known role/header before any network IO ---
    private fun saveRoleCache(detail: RoleDetail) {
        getSharedPreferences(PREFS_ROLE_CACHE, Context.MODE_PRIVATE).edit()
            .putString("$mDevId.roleId", detail.roleId)
            .putInt("$mDevId.bindRoleType", detail.bindRoleType)
            .putString("$mDevId.roleName", detail.roleName)
            .putString("$mDevId.roleImgUrl", detail.roleImgUrl)
            .putString("$mDevId.useLangName", detail.useLangName ?: detail.useLangCode)
            .putString("$mDevId.useTimbreName", detail.useTimbreName)
            .apply()
    }

    private fun loadCachedRole() {
        val sp = getSharedPreferences(PREFS_ROLE_CACHE, Context.MODE_PRIVATE)
        val roleId = sp.getString("$mDevId.roleId", null) ?: return
        currentRoleId = roleId
        currentBindRoleType = sp.getInt("$mDevId.bindRoleType", BindRoleType.DEFAULT)
        updateRoleHeader(
            RoleDetail(
                roleId = roleId,
                roleName = sp.getString("$mDevId.roleName", null),
                roleImgUrl = sp.getString("$mDevId.roleImgUrl", null),
                useLangName = sp.getString("$mDevId.useLangName", null),
                useTimbreName = sp.getString("$mDevId.useTimbreName", null),
                bindRoleType = currentBindRoleType
            )
        )
    }

    private fun updateRoleHeader(detail: RoleDetail) {
        val name = detail.roleName ?: detail.roleId ?: ""
        tvRoleName.text = name
        tvRoleNameSmall.text = name

        tvChipTag.setText(
            when (detail.bindRoleType) {
                BindRoleType.CUSTOM -> R.string.ai_role_tag_custom
                BindRoleType.TEMPLATE -> R.string.ai_role_tag_template
                else -> R.string.ai_role_tag_default
            }
        )

        val lang = detail.useLangName ?: detail.useLangCode
        tvChipLang.text = lang ?: ""
        tvChipLang.visibility = if (lang.isNullOrEmpty()) View.GONE else View.VISIBLE
        tvChipTimbre.text = detail.useTimbreName ?: ""
        tvChipTimbre.visibility =
            if (detail.useTimbreName.isNullOrEmpty()) View.GONE else View.VISIBLE

        if (!detail.roleImgUrl.isNullOrEmpty()) {
            Glide.with(this).load(detail.roleImgUrl)
                .transform(RoundedCorners(resources.displayMetrics.density.times(14).toInt()))
                .into(ivRoleAvatar)
            Glide.with(this).load(detail.roleImgUrl).circleCrop().into(ivRoleAvatarSmall)
        }

        btnEditRole.visibility =
            if (detail.bindRoleType == BindRoleType.CUSTOM) View.VISIBLE else View.GONE
    }

    private fun switchRole(bindRoleType: Int, roleId: String) {
        if (roleId.isEmpty()) return
        agent.bindRole(bindRoleType, roleId, object : Cb<Boolean> {
            override fun onOk(data: Boolean?) {
                showToast("Role switched")
                currentRoleId = roleId
                currentBindRoleType = bindRoleType
                // Re-fetch the bound role so the header card reflects the new role.
                agent.getBindRole(object : Cb<RoleDetail> {
                    override fun onOk(bind: RoleDetail?) {
                        if (bind?.roleId.isNullOrEmpty()) refreshAfterRoleChange()
                        else applyRole(bind)
                    }

                    override fun onErr(code: Int, msg: String?) {
                        refreshAfterRoleChange()
                    }
                })
            }

            override fun onErr(code: Int, msg: String?) {
                showToast("Switch role failed: $msg")
            }
        })
    }

    private fun refreshAfterRoleChange() {
        runOnUiThread {
            tvRoleName.text = currentRoleId
            tvRoleNameSmall.text = currentRoleId
            messageList.clear()
            chatAdapter.notifyDataSetChanged()
        }
        loadLocalHistory()
        loadCloudHistory()
    }

    private fun loadCloudHistory() {
        if (currentRoleId == "default" || currentRoleId.isEmpty()) return
        agent.fetchHistory(
            currentBindRoleType, currentRoleId,
            null, System.currentTimeMillis(), 50, true,
            object : Cb<ArrayList<ChatHistoryItem>> {
                override fun onOk(data: ArrayList<ChatHistoryItem>?) {
                    if (data.isNullOrEmpty()) return
                    mergeCloudHistory(data)
                }

                override fun onErr(code: Int, msg: String?) {
                    Log.w(TAG, "fetchHistory failed: $code $msg")
                }
            }
        )
    }

    private fun mergeCloudHistory(items: List<ChatHistoryItem>) {
        val seenBizIds = messageList.mapNotNull { it.bizId }.toMutableSet()
        val toAdd = mutableListOf<ChatMessage>()
        for (item in items) {
            val key = item.requestId ?: item.gmtCreate.toString()
            if (seenBizIds.contains(key)) continue
            seenBizIds.add(key)
            item.question?.forEach { part ->
                part.context?.takeIf { it.isNotEmpty() }?.let {
                    toAdd.add(
                        ChatMessage(
                            text = it,
                            isSentByUser = true,
                            messageType = ChatMessage.MessageType.TEXT,
                            bizId = key
                        )
                    )
                }
            }
            item.answer?.forEach { part ->
                part.context?.takeIf { it.isNotEmpty() }?.let {
                    toAdd.add(
                        ChatMessage(
                            text = it,
                            isSentByUser = false,
                            messageType = ChatMessage.MessageType.NLG_TEXT,
                            bizId = key
                        )
                    )
                }
            }
        }
        if (toAdd.isEmpty()) return
        runOnUiThread {
            messageList.addAll(toAdd)
            chatAdapter.notifyDataSetChanged()
            rvChatMessages.scrollToPosition(messageList.size - 1)
        }
    }

    // --- "..." popup menu ---
    private fun showMoreMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, MENU_MEMORY, 0, getString(R.string.ai_menu_memory))
        popup.menu.add(0, MENU_EMOTION, 1, getString(R.string.ai_menu_emotion))
        popup.menu.add(0, MENU_TEST_ALL, 2, getString(R.string.ai_menu_diagnostics))
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_MEMORY -> {
                    if (!requireRole()) return@setOnMenuItemClickListener true
                    startActivityForResult(
                        Intent(this, MemoryActivity::class.java)
                            .putExtra("devId", mDevId)
                            .putExtra("roleId", currentRoleId)
                            .putExtra("bindRoleType", currentBindRoleType),
                        REQUEST_MEMORY
                    )
                    true
                }

                MENU_EMOTION -> {
                    if (!isSessionActive()) {
                        showToast("Session not active")
                        return@setOnMenuItemClickListener true
                    }
                    showCurrentEmotion()
                    true
                }

                MENU_TEST_ALL -> {
                    if (!isSessionActive()) {
                        showToast("Session not active")
                        return@setOnMenuItemClickListener true
                    }
                    runDiagnostics()
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    private fun requireRole(): Boolean {
        if (!isSessionActive()) { showToast("Session not active"); return false }
        if (currentRoleId == "default" || currentRoleId.isEmpty()) {
            showToast("Role not resolved yet")
            return false
        }
        return true
    }

    private fun runDiagnostics() {
        showToast("Running API diagnostics...")
        AiAgentDiagnostics(this, mDevId, currentBindRoleType, currentRoleId).runAll { file ->
            runOnUiThread {
                val path = file?.absolutePath ?: "(write failed, see logcat tag ai_stream_Diag)"
                AlertDialog.Builder(this)
                    .setTitle("Diagnostics done")
                    .setMessage("Saved to:\n$path\n\nAlso logged to logcat (tag: ai_stream_Diag).")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun showCurrentEmotion() {
        agent.currentEmotion(object : Cb<ChatEmotion> {
            override fun onOk(data: ChatEmotion?) {
                data ?: return
                runOnUiThread {
                    if (!data.emotion.isNullOrEmpty()) tvEmoji.text = data.emotion
                    showToast("Emotion: ${data.emotion} ${data.text ?: ""}")
                }
            }

            override fun onErr(code: Int, msg: String?) {
                showToast("Get emotion failed: $msg")
            }
        })
    }

    // --- AI Stream Listener Callbacks ---
    private val aiStreamListener = object : ThingAiStreamListener {
        override fun onConnectStateChanged(connectionId: String, state: Int, errorCode: Int) {
            Log.i(
                TAG,
                "Connection state changed: connectionId=$connectionId, state=$state, errorCode=$errorCode"
            )
            runOnUiThread {
                if (state == Constants.ConnectState.CONNECTED) {
                    updateStatusText("Status: Connected ($connectionId)")
                    createNewSession()
                } else {
                    updateStatusText("Status: Disconnected (Error: $errorCode)")
                    currentSessionId = null
                }
                updateUiForSessionState()
            }
        }

        override fun onSessionStateChanged(sessionId: String, state: Int, errorCode: Int) {
            Log.i(
                TAG,
                "Session state changed: sessionId=$sessionId, state=$state, errorCode=$errorCode"
            )
            runOnUiThread {
                when (state) {
                    Constants.SessionState.CREATE_SUCCESS -> {
                        currentSessionId = sessionId
                        updateStatusText("Status: Session Created ($sessionId)")
                        resolveBoundRole()
                    }

                    Constants.SessionState.CREATE_FAILED -> {
                        updateStatusText("Status: Session Failed (Error: $errorCode)")
                        currentSessionId = null
                    }

                    Constants.SessionState.CLOSED_BY_SERVER -> {
                        updateStatusText("Status: Session Closed by Server ($sessionId)")
                        if (sessionId == currentSessionId) currentSessionId = null
                    }

                    Constants.SessionState.AGENT_TOKEN_EXPIRED -> {
                        updateStatusText("Status: Agent Token Expired. Requesting new token...")
                        if (sessionId == currentSessionId) {
                            currentSessionId = null
                            createNewSession()
                        }
                    }
                }
                updateUiForSessionState()
            }
        }

        override fun onAudioReceived(audioData: StreamAudio) {
            if (audioData.payload == null || audioData.payload!!.isEmpty()) {
                Log.w(TAG, "Received empty audio data, ignoring.")
                return
            }
            Log.i(
                TAG,
                "Audio received: ${audioData.payload!!.size} bytes, streamFlag=${audioData.streamFlag}"
            )
            if (audioData.streamFlag == Constants.StreamFlag.START) {
                aiStream?.startPlayAudio(audioData, audioPlayCallback)
            }
        }

        override fun onVideoReceived(videoData: StreamVideo) {
            Log.w(TAG, "Video received - Not supported yet")
        }

        override fun onImageReceived(imageData: StreamImage) {
            if (imageData.payload == null || imageData.payload!!.isEmpty()) {
                Log.w(TAG, "Received empty image data, ignoring.")
                return
            }
            Log.i(TAG, "Raw Image received: ${imageData.payload!!.size} bytes")
        }

        override fun onFileReceived(fileData: com.thingclips.smart.android.aistream.data.StreamFile) {
            Log.i(TAG, "File received: ${fileData.fileName}")
        }

        override fun onTextReceived(textData: StreamText) {
            if (textData.text.isNullOrEmpty()) {
                Log.w(TAG, "Received empty text data, ignoring.")
                return
            }
            Log.i(TAG, "Text received: ${textData.text} for sessionIds: ${textData.sessionId}")
            if (textData.sessionId != currentSessionId) {
                Log.w(TAG, "Received text for an inactive or different session. Ignoring.")
                return
            }
            runOnUiThread { parseAndDisplayReceivedText(textData.text) }
        }

        override fun onEventReceived(event: StreamEvent) {
            Log.i(TAG, "Event received: $event")
        }
    }

    private fun parseAndDisplayReceivedText(jsonText: String) {
        try {
            val jsonObject = JSONObject(jsonText)
            val bizType = jsonObject.optString("bizType")
            val bizId = jsonObject.optString("bizId")
            val eof = jsonObject.optInt("eof", -1)
            val data = jsonObject.optJSONObject("data")

            if (data == null) {
                Log.e(TAG, "No data object in received JSON: $jsonText")
                addMessage(
                    ChatMessage(
                        text = jsonText,
                        isSentByUser = false,
                        messageType = ChatMessage.MessageType.NLG_TEXT,
                        bizId = bizId
                    )
                )
                return
            }

            when (bizType.uppercase()) {
                "ASR" -> handleAsrData(data, eof)
                "NLG" -> handleNlgData(data, bizId, eof)
                "SKILL" -> handleSkillData(data, eof)
                else -> {
                    val unknownContent = data.optString("text", data.optString("content", jsonText))
                    addMessage(
                        ChatMessage(
                            text = unknownContent,
                            isSentByUser = false,
                            messageType = ChatMessage.MessageType.NLG_TEXT,
                            bizId = bizId
                        )
                    )
                    Log.w(TAG, "Unknown bizType: $bizType, displaying content: $unknownContent")
                }
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Error parsing received JSON: $jsonText", e)
            addMessage(
                ChatMessage(
                    text = jsonText,
                    isSentByUser = false,
                    messageType = ChatMessage.MessageType.NLG_TEXT
                )
            )
        }
    }

    private fun handleAsrData(data: JSONObject, eof: Int) {
        val asrText = data.optString("text")
        tempAsrResult = asrText
        if (eof == 0) { // Intermediate ASR result
            Log.d(TAG, "ASR intermediate: $tempAsrResult")
        } else if (eof == 1) { // Final ASR result
            Log.d(TAG, "ASR final: $tempAsrResult")
            if (tempAsrResult.isNotEmpty()) {
                addMessage(
                    ChatMessage(
                        text = tempAsrResult,
                        isSentByUser = true,
                        messageType = ChatMessage.MessageType.VOICE_TO_TEXT
                    )
                )
            }
            tempAsrResult = "" // Clear for next recording
        }
    }

    private fun handleNlgData(data: JSONObject, bizId: String, eof: Int) {
        val content = data.optString("content")
        val appendMode = data.optString("appendMode")

        if (content.isNotEmpty()) {
            var appended = false
            if ("append".equals(appendMode, ignoreCase = true) && bizId.isNotEmpty()) {
                val indexToUpdate = messageList.indexOfLast { msg ->
                    !msg.isSentByUser && msg.bizId == bizId && msg.messageType == ChatMessage.MessageType.NLG_TEXT
                }
                if (indexToUpdate != -1) {
                    val msg = messageList[indexToUpdate]
                    msg.text += content
                    chatAdapter.notifyItemChanged(indexToUpdate)
                    rvChatMessages.scrollToPosition(messageList.size - 1)
                    appended = true
                }
            }
            if (!appended) {
                addMessage(
                    ChatMessage(
                        text = content,
                        isSentByUser = false,
                        messageType = ChatMessage.MessageType.NLG_TEXT,
                        bizId = bizId
                    )
                )
            }
        }

        data.optJSONArray("images")?.let { imagesArray ->
            for (i in 0 until imagesArray.length()) {
                imagesArray.optJSONObject(i)?.optString("url")?.takeIf { it.isNotEmpty() }
                    ?.let { imageUrl ->
                        addMessage(
                            ChatMessage(
                                imageUrl = imageUrl,
                                isSentByUser = false,
                                messageType = ChatMessage.MessageType.NLG_IMAGE,
                                bizId = bizId
                            )
                        )
                    }
            }
        }

        if (eof == 1) {
            Log.d(TAG, "NLG stream finished for bizId: $bizId")
            // Persist the full reply once, when streaming completes (not per chunk).
            if (bizId.isNotEmpty()) persistNlg(bizId)
        }
    }

    private fun handleSkillData(data: JSONObject, eof: Int) {
        val code = data.optString("code")
        if ("llm_emo".equals(code, ignoreCase = true)) {
            data.optJSONObject("skillContent")?.let { skillContent ->
                val emoji = skillContent.optString("text")
                val startTime = skillContent.optLong("startTime")
                val endTime = skillContent.optLong("endTime")
                val sequence = skillContent.optInt("sequence")

                if (sequence == 1) {
                    emojiSteps.clear()
                }
                emojiSteps.add(SkillEmojiStep(emoji, startTime, endTime))

                if (eof == 1 || sequence > 0) {
                    showSkillEmojis()
                }
            }
        } else {
            Log.w(TAG, "Unhandled skill code: $code, data: $data")
        }
    }

    private fun showSkillEmojis() {
        if (emojiSteps.isEmpty()) return

        emojiRunnable?.let { emojiHandler.removeCallbacks(it) }
        currentEmojiIndex = 0

        emojiRunnable = object : Runnable {
            override fun run() {
                if (currentEmojiIndex >= emojiSteps.size) {
                    tvEmoji.text = "😀" // Reset to default
                    return
                }
                val step = emojiSteps[currentEmojiIndex]
                tvEmoji.text = step.emoji

                var duration = step.endTime - step.startTime
                if (duration <= 0) {
                    duration = if (currentEmojiIndex < emojiSteps.size - 1) {
                        val nextStep = emojiSteps[currentEmojiIndex + 1]
                        (nextStep.startTime - step.startTime).takeIf { it > 0 } ?: 500
                    } else {
                        1000 // Last emoji
                    }
                }

                currentEmojiIndex++
                if (currentEmojiIndex < emojiSteps.size) {
                    emojiHandler.postDelayed(this, duration)
                }
            }
        }
        emojiHandler.post(emojiRunnable!!)
    }

    private val audioPlayCallback = object : AudioPlayCallback {
        override fun onPlayStart() {
            Log.i(TAG, "Audio playback started")
        }

        override fun onPlayFinish() {
            Log.i(TAG, "Audio playback finished")
        }

        override fun onPlayError(errorCode: Int, errorMessage: String) {
            Log.e(TAG, "Audio playback error: $errorMessage")
        }
    }

    private val audioAmplitudesCallback = object : AudioAmplitudesCallback {
        override fun onSuccess(amplitudes: DoubleArray?) {
            runOnUiThread {
                if (audioAmplitudeView.visibility == View.VISIBLE && amplitudes?.size == AMPLITUDES_LENGTH) {
                    audioAmplitudeView.setAmplitudes(amplitudes)
                }
            }
        }

        override fun onError(errorCode: Int, errorMessage: String) {
            Log.w(TAG, "Audio amplitudes callback error: $errorMessage")
        }
    }

    // --- UI State & Utilities ---
    private fun updateUiForSessionState() {
        val active = isSessionActive()
        etMessageInput.isEnabled = active
        ivSelectImage.isEnabled = active
        ivSendOrVoice.isEnabled = active
        btnHoldToTalk.isEnabled = active

        etMessageInput.hint = getString(
            if (active) R.string.ai_chat_input_hint else R.string.ai_chat_input_hint_inactive
        )
        updateSendButtonIcon()
    }

    private fun showToast(message: String) {
        runOnUiThread { Toast.makeText(this@AiChatActivity, message, Toast.LENGTH_SHORT).show() }
    }

    private fun getPathFromUri(uri: Uri?): String? {
        if (uri == null) return null
        var tempFile: File? = null
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val cacheDir = applicationContext.cacheDir
                val fileName = "temp_img_${System.currentTimeMillis()}"
                tempFile = File.createTempFile(fileName, ".tmp", cacheDir)
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                return tempFile?.absolutePath
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copying file to cache from URI: $uri", e)
            tempFile?.takeIf { it.exists() }?.delete()
        }
        return null
    }

    // --- Permission Handling ---
    private fun checkAndRequestAudioPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                audioPermissions,
                REQUEST_RECORD_AUDIO_PERMISSION
            )
            false
        } else {
            true
        }
    }

    private fun checkAndRequestStoragePermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                storagePermissions_LEGACY[0]
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                storagePermissions_LEGACY,
                REQUEST_READ_STORAGE_PERMISSION
            )
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_RECORD_AUDIO_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showToast("Audio permission granted.")
                } else {
                    showToast("Record audio permission denied.")
                }
            }

            REQUEST_READ_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showToast("Storage permission granted.")
                    pickImageFromGallery()
                } else {
                    showToast("Storage permission denied.")
                }
            }
        }
    }

    // --- Lifecycle Methods ---
    override fun onResume() {
        super.onResume()
        if (aiStream != null) {
            if (!isStreamConnected()) {
                connectToAiStream()
            } else if (currentSessionId.isNullOrEmpty()) {
                createNewSession()
            }
        }
        updateUiForSessionState()
    }

    override fun onPause() {
        super.onPause()
        if (isRecordingAudio) {
            showToast("Recording cancelled due to app pause.")
            cancelAudioRecordingSession()
        }
        dismissRecordingPopup()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanupResources()
    }

    private fun cleanupResources() {
        aiStream?.let { stream ->
            if (!mCurrentEventId.isNullOrEmpty()) {
                cancelAndClearCurrentEvent()
            }
            if (!currentSessionId.isNullOrEmpty()) {
                stream.closeSession(currentSessionId!!, null)
                currentSessionId = null
            }
            stream.destroy()
        }
        aiStream = null
        emojiRunnable?.let { emojiHandler.removeCallbacks(it) }
        dismissRecordingPopup()
        dbHelper?.close()
        dbHelper = null
    }
}