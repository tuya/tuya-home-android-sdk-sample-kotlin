package com.tuya.smart.android.demo.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.RelativeLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.android.camera.sdk.ThingIPCSdk
import com.thingclips.smart.camera.camerasdk.thingplayer.callback.AbsP2pCameraListener
import com.thingclips.smart.camera.camerasdk.thingplayer.callback.OnRenderDirectionCallback
import com.thingclips.smart.camera.camerasdk.thingplayer.callback.OperationDelegateCallBack
import com.thingclips.smart.camera.ipccamerasdk.p2p.ICameraP2P
import com.thingclips.smart.camera.middleware.p2p.IThingSmartCameraP2P
import com.thingclips.smart.camera.middleware.widget.AbsVideoViewCallback
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.ipc.camera.autotesting.activity.AutoCameraTestingProgramListActivity
import com.thingclips.smart.ipc.camera.cloudtool.activity.CloudToolHomeActivity
import com.thingclips.smart.sdk.api.IResultCallback
import com.thingclips.smart.sdk.api.IThingDevice
import com.tuya.appsdk.sample.resource.HomeModel
import com.tuya.smart.android.demo.camera.databinding.ActivityCameraPanelBinding
import com.tuya.smart.android.demo.camera.utils.*
import java.io.File
import java.nio.ByteBuffer

/**
 * @author houqing <a href="mailto:developer@tuya.com"/>
 * @since 2021/7/26 5:02 PM
 */
class CameraPanelActivity : AppCompatActivity(), View.OnClickListener {
    companion object {
        private const val ASPECT_RATIO_WIDTH = 9
        private const val ASPECT_RATIO_HEIGHT = 16
        private const val TAG = "CameraPanelActivity"
    }

    private var isSpeaking = false
    private var isRecording = false
    private var isPlay = false
    private var previewMute = ICameraP2P.MUTE
    private var videoClarity = ICameraP2P.HD
    private var currVideoClarity: String? = null
    private var devId: String? = null
    private lateinit var viewBinding: ActivityCameraPanelBinding
    private var mCameraP2P: IThingSmartCameraP2P<Any>? = null

    @SuppressLint("HandlerLeak")
    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                Constants.MSG_CONNECT -> handleConnect(msg)
                Constants.MSG_SET_CLARITY -> handleClarity(msg)
                Constants.MSG_MUTE -> handleMute(msg)
                Constants.MSG_SCREENSHOT -> handlesnapshot(msg)
                Constants.MSG_VIDEO_RECORD_BEGIN -> ToastUtil.shortToast(
                    this@CameraPanelActivity,
                    getString(R.string.operation_suc)
                )
                Constants.MSG_VIDEO_RECORD_FAIL -> ToastUtil.shortToast(
                    this@CameraPanelActivity,
                    getString(R.string.operation_failed)
                )
                Constants.MSG_VIDEO_RECORD_OVER -> handleVideoRecordOver(msg)
                Constants.MSG_TALK_BACK_BEGIN -> handleStartTalk(msg)
                Constants.MSG_TALK_BACK_OVER -> handleStopTalk(msg)
                Constants.MSG_GET_VIDEO_CLARITY -> handleGetVideoClarity(msg)
            }
            super.handleMessage(msg)
        }
    }
    var cameraPTZHelper: CameraPTZHelper? = null

    private fun handleStopTalk(msg: Message) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
            ToastUtil.shortToast(
                this@CameraPanelActivity,
                getString(R.string.ipc_stop_talk) + getString(R.string.operation_suc)
            )
        } else {
            ToastUtil.shortToast(
                this@CameraPanelActivity,
                getString(R.string.ipc_stop_talk) + getString(R.string.operation_failed)
            )
        }
    }

    private fun handleStartTalk(msg: Message) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
            ToastUtil.shortToast(
                this@CameraPanelActivity,
                getString(R.string.ipc_start_talk) + getString(R.string.operation_suc)
            )
        } else {
            ToastUtil.shortToast(
                this@CameraPanelActivity,
                getString(R.string.ipc_start_talk) + getString(R.string.operation_failed)
            )
        }
    }

    private fun handleVideoRecordOver(msg: Message) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
            ToastUtil.shortToast(this@CameraPanelActivity, getString(R.string.operation_suc))
        } else {
            ToastUtil.shortToast(this@CameraPanelActivity, getString(R.string.operation_failed))
        }
    }

    private fun handlesnapshot(msg: Message) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
            ToastUtil.shortToast(this@CameraPanelActivity, getString(R.string.operation_suc))
        } else {
            ToastUtil.shortToast(this@CameraPanelActivity, getString(R.string.operation_failed))
        }
    }

    private fun handleMute(msg: Message) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
            viewBinding.cameraMute.isSelected = (previewMute == ICameraP2P.MUTE)
        } else {
            ToastUtil.shortToast(this@CameraPanelActivity, getString(R.string.operation_failed))
        }
    }


    private fun handleClarity(msg: Message) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
            viewBinding.cameraQuality.text =
                if (videoClarity == ICameraP2P.HD) getString(R.string.hd) else getString(R.string.sd)
        } else {
            ToastUtil.shortToast(this@CameraPanelActivity, getString(R.string.operation_failed))
        }
    }

    private fun handleConnect(msg: Message) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
            preview();
        } else {
            ToastUtil.shortToast(this@CameraPanelActivity, getString(R.string.connect_failed))
        }
    }

    private fun handleGetVideoClarity(msg: Message) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS && !TextUtils.isEmpty(currVideoClarity)) {
            var info = getString(R.string.other)
            if (currVideoClarity == ICameraP2P.HD.toString()) {
                info = getString(R.string.hd)
            } else if (currVideoClarity == ICameraP2P.STANDEND.toString()) {
                info = getString(R.string.sd)
            }
            ToastUtil.shortToast(
                this@CameraPanelActivity,
                getString(R.string.get_current_clarity) + info
            )
        } else {
            ToastUtil.shortToast(this@CameraPanelActivity, getString(R.string.operation_failed))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraPanelBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        setSupportActionBar(viewBinding.toolbarView)
        val windowManager = this.getSystemService(WINDOW_SERVICE) as WindowManager
        val width = windowManager.defaultDisplay.width
        val height = width * ASPECT_RATIO_WIDTH / ASPECT_RATIO_HEIGHT
        val layoutParams = RelativeLayout.LayoutParams(width, height)
        layoutParams.addRule(RelativeLayout.BELOW, R.id.toolbar_view)
        viewBinding.cameraVideoViewRl.layoutParams = layoutParams
        viewBinding.cameraMute.isSelected = true
        initData()
        initListener()
        if (querySupportByDPID(DPConstants.PTZ_CONTROL)) {
            //Cloud Station Control
            viewBinding.cameraVideoView.setOnRenderDirectionCallback(object :
                OnRenderDirectionCallback {
                override fun onLeft() {
                    cameraPTZHelper?.ptzControl(DPConstants.PTZ_LEFT)
                }

                override fun onRight() {
                    cameraPTZHelper?.ptzControl(DPConstants.PTZ_RIGHT)
                }

                override fun onUp() {
                    cameraPTZHelper?.ptzControl(DPConstants.PTZ_UP)
                }

                override fun onDown() {
                    cameraPTZHelper?.ptzControl(DPConstants.PTZ_DOWN)
                }

                override fun onCancel() {
                    cameraPTZHelper?.ptzStop()
                }
            })
        }
    }

    private var iTuyaDevice: IThingDevice? = null
    private fun publishDps(dpId: String, value: Any) {
        if (iTuyaDevice == null) {
            iTuyaDevice = ThingHomeSdk.newDeviceInstance(devId)
        }
        val jsonObject = JSONObject()
        jsonObject[dpId] = value
        val dps = jsonObject.toString()
        iTuyaDevice?.publishDps(dps, object : IResultCallback {
            override fun onError(code: String, error: String) {
                Log.e(TAG, "publishDps err $dps")
            }

            override fun onSuccess() {
                Log.i(TAG, "publishDps suc $dps")
            }
        })
    }

    private fun querySupportByDPID(dpId: String): Boolean {
        return ThingHomeSdk.getDataInstance().getDeviceBean(devId)?.run {
            val dps = this.getDps()
            return (dps != null && dps[dpId] != null)
        } == true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_camera_panel, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_remove_device) {
            val dialog = AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(getString(R.string.remove_device_dialog))
                .setPositiveButton(getString(R.string.confirm)) { _: DialogInterface?, _: Int -> unBindDevice() }
                .create()
            dialog.show()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun unBindDevice() {
        ThingHomeSdk.newDeviceInstance(devId).removeDevice(object : IResultCallback {
            override fun onError(s: String, s1: String) {
                ToastUtil.shortToast(this@CameraPanelActivity, s1)
            }

            override fun onSuccess() {
                mHandler.removeCallbacksAndMessages(null)
                finish()
            }
        })
    }

    private fun initData() {
        devId = intent.getStringExtra(Constants.INTENT_DEV_ID)
        ThingIPCSdk.getCameraInstance()?.let {
            mCameraP2P = it.createCameraP2P(devId)
        }
        viewBinding.cameraVideoView.setViewCallback(object : AbsVideoViewCallback() {
            override fun onCreated(o: Any) {
                super.onCreated(o)
                mCameraP2P?.generateCameraView(o)
            }
        })
//        viewBinding.cameraVideoView.createVideoView(p2pType)
        viewBinding.cameraVideoView.createVideoView(devId)
        if (mCameraP2P == null) showNotSupportToast()
        devId?.let {
            cameraPTZHelper = CameraPTZHelper(it)
        }
        cameraPTZHelper?.bindPtzBoard(findViewById(R.id.sv_ptz_board))
    }

    private fun showNotSupportToast() {
        ToastUtil.shortToast(this@CameraPanelActivity, getString(R.string.not_support_device))
    }

    private fun initListener() {
        mCameraP2P?.let {
            viewBinding.cameraMute.setOnClickListener(this)
            viewBinding.cameraQuality.setOnClickListener(this)
            viewBinding.cameraControlBoard.speakTxt.setOnClickListener(this)
            viewBinding.cameraControlBoard.recordTxt.setOnClickListener(this)
            viewBinding.cameraControlBoard.photoTxt.setOnClickListener(this)
            viewBinding.cameraControlBoard.replayTxt.setOnClickListener(this)
            viewBinding.cameraControlBoard.cloudTxt.setOnClickListener(this)
            viewBinding.cameraControlBoard.messageCenterTxt.setOnClickListener(this)
            viewBinding.cameraControlBoard.debugTxt.setOnClickListener(this)
            viewBinding.cameraControlBoard.ptzTxt.setOnClickListener(this)
        }
        viewBinding.toolbarView.setNavigationOnClickListener {
            onBackPressed()
        }
        viewBinding.cameraControlBoard.settingTxt.setOnClickListener(this)
        viewBinding.cameraControlBoard.infoTxt.setOnClickListener(this)
        viewBinding.cameraControlBoard.getClarityTxt.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.camera_mute -> muteClick()
            R.id.camera_quality -> setVideoClarity()
            R.id.speak_Txt -> speakClick()
            R.id.record_Txt -> recordClick()
            R.id.photo_Txt -> snapShotClick()
            R.id.replay_Txt -> {
                val intent = Intent(this@CameraPanelActivity, CameraPlaybackActivity::class.java)
                intent.putExtra(Constants.INTENT_DEV_ID, devId)
                startActivity(intent)
            }
            R.id.setting_Txt -> {
                val intent1 = Intent(this@CameraPanelActivity, CameraSettingActivity::class.java)
                intent1.putExtra(Constants.INTENT_DEV_ID, devId)
                startActivity(intent1)
            }
            R.id.cloud_Txt -> {
                // 判断设备是否支持云存储
                val isSupportCloudStorage =
                    ThingIPCSdk.getCloud()?.isSupportCloudStorage(devId) == true
                if (!isSupportCloudStorage) {
                    ToastUtil.shortToast(this@CameraPanelActivity, getString(R.string.not_support))
                    return
                }
                val intent2 =
                    Intent(this@CameraPanelActivity, CameraCloudStorageActivity::class.java)
                intent2.putExtra(Constants.INTENT_DEV_ID, devId)
                startActivity(intent2)
            }
            R.id.message_center_Txt -> {
                val intent3 = Intent(this@CameraPanelActivity, AlarmDetectionActivity::class.java)
                intent3.putExtra(Constants.INTENT_DEV_ID, devId)
                startActivity(intent3)
            }
            R.id.info_Txt -> {
                val intent4 = Intent(this@CameraPanelActivity, CameraInfoActivity::class.java)
                intent4.putExtra(Constants.INTENT_DEV_ID, devId)
                startActivity(intent4)
            }
            R.id.get_clarity_Txt -> {
                mCameraP2P?.getVideoClarity(object : OperationDelegateCallBack {
                    override fun onSuccess(i: Int, i1: Int, s: String) {
                        currVideoClarity = s
                        mHandler.sendMessage(
                            MessageUtil.getMessage(
                                Constants.MSG_GET_VIDEO_CLARITY,
                                Constants.ARG1_OPERATE_SUCCESS
                            )
                        )
                    }

                    override fun onFailure(i: Int, i1: Int, i2: Int) {
                        mHandler.sendMessage(
                            MessageUtil.getMessage(
                                Constants.MSG_GET_VIDEO_CLARITY,
                                Constants.ARG1_OPERATE_FAIL
                            )
                        )
                    }
                })
            }
            R.id.debug_Txt -> {
                val items = arrayOf(
                    getString(R.string.ipc_sdk_autotest_tools),
                    getString(R.string.ipc_cloud_debug_tools)
                )
                val builder = AlertDialog.Builder(this)
                builder.setItems(
                    items
                ) { _: DialogInterface?, which: Int ->
                    if (which == 0) {
                        val intent = Intent(
                            this@CameraPanelActivity,
                            AutoCameraTestingProgramListActivity::class.java
                        )
                        startActivity(intent)
                    } else if (which == 1) {
                        val intent =
                            Intent(this@CameraPanelActivity, CloudToolHomeActivity::class.java)
                        intent.putExtra(
                            "extra_current_home_id",
                            HomeModel.INSTANCE.getCurrentHome(this)
                        )
                        startActivity(intent)
                    }
                }
                builder.setNegativeButton(
                    getString(R.string.ipc_close)
                ) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
                builder.create().show()
            }
            R.id.ptz_Txt -> {
                cameraPTZHelper?.show()
            }
        }
    }

    private fun preview() {
        mCameraP2P?.startPreview(videoClarity, object : OperationDelegateCallBack {
            override fun onSuccess(sessionId: Int, requestId: Int, data: String) {
                Log.d(TAG, "start preview onSuccess")
                isPlay = true
            }

            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                Log.d(TAG, "start preview onFailure, errCode: $errCode")
                isPlay = false
            }
        })
    }

    private fun recordClick() {
        if (!isRecording) {
            val picPath = getExternalFilesDir(null)!!.path + "/" + devId
            val file = File(picPath)
            if (!file.exists()) {
                file.mkdirs()
            }
            val fileName = System.currentTimeMillis().toString() + ".mp4"
            mCameraP2P?.startRecordLocalMp4(
                picPath,
                fileName,
                this@CameraPanelActivity,
                object : OperationDelegateCallBack {
                    override fun onSuccess(sessionId: Int, requestId: Int, data: String) {
                        isRecording = true
                        mHandler.sendEmptyMessage(Constants.MSG_VIDEO_RECORD_BEGIN)
                        //returns the recorded thumbnail path （.jpg）
                        Log.i(TAG, "record :$data")
                    }

                    override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                        mHandler.sendEmptyMessage(Constants.MSG_VIDEO_RECORD_FAIL)
                    }
                })
            recordStatue(true)
        } else {
            mCameraP2P?.stopRecordLocalMp4(object : OperationDelegateCallBack {
                override fun onSuccess(sessionId: Int, requestId: Int, data: String) {
                    isRecording = false
                    mHandler.sendMessage(
                        MessageUtil.getMessage(
                            Constants.MSG_VIDEO_RECORD_OVER,
                            Constants.ARG1_OPERATE_SUCCESS
                        )
                    )
                }

                override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                    isRecording = false
                    mHandler.sendMessage(
                        MessageUtil.getMessage(
                            Constants.MSG_VIDEO_RECORD_OVER,
                            Constants.ARG1_OPERATE_FAIL
                        )
                    )
                }
            })
            recordStatue(false)
        }
    }

    private fun snapShotClick() {
        val picPath = getExternalFilesDir(null)!!.path + "/" + devId
        val file = File(picPath)
        if (!file.exists()) file.mkdirs()
        val fileName = System.currentTimeMillis().toString() + ".jpg"
        mCameraP2P?.snapshot(
            picPath,
            fileName,
            this@CameraPanelActivity,
            object : OperationDelegateCallBack {
                override fun onSuccess(sessionId: Int, requestId: Int, data: String) {
                    mHandler.sendMessage(
                        MessageUtil.getMessage(
                            Constants.MSG_SCREENSHOT,
                            Constants.ARG1_OPERATE_SUCCESS
                        )
                    )
                    Log.i(TAG, "snapshot :$data")
                }

                override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                    mHandler.sendMessage(
                        MessageUtil.getMessage(
                            Constants.MSG_SCREENSHOT,
                            Constants.ARG1_OPERATE_FAIL
                        )
                    )
                }
            })
    }

    private fun muteClick() {
        val mute = if (previewMute == ICameraP2P.MUTE) ICameraP2P.UNMUTE else ICameraP2P.MUTE
        mCameraP2P?.setMute(mute, object : OperationDelegateCallBack {
            override fun onSuccess(sessionId: Int, requestId: Int, data: String) {
                previewMute = Integer.valueOf(data)
                mHandler.sendMessage(
                    MessageUtil.getMessage(
                        Constants.MSG_MUTE,
                        Constants.ARG1_OPERATE_SUCCESS
                    )
                )
            }

            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                mHandler.sendMessage(
                    MessageUtil.getMessage(
                        Constants.MSG_MUTE,
                        Constants.ARG1_OPERATE_FAIL
                    )
                )
            }
        })
    }

    private fun speakClick() {
        if (isSpeaking) {
            mCameraP2P?.stopAudioTalk(object : OperationDelegateCallBack {
                override fun onSuccess(sessionId: Int, requestId: Int, data: String) {
                    isSpeaking = false
                    mHandler.sendMessage(
                        MessageUtil.getMessage(
                            Constants.MSG_TALK_BACK_OVER,
                            Constants.ARG1_OPERATE_SUCCESS
                        )
                    )
                }

                override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                    isSpeaking = false
                    mHandler.sendMessage(
                        MessageUtil.getMessage(
                            Constants.MSG_TALK_BACK_OVER,
                            Constants.ARG1_OPERATE_FAIL
                        )
                    )
                }
            })
        } else {
            if (Constants.hasRecordPermission()) {
                mCameraP2P?.startAudioTalk(object : OperationDelegateCallBack {
                    override fun onSuccess(sessionId: Int, requestId: Int, data: String) {
                        isSpeaking = true
                        mHandler.sendMessage(
                            MessageUtil.getMessage(
                                Constants.MSG_TALK_BACK_BEGIN,
                                Constants.ARG1_OPERATE_SUCCESS
                            )
                        )
                    }

                    override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                        isSpeaking = false
                        mHandler.sendMessage(
                            MessageUtil.getMessage(
                                Constants.MSG_TALK_BACK_BEGIN,
                                Constants.ARG1_OPERATE_FAIL
                            )
                        )
                    }
                })
            } else {
                Constants.requestPermission(
                    this@CameraPanelActivity,
                    Manifest.permission.RECORD_AUDIO,
                    Constants.EXTERNAL_AUDIO_REQ_CODE,
                    "open_recording"
                )
            }
        }
    }

    /**
     * Set video quality, HD or SD
     */
    private fun setVideoClarity() {
        mCameraP2P?.setVideoClarity(
            if (videoClarity == ICameraP2P.HD) ICameraP2P.STANDEND else ICameraP2P.HD,
            object : OperationDelegateCallBack {
                override fun onSuccess(sessionId: Int, requestId: Int, data: String) {
                    videoClarity = Integer.valueOf(data)
                    mHandler.sendMessage(
                        MessageUtil.getMessage(
                            Constants.MSG_SET_CLARITY,
                            Constants.ARG1_OPERATE_SUCCESS
                        )
                    )
                }

                override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                    mHandler.sendMessage(
                        MessageUtil.getMessage(
                            Constants.MSG_SET_CLARITY,
                            Constants.ARG1_OPERATE_FAIL
                        )
                    )
                }
            })
    }

    private fun recordStatue(isRecording: Boolean) {
        viewBinding.cameraControlBoard.speakTxt.isEnabled = !isRecording
        viewBinding.cameraControlBoard.photoTxt.isEnabled = !isRecording
        viewBinding.cameraControlBoard.replayTxt.isEnabled = !isRecording
        viewBinding.cameraControlBoard.recordTxt.isEnabled = true
        viewBinding.cameraControlBoard.recordTxt.isSelected = isRecording
    }

    /**
     * 设置智能画框的属性来控制框的样式（如框的颜色，画笔宽度，闪烁频率等），需要设备上报的 SEI 信息支持
     *
     * @param rectFeaturesJson 格式
     * {
     * "SmartRectFeature":[
     * {
     * "type":0,
     * "index":0,
     * "brushWidth":1,
     * "flashFps":{
     * "drawKeepFrames":2,
     * "stopKeepFrames":2
     * },
     * "rgb":0xFF0000,
     * "shape":0
     * },
     * {
     * "type":0,
     * "index":1,
     * "brushWidth":2,
     * "flashFps":{
     * "drawKeepFrames":3,
     * "stopKeepFrames":2
     * },
     * "rgb":0x00FF00,
     * "shape":1
     * }
     * ]
     * }
     */
    private fun setSmartRectFeatures(rectFeaturesJson: String) {
        mCameraP2P?.setSmartRectFeatures(rectFeaturesJson)
    }

    /**
     * 支持拉伸/缩放，左右/上下镜像，90/180/270度旋转等。
     *
     * @param renderFeaturesJson 格式
     * {
     * "DecPostProcess":{
     * "video":[
     * {
     * "restype":"4",
     * "oldres":"944*1080",
     * "newres":"1920*1080"
     * },
     * {
     * "restype":"2",
     * "oldres":"944*1080",
     * "newres":"1920*1080"
     * }
     * ],
     * "mirror":0,
     * "rotation":2
     * }
     * }
     */
    private fun setDeviceFeatures(renderFeaturesJson: String) {
        mCameraP2P?.setDeviceFeatures(renderFeaturesJson)
    }

    override fun onResume() {
        super.onResume()
        viewBinding.cameraVideoView.onResume()
        //must register again,or can't callback
        mCameraP2P?.let {
            it.registerP2PCameraListener(p2pCameraListener)
            it.generateCameraView(viewBinding.cameraVideoView.createdView())
            if (it.isConnecting) {
                it.startPreview(object : OperationDelegateCallBack {
                    override fun onSuccess(sessionId: Int, requestId: Int, data: String) {
                        isPlay = true
                    }

                    override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                        Log.d(TAG, "start preview onFailure, errCode: $errCode")
                    }
                })
            } else {
                if (ThingIPCSdk.getCameraInstance()?.isLowPowerDevice(devId) == true) {
                    ThingIPCSdk.getDoorbell()?.wirelessWake(devId)
                }
                //Establishing a p2p channel
                it.connect(devId, object : OperationDelegateCallBack {
                    override fun onSuccess(i: Int, i1: Int, s: String) {
                        mHandler.sendMessage(
                            MessageUtil.getMessage(
                                Constants.MSG_CONNECT,
                                Constants.ARG1_OPERATE_SUCCESS
                            )
                        )
                    }

                    override fun onFailure(i: Int, i1: Int, i2: Int) {
                        mHandler.sendMessage(
                            MessageUtil.getMessage(
                                Constants.MSG_CONNECT,
                                Constants.ARG1_OPERATE_FAIL
                            )
                        )
                    }
                })
            }
        }
    }

    var reConnect = false

    private val p2pCameraListener: AbsP2pCameraListener = object : AbsP2pCameraListener() {
        override fun onReceiveSpeakerEchoData(pcm: ByteBuffer, sampleRate: Int) {
            mCameraP2P?.let {
                val length = pcm.capacity()
                Log.d(TAG, "receiveSpeakerEchoData pcmlength $length sampleRate $sampleRate")
                val pcmData = ByteArray(length)
                pcm[pcmData, 0, length]
                it.sendAudioTalkData(pcmData, length)
            }
        }

        override fun onSessionStatusChanged(camera: Any?, sessionId: Int, sessionStatus: Int) {
            super.onSessionStatusChanged(camera, sessionId, sessionStatus)
            if (sessionStatus == -3 || sessionStatus == -105) {
                // 遇到超时/鉴权失败，建议重连一次，避免循环调用
                if (!reConnect) {
                    reConnect = true
                    mCameraP2P?.connect(devId, object : OperationDelegateCallBack {
                        override fun onSuccess(i: Int, i1: Int, s: String) {
                            mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_CONNECT,
                                Constants.ARG1_OPERATE_SUCCESS))
                        }

                        override fun onFailure(i: Int, i1: Int, i2: Int) {
                            mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_CONNECT,
                                Constants.ARG1_OPERATE_FAIL))
                        }
                    })
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewBinding.cameraVideoView.onPause()
        mCameraP2P?.let {
            if (isSpeaking) it.stopAudioTalk(null)
            if (isPlay) {
                it.stopPreview(object : OperationDelegateCallBack {
                    override fun onSuccess(sessionId: Int, requestId: Int, data: String) {}
                    override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {}
                })
                isPlay = false
            }
            it.removeOnP2PCameraListener()
            it.disconnect(object : OperationDelegateCallBack {
                override fun onSuccess(i: Int, i1: Int, s: String) {}
                override fun onFailure(i: Int, i1: Int, i2: Int) {}
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
        mCameraP2P?.destroyP2P()
    }

}