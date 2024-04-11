package com.tuya.smart.android.demo.camera

import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.fastjson.JSON
import com.thingclips.smart.android.camera.sdk.ThingIPCSdk
import com.thingclips.smart.camera.camerasdk.bean.ThingVideoSplitInfo
import com.thingclips.smart.camera.camerasdk.thingplayer.callback.AbsP2pCameraListener
import com.thingclips.smart.camera.camerasdk.thingplayer.callback.OperationDelegateCallBack
import com.thingclips.smart.camera.ipccamerasdk.p2p.ICameraP2P
import com.thingclips.smart.camera.middleware.p2p.IThingSmartCameraP2P
import com.thingclips.smart.camera.middleware.widget.AbsVideoViewCallback
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.ipc.yuv.monitor.utils.log.L
import com.thingclips.smart.sdk.api.IResultCallback
import com.tuya.smart.android.demo.camera.databinding.ActivityCameraMultiViewBinding
import com.tuya.smart.android.demo.camera.utils.Constants
import com.tuya.smart.android.demo.camera.utils.MessageUtil
import com.tuya.smart.android.demo.camera.utils.ToastUtil

/**
 * 多目预览
 */
class CameraMultiCamActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityCameraMultiViewBinding
    private var devId: String? = null
    private var mCameraP2P: IThingSmartCameraP2P<Any>? = null
    private var videoClarity = ICameraP2P.HD


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraMultiViewBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        initView()
        initData()
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
                ToastUtil.shortToast(this@CameraMultiCamActivity, s1)
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
        val layoutParams = viewBinding.multiVideo1.layoutParams
        layoutParams.width = width
        layoutParams.height = height
        val layoutParams2 = viewBinding.multiVideo2.layoutParams
        layoutParams2.width = width
        layoutParams2.height = height

    }

    private fun initData() {
        devId = intent.getStringExtra(Constants.INTENT_DEV_ID)
        ThingIPCSdk.getCameraInstance()?.let {
            mCameraP2P = it.createCameraP2P(devId)
        }

        if (null == devId || !isSupportVideoSegmentation(devId!!)) {
            ToastUtil.shortToast(this@CameraMultiCamActivity, "multi-camera not support!")
            finish()
            return
        }

        val cameraVideoSegmentationModel = getCameraVideoSegmentationModel(devId!!)
        val splitInfo = cameraVideoSegmentationModel?.split_info
        L.i(TAG, "splitInfo:" + JSON.toJSONString(splitInfo))
        // 获取分割镜头数，绑定对应的 view

        viewBinding.multiVideo1.setViewCallback(object : AbsVideoViewCallback() {
            override fun onCreated(o: Any) {
                super.onCreated(o)
                mCameraP2P?.generateCameraView(o)
                splitInfo?.get(0)?.let {
                    viewBinding.multiVideo1.setType(it.type)
                    viewBinding.multiVideo1.setIndex(it.index)
                }
            }
        })
        viewBinding.multiVideo1.createVideoView(devId)

        viewBinding.multiVideo2.setViewCallback(object : AbsVideoViewCallback() {
            override fun onCreated(o: Any) {
                super.onCreated(o)
                mCameraP2P?.generateCameraView(o)

                splitInfo?.get(1)?.let {
                    viewBinding.multiVideo2.setType(it.type)
                    viewBinding.multiVideo2.setIndex(it.index)
                }
            }
        })
        viewBinding.multiVideo2.createVideoView(devId)
    }

    private val p2pCameraListener: AbsP2pCameraListener = object : AbsP2pCameraListener() {

        override fun onSessionStatusChanged(camera: Any?, sessionId: Int, sessionStatus: Int) {
            super.onSessionStatusChanged(camera, sessionId, sessionStatus)
            if (sessionStatus == -3 || sessionStatus == -105) {
                //连接超时，可重连
            }
        }
    }

    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                Constants.MSG_CONNECT -> {
                    if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
                        preview();
                    } else {
                        ToastUtil.shortToast(
                            this@CameraMultiCamActivity,
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

    override fun onResume() {
        super.onResume()
        viewBinding.multiVideo1.onResume()
        viewBinding.multiVideo2.onResume()

        mCameraP2P?.let {
            it.registerP2PCameraListener(p2pCameraListener)
            it.generateCameraView(viewBinding.multiVideo1.createdView())
            it.generateCameraView(viewBinding.multiVideo2.createdView())
            if (it.isConnecting) {
                it.startPreview(object : OperationDelegateCallBack {
                    override fun onSuccess(sessionId: Int, requestId: Int, data: String) {
                    }

                    override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                    }
                })
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
        viewBinding.multiVideo1.onPause()
        viewBinding.multiVideo2.onPause()
        mCameraP2P?.removeOnP2PCameraListener(p2pCameraListener)
        stopPreview()
        mCameraP2P?.disconnect(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
        mCameraP2P?.destroyP2P()
    }

    companion object {
        private const val TAG = "CameraMultiCamActivity"
    }

    /**
     * 获取设备是否支持 IPC视频流分割高级能力
     */
    private fun isSupportVideoSegmentation(deviceId: String): Boolean {
        return ThingIPCSdk.getCameraInstance()
            ?.getCameraConfig(deviceId)?.isSupportVideoSegmentation ?: false
    }

    /**
     * 获取视频分割协议，与设备约定
     */
    private fun getCameraVideoSegmentationModel(deviceId: String): ThingVideoSplitInfo? {
        return ThingIPCSdk.getCameraInstance()
            ?.getCameraConfig(deviceId)?.cameraVideoSegmentationModel;
    }
}