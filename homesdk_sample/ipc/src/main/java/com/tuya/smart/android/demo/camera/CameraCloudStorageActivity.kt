package com.tuya.smart.android.demo.camera

import android.Manifest
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.android.camera.sdk.ThingIPCSdk
import com.thingclips.smart.android.camera.sdk.bean.CloudStatusBean
import com.thingclips.smart.camera.annotation.CloudPlaySpeed
import com.thingclips.smart.camera.camerasdk.bean.ThingVideoFrameInfo
import com.thingclips.smart.camera.camerasdk.thingplayer.callback.AbsP2pCameraListener
import com.thingclips.smart.camera.camerasdk.thingplayer.callback.IRegistorIOTCListener
import com.thingclips.smart.camera.camerasdk.thingplayer.callback.OperationCallBack
import com.thingclips.smart.camera.camerasdk.thingplayer.callback.OperationDelegateCallBack
import com.thingclips.smart.camera.ipccamerasdk.cloud.IThingCloudCamera
import com.thingclips.smart.camera.middleware.cloud.bean.CloudDayBean
import com.thingclips.smart.camera.middleware.cloud.bean.TimePieceBean
import com.thingclips.smart.camera.middleware.cloud.bean.TimeRangeBean
import com.thingclips.smart.camera.middleware.widget.AbsVideoViewCallback
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.tuya.smart.android.demo.camera.adapter.CameraCloudVideoDateAdapter
import com.tuya.smart.android.demo.camera.adapter.CameraVideoTimeAdapter
import com.tuya.smart.android.demo.camera.databinding.ActivityCameraCloudStorageBinding
import com.tuya.smart.android.demo.camera.utils.Constants
import com.tuya.smart.android.demo.camera.utils.IPCSavePathUtils
import com.tuya.smart.android.demo.camera.utils.ToastUtil
import java.io.File
import java.nio.ByteBuffer
import java.util.*

/**
 * CloudStorage Video
 * @author hou qing <a href="mailto:developer@tuya.com"/>
 * @since 2021/7/28 10:24 AM
 */
class CameraCloudStorageActivity : AppCompatActivity() {

    private var cloudCamera: IThingCloudCamera? = null
    private val dayBeanList: MutableList<CloudDayBean> = ArrayList()
    private val timePieceBeans = ArrayList<TimePieceBean>()
    private var soundState = 0
    private var devId: String? = null
    private lateinit var viewBinding: ActivityCameraCloudStorageBinding

    private var timeAdapter: CameraVideoTimeAdapter? = null
    private var dateAdapter: CameraCloudVideoDateAdapter? = null

    private val SERVICE_RUNNING = 10010
    private val SERVICE_EXPIRED = 10011
    private val NO_SERVICE = 10001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraCloudStorageBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        devId = intent.getStringExtra(Constants.INTENT_DEV_ID)
        cloudCamera = ThingIPCSdk.getCloud()?.createCloudCamera()
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
        cloudCamera?.queryCloudServiceStatus(
            devId,
            object : IThingResultCallback<CloudStatusBean> {
                override fun onSuccess(result: CloudStatusBean) {
                    //Get cloud storage status
                    viewBinding.statusTv.text =
                        getString(R.string.cloud_status) + getServiceStatus(result.status)
                    if (result.status == SERVICE_EXPIRED || result.status == SERVICE_RUNNING) {
                        viewBinding.queryBtn.visibility = View.VISIBLE
                        viewBinding.llBottom.visibility = View.VISIBLE
                    }
                }

                override fun onError(errorCode: String, errorMessage: String) {
                    ToastUtil.shortToast(
                        this@CameraCloudStorageActivity,
                        getString(R.string.err_code) + errorCode
                    )
                }
            })
        viewBinding.queryBtn.setOnClickListener {
            //1. Get device cloud storage-related data
            cloudCamera?.getCloudDays(devId,
                TimeZone.getDefault().id,
                object : IThingResultCallback<List<CloudDayBean>?> {
                    override fun onSuccess(result: List<CloudDayBean>?) {
                        if (result == null || result.isEmpty()) {
                            ToastUtil.shortToast(
                                this@CameraCloudStorageActivity,
                                getString(R.string.no_data)
                            )
                        } else {
                            dayBeanList.clear()
                            dayBeanList.addAll(result)
                            dateAdapter?.notifyDataSetChanged()
                            if (dayBeanList.size > 0)
                                viewBinding.dateRv.scrollToPosition(dayBeanList.size - 1)
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
        viewBinding.pauseBtn.setOnClickListener { pausePlayCloudVideo() }

        viewBinding.resumeBtn.setOnClickListener { resumePlayCloudVideo() }

        viewBinding.stopBtn.setOnClickListener { stopPlayCloudVideo() }
        viewBinding.cameraMute.setOnClickListener { setMuteValue(if (soundState == 0) 1 else 0) }
        viewBinding.cameraMute.isSelected = soundState == 0
        viewBinding.snapshotBtn.setOnClickListener { snapshot() }
        viewBinding.recordStart.setOnClickListener { startCloudRecordLocalMP4() }
        viewBinding.recordEnd.setOnClickListener { stopCloudRecordLocalMP4() }

        val mLayoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        viewBinding.timeRv.layoutManager = mLayoutManager
        viewBinding.timeRv.addItemDecoration(DividerItemDecoration(this,
            DividerItemDecoration.VERTICAL))
        timeAdapter = CameraVideoTimeAdapter(this, timePieceBeans)
        viewBinding.timeRv.adapter = timeAdapter
        val ipcSavePathUtils = IPCSavePathUtils(this)
        timeAdapter?.setListener(object : CameraVideoTimeAdapter.OnTimeItemListener {
            override fun onClick(bean: TimePieceBean) {
                playCloudDataWithStartTime(bean.startTime, bean.endTime, bean.isEvent)
            }

            override fun onLongClick(o: TimePieceBean) {
                val openStorage = Constants.requestPermission(this@CameraCloudStorageActivity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Constants.EXTERNAL_STORAGE_REQ_CODE,
                    "open_storage")
                if (openStorage) {
                    ToastUtil.shortToast(this@CameraCloudStorageActivity, "start download")
                    startCloudDataDownload(o.startTime.toLong(),
                        o.endTime.toLong(),
                        ipcSavePathUtils.recordPathSupportQ(
                            devId!!),
                        "download_" + System.currentTimeMillis() + ".mp4")
                }
            }
        })

        val mDateLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        viewBinding.dateRv.layoutManager = mDateLayoutManager
        dateAdapter = CameraCloudVideoDateAdapter(this, dayBeanList)
        viewBinding.dateRv.adapter = dateAdapter
        dateAdapter?.setListener(object : CameraCloudVideoDateAdapter.OnTimeItemListener {
            override fun onClick(dayBean: CloudDayBean?) {
                getTimeLineInfoByTimeSlice(devId,
                    dayBean?.currentStartDayTime.toString(),
                    dayBean?.currentDayEndTime.toString())
            }
        })
    }

    private fun getServiceStatus(code: Int): String? {
        return when (code) {
            SERVICE_EXPIRED -> {
                getString(R.string.ipc_sdk_service_expired)
            }
            SERVICE_RUNNING -> {
                getString(R.string.ipc_sdk_service_running)
            }
            NO_SERVICE -> {
                getString(R.string.ipc_sdk_no_service)
            }
            else -> {
                code.toString()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewBinding.cameraCloudVideoView.onResume()
        cloudCamera?.let {
            if (viewBinding.cameraCloudVideoView.createdView() is IRegistorIOTCListener) {
                it.generateCloudCameraView(viewBinding.cameraCloudVideoView.createdView() as IRegistorIOTCListener)
            }
            it.registerP2PCameraListener(object : AbsP2pCameraListener() {
                override fun receiveFrameDataForMediaCodec(
                    i: Int,
                    bytes: ByteArray,
                    i1: Int,
                    i2: Int,
                    bytes1: ByteArray,
                    b: Boolean,
                    i3: Int,
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
                    o: Any,
                ) {
                }

                override fun onReceiveFrameYUVData(
                    sessionId: Int,
                    y: ByteBuffer?,
                    u: ByteBuffer?,
                    v: ByteBuffer?,
                    videoFrameInfo: ThingVideoFrameInfo?,
                    camera: Any?,
                ) {

                }

                override fun onSessionStatusChanged(o: Any, i: Int, i1: Int) {}
                override fun onReceiveAudioBufferData(
                    i: Int,
                    i1: Int,
                    i2: Int,
                    l: Long,
                    l1: Long,
                    l2: Long,
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
            object : IThingResultCallback<List<TimePieceBean>?> {
                override fun onSuccess(result: List<TimePieceBean>?) {
                    if (result == null || result.isEmpty()) {
                        ToastUtil.shortToast(
                            this@CameraCloudStorageActivity,
                            getString(R.string.no_data)
                        )
                    } else {
                        timePieceBeans.clear()
                        timePieceBeans.addAll(result)
                        timeAdapter?.notifyDataSetChanged()
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
        limit: Int,
    ) {
        cloudCamera?.getMotionDetectionInfo(
            devId,
            timeGT.toLong(),
            timeLT.toLong(),
            offset,
            limit,
            object : IThingResultCallback<List<TimeRangeBean?>?> {
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


    /**
     * 设置倍数播放，在开始播放时进行设置
     */
    private fun setPlayCloudDataSpeed(@CloudPlaySpeed speed: Int) {
        cloudCamera?.setPlayCloudDataSpeed(speed, object : OperationCallBack {
            override fun onSuccess(sessionId: Int, requestId: Int, data: String, camera: Any) {
                // TODO " setPlayCloudDataSpeed  onSuccess"
            }

            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int, camera: Any) {}
        })
    }

    /**
     * 查询 NVR 子设备云盘配置信息（子设备是否开通云存储）
     *
     * @param curNodeId      当前设备的nodeId
     * @param parentDeviceId 当前设备的父设备id
     */
    private fun getCloudDiskPro(curNodeId: String, parentDeviceId: String) {
        cloudCamera?.queryCloudDiskProperty(parentDeviceId,
            object : IThingResultCallback<JSONObject> {
                override fun onSuccess(result: JSONObject) {
                    try { // 解析子列表
                        val jsonArray = result.getJSONArray("propertyList")
                        if (jsonArray != null && jsonArray.size > 0) {
                            for (i in jsonArray.indices) {
                                val jsonObject = jsonArray.getJSONObject(i)
                                val nodeId = jsonObject.getString("nodeId")
                                if (TextUtils.equals(curNodeId, nodeId)) {
                                    val openStatus = jsonObject.getBoolean("openStatus")
                                    if (openStatus) {
                                        // TODO 已开通云存储
                                    }
                                    break
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onError(errorCode: String, errorMessage: String) {}
            })
    }


    /**
     * 云存储下载
     *
     * @param startTime
     * @param stopTime
     * @param folderPath
     * @param mp4FileName
     */
    private fun startCloudDataDownload(
        startTime: Long,
        stopTime: Long,
        folderPath: String,
        mp4FileName: String,
    ) {
        cloudCamera?.startCloudDataDownload(startTime,
            stopTime,
            folderPath,
            mp4FileName,
            object : OperationCallBack {
                override fun onSuccess(
                    sessionId: Int,
                    requestId: Int,
                    data: String,
                    camera: Any,
                ) {
                }

                override fun onFailure(
                    sessionId: Int,
                    requestId: Int,
                    errCode: Int,
                    camera: Any,
                ) {
                }
            },
            { sessionId: Int, requestId: Int, pos: Int, camera: Any? -> },
            object : OperationCallBack {
                override fun onSuccess(
                    sessionId: Int,
                    requestId: Int,
                    data: String,
                    camera: Any,
                ) {
                    runOnUiThread{
                        ToastUtil.shortToast(this@CameraCloudStorageActivity, "download finished")
                    }
                }

                override fun onFailure(
                    sessionId: Int,
                    requestId: Int,
                    errCode: Int,
                    camera: Any,
                ) {
                }
            })
    }


    /**
     * 停止下载视频
     */
    private fun stopCloudDataDownload() {
        cloudCamera?.stopCloudDataDownload(object : OperationCallBack {
            override fun onSuccess(sessionId: Int, requestId: Int, data: String, camera: Any) {}
            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int, camera: Any) {}
        })
    }

    /**
     * 删除云存储视频
     *
     * @param devId
     * @param timeGT
     * @param timeLT
     * @param isAllDay
     * @param timeZone
     */
    private fun deleteCloudVideo(
        devId: String,
        timeGT: Long,
        timeLT: Long,
        isAllDay: Boolean,
        timeZone: String,
    ) {
        cloudCamera?.deleteCloudVideo(devId,
            timeGT,
            timeLT,
            isAllDay,
            timeZone,
            object : IThingResultCallback<String?> {
                override fun onSuccess(result: String?) {}
                override fun onError(errorCode: String, errorMessage: String) {}
            })
    }
}