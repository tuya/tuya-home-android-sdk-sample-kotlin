package com.tuya.smart.android.demo.camera

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.thingclips.smart.android.camera.sdk.ThingIPCSdk
import com.thingclips.smart.call.module.api.ThingCallSDK
import com.thingclips.smart.call.module.api.bean.ThingCall
import com.thingclips.smart.call.module.api.bean.ThingTargetState
import com.thingclips.smart.call.module.api.ui.ICallInterface
import com.thingclips.smart.call.module.api.ui.ICallInterfaceListener
import com.thingclips.smart.call.module.api.util.L
import com.thingclips.smart.camera.annotation.MuteStatus
import com.thingclips.smart.camera.camerasdk.thingplayer.callback.AbsP2pCameraListener
import com.thingclips.smart.camera.camerasdk.thingplayer.callback.OperationDelegateCallBack
import com.thingclips.smart.camera.ipccamerasdk.p2p.ICameraP2P
import com.thingclips.smart.camera.middleware.p2p.IThingSmartCameraP2P
import com.thingclips.smart.camera.middleware.widget.AbsVideoViewCallback
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.sdk.api.IResultCallback
import com.tuya.smart.android.demo.camera.databinding.ActivityCameraVideoCallBinding
import com.tuya.smart.android.demo.camera.utils.Constants
import com.tuya.smart.android.demo.camera.utils.MessageUtil
import com.tuya.smart.android.demo.camera.utils.ToastUtil

/**
 * 视频通话
 */
class CameraVideoCallActivity : AppCompatActivity(), ICallInterface {

    private lateinit var viewBinding: ActivityCameraVideoCallBinding
    private var devId: String? = null
    private var mCameraP2P: IThingSmartCameraP2P<Any>? = null
    private var videoClarity = ICameraP2P.HD

    private var isCallOut: Boolean = false
    private var isAccept: Boolean = false
    private var isOpenCamera: Boolean = false
    private var isVideoPause: Boolean = false

    /**
     * 视频对讲通道是否已建立
     */
    var videoTalkConnected: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraVideoCallBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        initView()
        initData()
        initListener()
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
                ToastUtil.shortToast(this@CameraVideoCallActivity, s1)
            }

            override fun onSuccess() {
                mHandler.removeCallbacksAndMessages(null)
                finish()
            }
        })
    }

    private fun initView() {
        setSupportActionBar(viewBinding.toolbarView)
        viewBinding.toolbarView.setNavigationOnClickListener {
            onBackPressed()
        }

        val windowManager = this.getSystemService(WINDOW_SERVICE) as WindowManager
        val width = windowManager.defaultDisplay.width
        val height = width * 9 / 16
        val layoutParams = viewBinding.cameraVideoView.layoutParams
        layoutParams.width = width
        layoutParams.height = height
    }

    private fun initData() {
        devId = intent.getStringExtra(Constants.INTENT_DEV_ID)
        // 呼叫信息
        call = intent.getParcelableExtra("thing_call")
        if (null == call || null == devId) {
            ToastUtil.shortToast(this@CameraVideoCallActivity, " not support!")
            finish()
            return
        }

        //是否是呼出会话
        isCallOut = call?.outgoing ?: false
        //通知 SDK 通话界面已加载
        ThingCallSDK.getCallModelService()?.launchUISuccess(call!!, this)

        //呼入通话，告知SDK响铃状态，App 侧可播放自定义铃声
        if (!isCallOut) {
            listener?.onRing()
            viewBinding.clCamMobileView.visibility = View.GONE
            viewBinding.btnScreenCamVideoAnswer.visibility = View.VISIBLE
        } else {
            viewBinding.clCamMobileView.visibility = View.VISIBLE
            viewBinding.btnScreenCamVideoAnswer.visibility = View.GONE
        }

        ThingIPCSdk.getCameraInstance()?.let {
            mCameraP2P = it.createCameraP2P(devId)
        }

        viewBinding.cameraVideoView.setViewCallback(object : AbsVideoViewCallback() {
            override fun onCreated(o: Any) {
                super.onCreated(o)
                mCameraP2P?.generateCameraView(o)
            }
        })
        viewBinding.cameraVideoView.createVideoView(devId)
    }

    private fun initListener() {
        viewBinding.ivCamMobileChange.setOnClickListener {
            chooseCamera()
        }

        viewBinding.btnScreenCamVideoAnswer.setOnClickListener {
            isAccept = true
            listener?.acceptClick()
            viewBinding.clCamMobileView.visibility = View.VISIBLE
            viewBinding.btnScreenCamVideoAnswer.visibility = View.GONE
        }

        viewBinding.btnScreenCamHungUp.setOnClickListener {
            isAccept = false
            listener?.hangupClick()
            finish()
        }
    }

    private val p2pCameraListener: AbsP2pCameraListener = object : AbsP2pCameraListener() {

        override fun onSessionStatusChanged(camera: Any?, sessionId: Int, sessionStatus: Int) {
            super.onSessionStatusChanged(camera, sessionId, sessionStatus)
            videoTalkConnected = false
            if (sessionStatus == -23 || sessionStatus == -104 || sessionStatus == -113) {
                // 设备忙线
                runOnUiThread {
                    ToastUtil.shortToast(this@CameraVideoCallActivity, "Device busy")
                    finish()
                }
            } else if (sessionStatus == -105 || sessionStatus == -3) {
                //连接超时，可重连
            } else {
                // 异常断开
                listener?.onConnect(false)
                finish()
            }
        }
    }

    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                Constants.MSG_CONNECT -> {
                    if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
                        preview()
                    } else {
                        ToastUtil.shortToast(
                            this@CameraVideoCallActivity,
                            getString(R.string.connect_failed)
                        )
                    }
                }
            }
            super.handleMessage(msg)
        }
    }

    private fun preview() {
        mCameraP2P?.startPreview(videoClarity, object : OperationDelegateCallBack {
            override fun onSuccess(sessionId: Int, requestId: Int, data: String) {
                Log.d(TAG, "start preview onSuccess")
                //双向视频铜壶监听云端状态回复是异步进行的，所以此处做个补偿判断
                //拉流成功，开启视频通话
                if (isCallOut) {
                    //呼出且收到设备回复
                    if (call?.targetState === ThingTargetState.ANSWER) {
                        setMute(MuteStatus.UNMUTE)
                        startTalk()
                        openCamera()
                        // 如果先收到设备回复answer，p2p 才连接成功，则直接建立视频对讲通道
                        connectCameraChannel()
                    }
                } else {
                    //呼入已接听
                    if (isAccept) {
                        //呼入，等云端回复接听抢占成功之后再开启对讲，否则会占用设备通道，产生busy
                        if (call?.targetState == ThingTargetState.ALREADY_ANSWERED) {
                            setMute(MuteStatus.UNMUTE);
                            startTalk()
                            openCamera()
                            connectCameraChannel();
                        }
                    } else {
                        setMute(MuteStatus.MUTE);
                    }
                }
            }

            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                Log.d(TAG, "start preview onFailure, errCode: $errCode")
            }
        })
    }

    private fun stopPreview() {
        mCameraP2P?.stopPreview(object : OperationDelegateCallBack {
            override fun onSuccess(sessionId: Int, requestId: Int, data: String?) {
            }

            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
            }
        })
    }

    private fun setMute(status: Int) {
        mCameraP2P?.setMute(status, object : OperationDelegateCallBack {
            override fun onSuccess(sessionId: Int, requestId: Int, data: String?) {
                L.i(TAG, "setMute $data")
            }

            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                L.i(TAG, "setMute fail")
            }
        })
    }

    private fun startTalk() {
        //开启对讲
        if (mCameraP2P?.isTalking == true) {
            return
        }
        if (Constants.hasRecordPermission()) {
            mCameraP2P?.startAudioTalk(object : OperationDelegateCallBack {
                override fun onSuccess(sessionId: Int, requestId: Int, data: String?) {
                    L.i(TAG, "startAudioTalk $data")
                }

                override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                    L.i(TAG, "startAudioTalk fail:$errCode")
                }
            })
        } else {
            Constants.requestPermission(
                this@CameraVideoCallActivity,
                Manifest.permission.RECORD_AUDIO,
                Constants.EXTERNAL_AUDIO_REQ_CODE,
                "open_recording"
            )
        }
    }

    private fun stopTalk() {
        mCameraP2P?.stopAudioTalk(object : OperationDelegateCallBack {
            override fun onSuccess(sessionId: Int, requestId: Int, data: String?) {
                L.i(TAG, "stopAudioTalk $data")
            }

            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                L.i(TAG, "stopAudioTalk fail")
            }
        })
    }

    private fun disconnect() {
        disConnectCameraChannel()
        mCameraP2P?.disconnect(null)
        listener?.onDisconnect()
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        viewBinding.cameraVideoView.onResume()
        mCameraP2P?.let {
            it.registerP2PCameraListener(p2pCameraListener)
            it.generateCameraView(viewBinding.cameraVideoView.createdView())
            it.generateCameraView(viewBinding.ivCamMobileView)
            if (it.isConnecting) {
                preview()
            } else {
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

    override fun onPause() {
        super.onPause()
        viewBinding.cameraVideoView.onPause()
        setMute(MuteStatus.MUTE)
        stopTalk()
        stopPreview()
        disconnect()
        mCameraP2P?.removeOnP2PCameraListener(p2pCameraListener)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
        mCameraP2P?.destroyP2P()
        listener?.onViewDestroy()
        listener = null
        call = null
    }

    companion object {
        private const val TAG = "CameraVideoCallActivity"
    }

    override var call: ThingCall? = null

    override var listener: ICallInterfaceListener? = null

    override fun callCancel(call: ThingCall) {
        L.i(TAG, "callCancel :" + call.targetId)
        val name =
            ThingIPCSdk.getHomeProxy().dataInstance.getDeviceBean(call.targetId)?.getName()
                ?: ""
        runOnUiThread {
            ToastUtil.shortToast(this, "$name Called you and hung up")
        }
    }

    override fun callStart(call: ThingCall) {
        L.i(TAG, "callStart :" + call.targetId)
    }

    override fun callUpdate(call: ThingCall) {
        this.call = call
        // 呼叫状态 页面更新
        L.i(TAG, "call.targetState --" + call.targetState)
        when (call.targetState) {
            ThingTargetState.INITIATING -> {
            }

            ThingTargetState.RINGING -> {
            }

            ThingTargetState.CALLING -> {
            }

            ThingTargetState.ANSWER,
            ThingTargetState.ALREADY_ANSWERED,
            -> {
                //协议层收到对端已接听/收到对方接听消息，开启视频传输，先建立p2p视频通道，在开启采集
                if (mCameraP2P?.isConnecting == true) {
                    //开启声音
                    setMute(MuteStatus.UNMUTE)
                    //开启对讲
                    startTalk()
                    //开启本地摄像头
                    openCamera()
                    //p2p 已连接，建立视频通道发流
                    connectCameraChannel()
                }
            }

            ThingTargetState.CANCEL -> {
                runOnUiThread {
                    ToastUtil.shortToast(this, "The other party has canceled")
                    finish()
                }
            }

            ThingTargetState.REJECT -> {
            }

            ThingTargetState.OTHER_ANSWERED -> {
                runOnUiThread {
                    ToastUtil.shortToast(this, "Call answered")
                    finish()
                }
            }

            ThingTargetState.ALREADY_REJECTED -> {
                runOnUiThread {
                    ToastUtil.shortToast(this, "Call rejected")
                    finish()
                }
            }

            ThingTargetState.BUSY -> {
                runOnUiThread {
                    ToastUtil.shortToast(this, "Call busy")
                    finish()
                }
            }

            ThingTargetState.HANG_UP,
            ThingTargetState.STOP,
            -> {
                runOnUiThread {
                    ToastUtil.shortToast(this, "Call end")
                    finish()
                }
            }

            ThingTargetState.ERROR -> {
                runOnUiThread {
                    ToastUtil.shortToast(this, "Call exception")
                    finish()
                }
            }

            else -> {}
        }
    }

    override fun timeout() {
        runOnUiThread {
            ToastUtil.shortToast(this, "Call Timeout")
            finish()
        }
    }

    /**
     * 开启本地摄像头
     */
    private fun openCamera() {
        runOnUiThread {
            //检查权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    100
                )
            } else {
                if (!isOpenCamera) {
                    isOpenCamera = mCameraP2P?.startVideoCapture() ?: false
                }
                //暂停则恢复播放
                if (isVideoPause) {
                    resumeVideoTalk()
                }

                viewBinding.ivCamMobileView.onResume()
            }
        }
    }

    /**
     *  建立本地视频数据传输通道
     */
    private fun connectCameraChannel() {
        if (videoTalkConnected) {
            return
        }
        mCameraP2P?.startVideoTalk(object : OperationDelegateCallBack {
            override fun onSuccess(sessionId: Int, requestId: Int, data: String?) {
                videoTalkConnected = true
            }

            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                videoTalkConnected = false
            }
        })
    }

    /**
     *  关闭本地摄像头
     */
    private fun closeCamera() {
        //关闭采集
        mCameraP2P?.stopVideoCapture()
        isOpenCamera = false
        viewBinding.ivCamMobileView.onPause()
    }

    /**
     * 恢复本地视频数采集
     */
    private fun resumeVideoTalk() {
        mCameraP2P?.resumeVideoTalk(object : OperationDelegateCallBack {
            override fun onSuccess(sessionId: Int, requestId: Int, data: String) {
                isVideoPause = false
            }

            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
            }
        })
    }

    /**
     * 暂停本地视频数据采集
     */
    private fun pauseVideoTalk() {
        mCameraP2P?.pauseVideoTalk(object : OperationDelegateCallBack {
            override fun onSuccess(sessionId: Int, requestId: Int, data: String?) {
                isVideoPause = true
            }

            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
            }
        })
    }

    /**
     * 切换前后置摄像头
     */
    private fun chooseCamera() {
        runOnUiThread { mCameraP2P?.switchCamera() }
    }

    /**
     *  退出关闭
     */
    private fun disConnectCameraChannel() {
        isOpenCamera = false
        isVideoPause = false
        //停止采集
        closeCamera()
        videoTalkConnected = false
        //断开视频传输通道
        mCameraP2P?.stopVideoTalk(null)
    }
}