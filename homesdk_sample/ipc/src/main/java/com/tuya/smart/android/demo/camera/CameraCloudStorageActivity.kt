package com.tuya.smart.android.demo.camera

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tuya.smart.android.camera.sdk.TuyaIPCSdk
import com.tuya.smart.android.camera.sdk.bean.CloudStatusBean
import com.tuya.smart.android.demo.camera.databinding.ActivityCameraCloudStorageBinding
import com.tuya.smart.android.demo.camera.utils.Constants
import com.tuya.smart.android.demo.camera.utils.ToastUtil
import com.tuya.smart.camera.camerasdk.bean.TuyaVideoFrameInfo
import com.tuya.smart.camera.camerasdk.typlayer.callback.IRegistorIOTCListener
import com.tuya.smart.camera.camerasdk.typlayer.callback.OnP2PCameraListener
import com.tuya.smart.camera.camerasdk.typlayer.callback.OperationCallBack
import com.tuya.smart.camera.camerasdk.typlayer.callback.OperationDelegateCallBack
import com.tuya.smart.camera.ipccamerasdk.cloud.ITYCloudCamera
import com.tuya.smart.camera.middleware.cloud.bean.CloudDayBean
import com.tuya.smart.camera.middleware.cloud.bean.TimePieceBean
import com.tuya.smart.camera.middleware.cloud.bean.TimeRangeBean
import com.tuya.smart.camera.middleware.widget.AbsVideoViewCallback
import com.tuya.smart.home.sdk.callback.ITuyaResultCallback
import java.io.File
import java.nio.ByteBuffer
import java.util.*

/**
 * CloudStorage Video
 * @author hou qing <a href="mailto:developer@tuya.com"/>
 * @since 2021/7/28 10:24 AM
 */
class CameraCloudStorageActivity : AppCompatActivity() {

    private var cloudCamera: ITYCloudCamera? = null
    private val dayBeanList: MutableList<CloudDayBean> = ArrayList()
    private val timePieceBeans = ArrayList<TimePieceBean>()
    private var soundState = 0
    private var devId: String? = null
    private lateinit var viewBinding: ActivityCameraCloudStorageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraCloudStorageBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        devId = intent.getStringExtra(Constants.INTENT_DEV_ID)
        cloudCamera = TuyaIPCSdk.getCloud()?.createCloudCamera()
        viewBinding.cameraCloudVideoView.setViewCallback(object : AbsVideoViewCallback() {
            override fun onCreated(o: Any) {
                super.onCreated(o)
                if (o is IRegistorIOTCListener) {
                    cloudCamera?.generateCloudCameraView(o)
                }
            }
        })
        viewBinding.cameraCloudVideoView.createVideoView(devId)
        cloudCamera?.createCloudDevice(application.cacheDir.path, devId)
        viewBinding.toolbarView.setNavigationOnClickListener { onBackPressed() }
        viewBinding.statusBtn.setOnClickListener {
            cloudCamera?.queryCloudServiceStatus(
                devId,
                object : ITuyaResultCallback<CloudStatusBean> {
                    override fun onSuccess(result: CloudStatusBean) {
                        //Get cloud storage status
                        ToastUtil.shortToast(
                            this@CameraCloudStorageActivity,
                            getString(R.string.current_state) + result.status
                        )
                    }

                    override fun onError(errorCode: String, errorMessage: String) {
                        ToastUtil.shortToast(
                            this@CameraCloudStorageActivity,
                            getString(R.string.err_code) + errorCode
                        )
                    }
                })
        }
        viewBinding.buyBtn.setOnClickListener {
            //TODO use cloud service purchase component https://developer.tuya.com/cn/docs/app-development/cloudstorage?id=Ka8qhzjzay7fx

        }
        viewBinding.queryBtn.setOnClickListener {
            //1. Get device cloud storage-related data
            cloudCamera?.getCloudDays(devId, object : ITuyaResultCallback<List<CloudDayBean>?> {
                override fun onSuccess(result: List<CloudDayBean>?) {
                    if (result == null || result.isEmpty()) {
                        ToastUtil.shortToast(
                            this@CameraCloudStorageActivity,
                            getString(R.string.no_data)
                        )
                    } else {
                        dayBeanList.clear()
                        dayBeanList.addAll(result)
                        ToastUtil.shortToast(
                            this@CameraCloudStorageActivity,
                            getString(R.string.operation_suc)
                        )
                    }
                }

                override fun onError(errorCode: String, errorMessage: String) {
                    ToastUtil.shortToast(
                        this@CameraCloudStorageActivity,
                        getString(R.string.err_code) + errorCode
                    )
                }
            })
        }
        viewBinding.queryTimeBtn.setOnClickListener { //Get the time data of the first day queried
            //2. Get time slice at a specified time
            if (dayBeanList.size > 0) {
                getAppointedDayCloudTimes(dayBeanList[0])
            } else {
                ToastUtil.shortToast(this@CameraCloudStorageActivity, getString(R.string.no_data))
            }
        }
        viewBinding.startBtn.setOnClickListener {
            if (timePieceBeans.size > 0) {
                playCloudDataWithStartTime(
                    timePieceBeans[0].startTime,
                    timePieceBeans[0].endTime,
                    true
                )
                ToastUtil.shortToast(
                    this@CameraCloudStorageActivity,
                    getString(R.string.operation_suc)
                )
            } else {
                ToastUtil.shortToast(this@CameraCloudStorageActivity, getString(R.string.no_data))
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
                override fun receiveFrameDataForMediaCodec(
                    i: Int,
                    bytes: ByteArray,
                    i1: Int,
                    i2: Int,
                    bytes1: ByteArray,
                    b: Boolean,
                    i3: Int
                ) {
                }

                override fun onReceiveFrameYUVData(
                    i: Int,
                    byteBuffer: ByteBuffer,
                    byteBuffer1: ByteBuffer,
                    byteBuffer2: ByteBuffer,
                    i1: Int,
                    i2: Int,
                    i3: Int,
                    i4: Int,
                    l: Long,
                    l1: Long,
                    l2: Long,
                    o: Any
                ) {
                }

                override fun onReceiveFrameYUVData(
                    sessionId: Int,
                    y: ByteBuffer?,
                    u: ByteBuffer?,
                    v: ByteBuffer?,
                    videoFrameInfo: TuyaVideoFrameInfo?,
                    camera: Any?
                ) {
                    TODO("Not yet implemented")
                }

                override fun onSessionStatusChanged(o: Any, i: Int, i1: Int) {}
                override fun onReceiveAudioBufferData(
                    i: Int,
                    i1: Int,
                    i2: Int,
                    l: Long,
                    l1: Long,
                    l2: Long
                ) {
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
        cloudCamera?.destroy()
        cloudCamera?.deinitCloudCamera()
    }

    private fun startCloudRecordLocalMP4() {
        cloudCamera?.let {
            val path = getExternalFilesDir(null)!!.path + "/" + devId
            val file = File(path)
            if (!file.exists()) {
                file.mkdirs()
            }
            it.startRecordLocalMp4(
                path,
                System.currentTimeMillis().toString() + ".mp4",
                object : OperationDelegateCallBack {
                    override fun onSuccess(sessionId: Int, requestId: Int, data: String) {
                        ToastUtil.shortToast(
                            this@CameraCloudStorageActivity,
                            getString(R.string.operation_suc)
                        )
                    }

                    override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                        ToastUtil.shortToast(
                            this@CameraCloudStorageActivity,
                            getString(R.string.operation_failed) + " errCode=$errCode"
                        )
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
                ToastUtil.shortToast(
                    this@CameraCloudStorageActivity,
                    getString(R.string.operation_suc)
                )
            }

            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                ToastUtil.shortToast(
                    this@CameraCloudStorageActivity,
                    getString(R.string.operation_failed) + " errCode=$errCode"
                )
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
                    ToastUtil.shortToast(
                        this@CameraCloudStorageActivity,
                        getString(R.string.operation_suc)
                    )
                }

                override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                    ToastUtil.shortToast(
                        this@CameraCloudStorageActivity,
                        getString(R.string.operation_failed) + " errCode=$errCode"
                    )
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
                ToastUtil.shortToast(
                    this@CameraCloudStorageActivity,
                    getString(R.string.operation_failed)
                )
            }
        })
    }

    private fun stopPlayCloudVideo() {
        cloudCamera?.stopPlayCloudVideo(object : OperationDelegateCallBack {
            override fun onSuccess(sessionId: Int, requestId: Int, data: String) {}
            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                ToastUtil.shortToast(
                    this@CameraCloudStorageActivity,
                    getString(R.string.operation_failed)
                )
            }
        })
    }

    private fun resumePlayCloudVideo() {
        cloudCamera?.resumePlayCloudVideo(object : OperationDelegateCallBack {
            override fun onSuccess(sessionId: Int, requestId: Int, data: String) {}
            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                ToastUtil.shortToast(
                    this@CameraCloudStorageActivity,
                    getString(R.string.operation_failed) + " errCode=$errCode"
                )
            }
        })
    }

    private fun pausePlayCloudVideo() {
        cloudCamera?.pausePlayCloudVideo(object : OperationDelegateCallBack {
            override fun onSuccess(sessionId: Int, requestId: Int, data: String) {}
            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                ToastUtil.shortToast(
                    this@CameraCloudStorageActivity,
                    getString(R.string.operation_failed) + " errCode=$errCode"
                )
            }
        })
    }

    private fun playCloudDataWithStartTime(startTime: Int, endTime: Int, isEvent: Boolean) {
        cloudCamera?.playCloudDataWithStartTime(startTime.toLong(), endTime.toLong(), isEvent,
            object : OperationCallBack {
                override fun onSuccess(sessionId: Int, requestId: Int, data: String, camera: Any) {
                    //playing
                }

                override fun onFailure(sessionId: Int, requestId: Int, errCode: Int, camera: Any) {}
            }, object : OperationCallBack {
                override fun onSuccess(sessionId: Int, requestId: Int, data: String, camera: Any) {
                    //playCompleted
                }

                override fun onFailure(sessionId: Int, requestId: Int, errCode: Int, camera: Any) {}
            })
    }

    private fun getAppointedDayCloudTimes(dayBean: CloudDayBean?) {
        dayBean?.let {
            getTimeLineInfoByTimeSlice(
                devId,
                it.currentStartDayTime.toString(),
                it.currentDayEndTime.toString()
            )
        }
    }

    /**
     * Get the time slice of the specified time.
     *
     * @param devId  Device id.
     * @param timeGT Start time.
     * @param timeLT End time.
     */
    private fun getTimeLineInfoByTimeSlice(devId: String?, timeGT: String?, timeLT: String?) {
        timeGT ?: return
        timeLT ?: return
        cloudCamera?.getTimeLineInfo(
            devId,
            timeGT.toLong(),
            timeLT.toLong(),
            object : ITuyaResultCallback<List<TimePieceBean>?> {
                override fun onSuccess(result: List<TimePieceBean>?) {
                    if (result == null || result.isEmpty()) {
                        ToastUtil.shortToast(
                            this@CameraCloudStorageActivity,
                            getString(R.string.no_data)
                        )
                    } else {
                        timePieceBeans.clear()
                        timePieceBeans.addAll(result)
                        ToastUtil.shortToast(
                            this@CameraCloudStorageActivity,
                            getString(R.string.operation_suc)
                        )
                    }
                }

                override fun onError(errorCode: String, errorMessage: String) {
                    ToastUtil.shortToast(
                        this@CameraCloudStorageActivity,
                        getString(R.string.err_code) + errorCode
                    )
                }
            })
    }

    /**
     * Obtain the corresponding motion detection data according to the beginning and end of the time segment.
     *
     * @param devId  Device id.
     * @param timeGT Start time.
     * @param timeLT End time.
     * @param offset Which page, default 0
     * @param limit  The number of items pulled each time, the default is -1, which means all data
     */
    fun getMotionDetectionByTimeSlice(
        devId: String?,
        timeGT: String,
        timeLT: String,
        offset: Int,
        limit: Int
    ) {
        cloudCamera?.getMotionDetectionInfo(
            devId,
            timeGT.toLong(),
            timeLT.toLong(),
            offset,
            limit,
            object : ITuyaResultCallback<List<TimeRangeBean?>?> {
                override fun onSuccess(result: List<TimeRangeBean?>?) {}
                override fun onError(errorCode: String, errorMessage: String) {}
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