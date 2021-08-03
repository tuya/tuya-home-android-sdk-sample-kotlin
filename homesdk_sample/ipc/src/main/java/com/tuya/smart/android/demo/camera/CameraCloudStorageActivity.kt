package com.tuya.smart.android.demo.camera

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tuya.appsdk.sample.resource.HomeModel
import com.tuya.smart.android.camera.sdk.TuyaIPCSdk
import com.tuya.smart.android.demo.camera.databinding.ActivityCameraCloudStorageBinding
import com.tuya.smart.android.demo.camera.utils.Constants
import com.tuya.smart.android.demo.camera.utils.ToastUtil
import com.tuya.smart.api.service.MicroServiceManager
import com.tuya.smart.camera.camerasdk.typlayer.callback.IRegistorIOTCListener
import com.tuya.smart.camera.camerasdk.typlayer.callback.OnP2PCameraListener
import com.tuya.smart.camera.camerasdk.typlayer.callback.OperationCallBack
import com.tuya.smart.camera.camerasdk.typlayer.callback.OperationDelegateCallBack
import com.tuya.smart.camera.cloud.purchase.AbsCameraCloudPurchaseService
import com.tuya.smart.camera.cloud.purchase.AbsCloudCallback
import com.tuya.smart.camera.ipccamerasdk.cloud.ITYCloudCamera
import com.tuya.smart.camera.middleware.cloud.CameraCloudSDK
import com.tuya.smart.camera.middleware.cloud.ICloudCacheManagerCallback
import com.tuya.smart.camera.middleware.cloud.bean.CloudDayBean
import com.tuya.smart.camera.middleware.cloud.bean.TimePieceBean
import com.tuya.smart.camera.middleware.cloud.bean.TimeRangeBean
import com.tuya.smart.camera.middleware.widget.AbsVideoViewCallback
import com.tuya.smart.home.sdk.TuyaHomeSdk
import java.io.File
import java.nio.ByteBuffer
import java.util.*

/**
 * TODO feature
 *云视频
 * CloudStorage Video
 * @author hou qing <a href="mailto:developer@tuya.com"/>
 * @since 2021/7/28 10:24 上午
 */
class CameraCloudStorageActivity:AppCompatActivity(), ICloudCacheManagerCallback {
    private lateinit var cameraCloudSDK: CameraCloudSDK
    private var cloudCamera: ITYCloudCamera? = null
    private val dayBeanList: MutableList<CloudDayBean> = ArrayList()
    private val timePieceBeans: MutableList<TimePieceBean> = ArrayList()
    private var mEncryptKey = ""
    private var mAuthorityJson = ""
    private var soundState = 0
    private var p2pType = 0
    private var devId: String? = null
    private lateinit var viewBinding: ActivityCameraCloudStorageBinding
    private var cloudState =1001
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraCloudStorageBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        devId = intent.getStringExtra(Constants.INTENT_DEV_ID)
        p2pType = intent.getIntExtra(Constants.INTENT_P2P_TYPE, -1)
        cameraCloudSDK = CameraCloudSDK()
        cloudCamera = TuyaIPCSdk.getCloud()?.createCloudCamera()
        viewBinding.cameraCloudVideoView.setViewCallback(object : AbsVideoViewCallback() {
            override fun onCreated(o: Any) {
                super.onCreated(o)
                if (o is IRegistorIOTCListener) {
                    cloudCamera?.generateCloudCameraView(o)
                }
            }
        })
        viewBinding.cameraCloudVideoView.createVideoView(p2pType)
        cloudCamera?.createCloudDevice(application.cacheDir.path, devId)
        viewBinding.toolbarView.setNavigationOnClickListener{onBackPressed()}
        viewBinding.statusBtn.setOnClickListener {
            cameraCloudSDK.getCameraCloudInfo(TuyaHomeSdk.getDataInstance().getDeviceBean(devId), this@CameraCloudStorageActivity)
        }
        viewBinding.buyBtn.setOnClickListener {
            // TODO: 2021/7/29 购买云存储，接入方式可参考业务包接入文档 https://developer.tuya.com/cn/docs/app-development/cloudstorage?id=Ka8qhzjzay7fx
            //TODO use cloud service purchase component https://developer.tuya.com/cn/docs/app-development/cloudstorage?id=Ka8qhzjzay7fx
//            val cameraCloudService: AbsCameraCloudPurchaseService = MicroServiceManager.getInstance().findServiceByInterface(AbsCameraCloudPurchaseService::class.java.name)
//            cameraCloudService?.buyCloudStorage(
//                this,
//                TuyaHomeSdk.getDataInstance().getDeviceBean(devId),
//                HomeModel.INSTANCE.getCurrentHome(this).toString(), object: AbsCloudCallback() {
//                    override fun onError(errorCode: String?, errorMessage: String?) {
//                        super.onError(errorCode, errorMessage)
//                    }
//                }
//            )
        }
        viewBinding.queryBtn.setOnClickListener {
            cameraCloudSDK.getCloudMediaCount(devId, TimeZone.getDefault().id, this@CameraCloudStorageActivity)
        }
        viewBinding.queryTimeBtn.setOnClickListener { //Get the time data of the first day queried
            if (dayBeanList.isNotEmpty()) {
                getAppointedDayCloudTimes(dayBeanList[0])
            }
        }
        viewBinding.startBtn.setOnClickListener {
            if (timePieceBeans.isNotEmpty()) {
                playCloudDataWithStartTime(timePieceBeans[0].getStartTime(), timePieceBeans[0].getEndTime(), true)
            }
        }
        viewBinding.pauseBtn.setOnClickListener { pausePlayCloudVideo() }

        viewBinding.resumeBtn.setOnClickListener { resumePlayCloudVideo() }

        viewBinding.stopBtn.setOnClickListener { stopPlayCloudVideo() }
        viewBinding.cameraMute.setOnClickListener { setMuteValue(if (soundState == 0) 1 else 0) }
        viewBinding.cameraMute.isSelected = soundState == 0
        viewBinding.snapshotBtn.setOnClickListener { snapshot() }
        viewBinding.recordStart.setOnClickListener { startCloudRecordLocalMP4() }
        viewBinding.recordEnd.setOnClickListener { stopCloudRecordLocalMP4() }

    }
    override fun onResume() {
        super.onResume()
        viewBinding.cameraCloudVideoView.onResume()
        cloudCamera?.let {
            if (viewBinding.cameraCloudVideoView.createdView() is IRegistorIOTCListener) {
                it.generateCloudCameraView(viewBinding.cameraCloudVideoView.createdView() as IRegistorIOTCListener)
            }
            it.registorOnP2PCameraListener(object : OnP2PCameraListener {
                override fun receiveFrameDataForMediaCodec(i: Int, bytes: ByteArray, i1: Int, i2: Int, bytes1: ByteArray, b: Boolean, i3: Int) {
                }
                override fun onReceiveFrameYUVData(i: Int, byteBuffer: ByteBuffer, byteBuffer1: ByteBuffer, byteBuffer2: ByteBuffer, i1: Int, i2: Int, i3: Int, i4: Int, l: Long, l1: Long, l2: Long, o: Any) {
                }
                override fun onSessionStatusChanged(o: Any, i: Int, i1: Int) {}
                override fun onReceiveAudioBufferData(i: Int, i1: Int, i2: Int, l: Long, l1: Long, l2: Long) {
                }
                override fun onReceiveSpeakerEchoData(byteBuffer: ByteBuffer, i: Int) {}
            })
        }
    }

    override fun onPause() {
        super.onPause()
        viewBinding.cameraCloudVideoView.onPause()
        cloudCamera?.removeOnP2PCameraListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraCloudSDK.onDestroy()
        cloudCamera?.destroyCloudBusiness()
        cloudCamera?.deinitCloudCamera()
    }
   private fun startCloudRecordLocalMP4() {
       cloudCamera?.let{
            val path = getExternalFilesDir(null)!!.path + "/" + devId
            val file = File(path)
            if (!file.exists()) {
                file.mkdirs()
            }
            it.startRecordLocalMp4(path, System.currentTimeMillis().toString() + ".mp4", object : OperationDelegateCallBack {
                    override fun onSuccess(sessionId: Int, requestId: Int, data: String) {
                        ToastUtil.shortToast(this@CameraCloudStorageActivity, getString(R.string.operation_suc))
                    }

                    override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                        ToastUtil.shortToast(this@CameraCloudStorageActivity, getString(R.string.operation_failed)+" errCode=$errCode")
                    }
                })
        }
    }

    /**
     * record stop
     */
    private fun stopCloudRecordLocalMP4() {
        cloudCamera?.stopRecordLocalMp4(object : OperationDelegateCallBack {
            override fun onSuccess(sessionId: Int, requestId: Int, data: String) {
                ToastUtil.shortToast(this@CameraCloudStorageActivity, getString(R.string.operation_suc))
            }

            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                ToastUtil.shortToast(this@CameraCloudStorageActivity, getString(R.string.operation_failed)+" errCode=$errCode")
            }
        })
    }
   private fun snapshot() {
        cloudCamera?.let {
            val path = getExternalFilesDir(null)!!.path + "/" + devId
            val file = File(path)
            if (!file.exists()) {
                file.mkdirs()
            }
            it.snapshot(path, object : OperationDelegateCallBack {
                override fun onSuccess(sessionId: Int, requestId: Int, data: String) {
                    ToastUtil.shortToast(this@CameraCloudStorageActivity, getString(R.string.operation_suc))
                }

                override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                    ToastUtil.shortToast(this@CameraCloudStorageActivity, getString(R.string.operation_failed)+" errCode=$errCode")
                }
            })
        }
    }
    private fun setMuteValue(mute: Int) {
        cloudCamera?.setCloudMute(mute, object : OperationDelegateCallBack {
            override fun onSuccess(sessionId: Int, requestId: Int, data: String) {
                soundState = Integer.valueOf(data)
                viewBinding.cameraMute.isSelected = soundState == 0
            }

            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                ToastUtil.shortToast(this@CameraCloudStorageActivity, getString(R.string.operation_failed))
            }
        })
    }
    private fun stopPlayCloudVideo() {
        cloudCamera?.stopPlayCloudVideo(object : OperationDelegateCallBack {
            override fun onSuccess(sessionId: Int, requestId: Int, data: String) {}
            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                ToastUtil.shortToast(this@CameraCloudStorageActivity, getString(R.string.operation_failed))
            }
        })
    }
    private fun resumePlayCloudVideo() {
        cloudCamera?.resumePlayCloudVideo(object : OperationDelegateCallBack {
            override fun onSuccess(sessionId: Int, requestId: Int, data: String) {}
            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                ToastUtil.shortToast(this@CameraCloudStorageActivity, getString(R.string.operation_failed)+" errCode=$errCode")
            }
        })
    }
    private fun pausePlayCloudVideo() {
        cloudCamera?.pausePlayCloudVideo(object : OperationDelegateCallBack {
            override fun onSuccess(sessionId: Int, requestId: Int, data: String) {}
            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                ToastUtil.shortToast(this@CameraCloudStorageActivity, getString(R.string.operation_failed)+" errCode=$errCode")
            }
        })
    }
    private fun playCloudDataWithStartTime(startTime: Int, endTime: Int, isEvent: Boolean) {
            cloudCamera?.playCloudDataWithStartTime(startTime.toLong(), endTime.toLong(), isEvent,
                mAuthorityJson, mEncryptKey,
                object : OperationCallBack {
                    override fun onSuccess(sessionId: Int, requestId: Int, data: String, camera: Any) {
                        //playing
                    }

                    override fun onFailure(sessionId: Int, requestId: Int, errCode: Int, camera: Any) {
                    }
                }, object : OperationCallBack {
                    override fun onSuccess(sessionId: Int, requestId: Int, data: String, camera: Any) {
                        //playCompleted
                    }

                    override fun onFailure(sessionId: Int, requestId: Int, errCode: Int, camera: Any) {
                    }
                })
    }
    private fun getAppointedDayCloudTimes(dayBean: CloudDayBean?) {
        dayBean?.let {
            getTimeLineInfoByTimeSlice(devId, it.currentStartDayTime.toString(), it.currentDayEndTime.toString())
        }
    }
    private fun getTimeLineInfoByTimeSlice(devId: String?, timeGT: String?, timeLT: String?) {
        cameraCloudSDK.getTimeLineInfoByTimeSlice(devId, timeGT, timeLT, this)
    }
    override fun getCloudDayList(cloudDayBeanList: MutableList<CloudDayBean>) {

        //Get the date with data
        dayBeanList.clear()
        dayBeanList.addAll(cloudDayBeanList)
    }

    override fun getCloudSecret(encryKey: String) {
        mEncryptKey = encryKey
    }

    override fun getAuthorityGet(authorityJson: String) {
        mAuthorityJson = authorityJson
    }

    override fun getTimePieceInfoByTimeSlice(list: MutableList<TimePieceBean>) {
        timePieceBeans.clear()
        timePieceBeans.addAll(list)
    }

    override fun getMotionDetectionByTimeSlice(p0: MutableList<TimeRangeBean>?) {
    }

    override fun onError(errorCode: Int) {
        ToastUtil.shortToast(this, getString(R.string.err_code) + errorCode)
    }

    override fun getCloudStatusSuccess(i: Int) {
        //Get cloud storage status
        cloudState = i
        ToastUtil.shortToast(this, getString(R.string.current_state) + i)
    }

    override fun getCloudConfigDataTags(config: String) {
        cloudCamera?.configCloudDataTagsV1(config, object : OperationDelegateCallBack {
            override fun onSuccess(i: Int, i1: Int, s: String) {
                if (timePieceBeans.isNotEmpty()) {
                    val startTime = timePieceBeans[0].getStartTime()
                    var endTime = (getTodayEnd(startTime * 1000L) / 1000).toInt()
                    playCloudDataWithStartTime(startTime, endTime - 1, true)
                }
            }

            override fun onFailure(i: Int, i1: Int, i2: Int) {}
        })
    }
    fun getTodayEnd(currentTime: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.time = Date(currentTime)
        calendar[Calendar.HOUR_OF_DAY] = 0
        calendar[Calendar.MINUTE] = 0
        calendar[Calendar.SECOND] = 0
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        return calendar.timeInMillis
    }
}