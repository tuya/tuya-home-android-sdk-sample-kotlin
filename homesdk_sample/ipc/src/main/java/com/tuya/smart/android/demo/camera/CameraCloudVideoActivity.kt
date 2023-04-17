package com.tuya.smart.android.demo.camera

import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.thingclips.smart.android.camera.sdk.ThingIPCSdk
import com.tuya.smart.android.demo.camera.databinding.ActivityCameraCloudVideoBinding
import com.tuya.smart.android.demo.camera.utils.Constants
import com.tuya.smart.android.demo.camera.utils.MessageUtil
import com.tuya.smart.android.demo.camera.utils.ToastUtil
import com.thingclips.smart.camera.camerasdk.thingplayer.callback.AbsP2pCameraListener
import com.thingclips.smart.camera.camerasdk.thingplayer.callback.IRegistorIOTCListener
import com.thingclips.smart.camera.camerasdk.thingplayer.callback.OperationCallBack
import com.thingclips.smart.camera.camerasdk.thingplayer.callback.OperationDelegateCallBack
import com.thingclips.smart.camera.ipccamerasdk.msgvideo.IThingCloudVideo
import com.thingclips.smart.camera.ipccamerasdk.p2p.ICameraP2P
import org.json.JSONObject

/**
 *  Cloud Video Message
 *  @author houqing <a href="mailto:developer@tuya.com"/>
 *  @since 2021/7/27 5:50 PM
 */
class CameraCloudVideoActivity : AppCompatActivity() {
    private val OPERATE_SUCCESS = 1
    private val OPERATE_FAIL = 0
    private val MSG_CLOUD_VIDEO_DEVICE = 1000
    private var mCloudVideo: IThingCloudVideo? = null
    private var playUrl: String? = null
    private var encryptKey: String? = null
    private var playDuration = 0
    private var cachePath: String? = null
    private var mDevId: String? = null
    private var previewMute = ICameraP2P.MUTE
    private lateinit var viewBinding: ActivityCameraCloudVideoBinding
    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_CLOUD_VIDEO_DEVICE -> startplay()
                Constants.MSG_MUTE -> handleMute(msg)
            }
            super.handleMessage(msg)
        }
    }

    private fun startplay() {
        mCloudVideo?.playVideo(playUrl, 0, encryptKey, object : OperationCallBack {
            override fun onSuccess(i: Int, i1: Int, s: String?, o: Any) {
                Log.d("mcloudCamera", "onsuccess")
            }

            override fun onFailure(i: Int, i1: Int, i2: Int, o: Any) {}
        }, object : OperationCallBack {
            override fun onSuccess(i: Int, i1: Int, s: String?, o: Any) {
                Log.d("mcloudCamera", "finish onsuccess")
            }

            override fun onFailure(i: Int, i1: Int, i2: Int, o: Any) {}
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraCloudVideoBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        initData()
        initView()
        initCloudCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        mCloudVideo?.stopVideo(null)
        mCloudVideo?.removeOnDelegateP2PCameraListener()
        mCloudVideo?.deinitCloudVideo()
    }

    private fun initData() {
        playUrl = intent.getStringExtra("playUrl")
        encryptKey = intent.getStringExtra("encryptKey")
        playDuration = intent.getIntExtra("playDuration", 0)
        mDevId = intent.getStringExtra("devId")
        cachePath = application.cacheDir.path
    }

    private fun initCloudCamera() {
        mCloudVideo = ThingIPCSdk.getMessage()?.run { this.createVideoMessagePlayer() }
        mCloudVideo?.let {
            it.registerP2PCameraListener(object : AbsP2pCameraListener() {
                override fun receiveFrameDataForMediaCodec(
                    i: Int,
                    bytes: ByteArray,
                    i1: Int,
                    i2: Int,
                    bytes1: ByteArray,
                    b: Boolean,
                    i3: Int
                ) {
                    super.receiveFrameDataForMediaCodec(i, bytes, i1, i2, bytes1, b, i3)
                }
            })
            val listener: Any? = viewBinding.cameraCloudVideoView.createdView()
            if (listener != null) {
                it.generateCloudCameraView(listener as IRegistorIOTCListener)
            }
            it.createCloudDevice(cachePath, mDevId, object : OperationDelegateCallBack {
                override fun onSuccess(sessionId: Int, requestId: Int, data: String?) {
                    mHandler.sendMessage(
                        MessageUtil.getMessage(
                            MSG_CLOUD_VIDEO_DEVICE,
                            OPERATE_SUCCESS
                        )
                    )
                }

                override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {}
            })
        }
    }

    private fun initView() {
        viewBinding.cameraCloudVideoView.createVideoView(mDevId)
        viewBinding.btnPauseVideoMsg.setOnClickListener {
            mCloudVideo?.pauseVideo(null)
        }
        viewBinding.btnResumeVideoMsg.setOnClickListener {
            mCloudVideo?.resumeVideo(null)
        }
        viewBinding.cameraMute.setOnClickListener { muteClick() }
        viewBinding.cameraMute.isSelected = true
    }

    private fun muteClick() {
        mCloudVideo?.let {
            val mute = if (previewMute == ICameraP2P.MUTE) ICameraP2P.UNMUTE else ICameraP2P.MUTE
            it.setCloudVideoMute(mute, object : OperationDelegateCallBack {
                override fun onSuccess(sessionId: Int, requestId: Int, data: String?) {
                    val jsonObject = com.alibaba.fastjson.JSONObject.parseObject(data)
                    val value = jsonObject["mute"]
                    previewMute = Integer.valueOf(value.toString())
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
    }

    private fun handleMute(msg: Message) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
            viewBinding.cameraMute.isSelected = previewMute == ICameraP2P.MUTE
        } else {
            ToastUtil.shortToast(
                this@CameraCloudVideoActivity,
                getString(R.string.operation_failed)
            )
        }
    }
}