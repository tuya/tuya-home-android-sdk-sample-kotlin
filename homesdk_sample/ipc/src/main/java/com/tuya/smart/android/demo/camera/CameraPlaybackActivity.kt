package com.tuya.smart.android.demo.camera

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.android.camera.sdk.ThingIPCSdk
import com.thingclips.smart.android.camera.timeline.OnBarMoveListener
import com.thingclips.smart.android.camera.timeline.TimeBean
import com.thingclips.smart.android.common.utils.L
import com.thingclips.smart.camera.camerasdk.thingplayer.callback.AbsP2pCameraListener
import com.thingclips.smart.camera.camerasdk.thingplayer.callback.OperationDelegateCallBack
import com.thingclips.smart.camera.camerasdk.thingplayer.callback.ProgressCallBack
import com.thingclips.smart.camera.ipccamerasdk.bean.MonthDays
import com.thingclips.smart.camera.ipccamerasdk.p2p.ICameraP2P
import com.thingclips.smart.camera.middleware.p2p.IThingSmartCameraP2P
import com.thingclips.smart.camera.middleware.widget.AbsVideoViewCallback
import com.tuya.smart.android.demo.camera.adapter.CameraPlaybackTimeAdapter
import com.tuya.smart.android.demo.camera.adapter.CameraPlaybackVideoDateAdapter
import com.tuya.smart.android.demo.camera.bean.RecordInfoBean
import com.tuya.smart.android.demo.camera.bean.TimePieceBean
import com.tuya.smart.android.demo.camera.databinding.ActivityCameraPlaybackBinding
import com.tuya.smart.android.demo.camera.utils.Constants
import com.tuya.smart.android.demo.camera.utils.IPCSavePathUtils
import com.tuya.smart.android.demo.camera.utils.MessageUtil
import com.tuya.smart.android.demo.camera.utils.ToastUtil
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

/**
 * SdCard Video PlayBack
 * @author hou qing <a href="mailto:developer@tuya.com"/>
 * @since 2021/7/27 8:27 PM
 */
class CameraPlaybackActivity : AppCompatActivity(), View.OnClickListener {
    companion object {
        private const val TAG = "CameraPlaybackActivity"
    }

    private lateinit var viewBinding: ActivityCameraPlaybackBinding
    private lateinit var mCameraP2P: IThingSmartCameraP2P<Any>
    private val ASPECT_RATIO_WIDTH = 9
    private val ASPECT_RATIO_HEIGHT = 16
    private var devId: String? = null
    private var adapter: CameraPlaybackTimeAdapter? = null
    private var dateAdapter: CameraPlaybackVideoDateAdapter? = null
    private var queryDateList: MutableList<TimePieceBean>? = null
    private var dateList: ArrayList<String>? = null

    private var isPlayback = false

    var mBackDataMonthCache: MutableMap<String, MutableList<String>>? = null
    var mBackDataDayCache: MutableMap<String, MutableList<TimePieceBean>>? = null
    private var mPlaybackMute = ICameraP2P.MUTE

    private var isSupportPlaybackDownload = false
    private var isSupportPlaybackDelete = false
    private var isDownloading = false

    @SuppressLint("HandlerLeak")
    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                Constants.MSG_MUTE -> handleMute(msg)
                Constants.MSG_DATA_DATE -> handleDataDate(msg)
                Constants.MSG_DATA_DATE_BY_DAY_SUCC, Constants.MSG_DATA_DATE_BY_DAY_FAIL -> handleDataDay(
                    msg
                )
            }
            super.handleMessage(msg)
        }
    }

    private fun handleDataDay(msg: Message) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
            queryDateList?.clear()
            //Timepieces with data for the query day
            val timePieceBeans = mBackDataDayCache!![mCameraP2P.dayKey]
            if (!timePieceBeans.isNullOrEmpty()) {
                queryDateList?.addAll(timePieceBeans)
                val timelineData: MutableList<TimeBean> = arrayListOf()
                for ((startTime, endTime) in timePieceBeans) {
                    val b = TimeBean()
                    b.startTime = startTime
                    b.endTime = endTime
                    timelineData.add(b)
                }
                viewBinding.timeline.setCurrentTimeConfig(timePieceBeans[0].endTime * 1000L)
                viewBinding.timeline.setRecordDataExistTimeClipsList(timelineData)
            } else {
                showErrorToast()
            }
            adapter?.notifyDataSetChanged()
        }
    }

    private fun handleMute(msg: Message) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
            viewBinding.cameraMute.isSelected = mPlaybackMute == ICameraP2P.MUTE
        } else {
            ToastUtil.shortToast(this@CameraPlaybackActivity, getString(R.string.operation_failed))
        }
    }

    private fun showErrorToast() {
        runOnUiThread {
            ToastUtil.shortToast(this@CameraPlaybackActivity, getString(R.string.no_data))
        }
    }

    private fun handleDataDate(msg: Message) {
        if (msg.arg1 == Constants.ARG1_OPERATE_SUCCESS) {
            dateList?.clear()
            queryDateList?.clear();
            val days = mBackDataMonthCache?.get(mCameraP2P.monthKey)
            if (null == days || days.isEmpty()) {
                showErrorToast()
                return
            }
            viewBinding.rvMonth.scrollToPosition(dateList?.size?.minus(1) ?: 0)

            val inputStr: String = viewBinding.dateInputEdt.text.toString()
            if (inputStr.isNotEmpty() && inputStr.contains("/")) {
                for (s in days) {
                    dateList?.add("$inputStr/$s")
                }
            }
            dateAdapter?.notifyDataSetChanged()
        }
    }

    private fun parsePlaybackData(obj: Any) {
        val parseObject = JSON.parseObject(obj.toString(), RecordInfoBean::class.java)
        if (parseObject.count != 0) {
            if (parseObject.items.isNotEmpty()) {
                mBackDataDayCache?.put(mCameraP2P.dayKey, parseObject.items)
            }
            mHandler.sendMessage(
                MessageUtil.getMessage(
                    Constants.MSG_DATA_DATE_BY_DAY_SUCC,
                    Constants.ARG1_OPERATE_SUCCESS
                )
            )
        } else {
            mHandler.sendMessage(
                MessageUtil.getMessage(
                    Constants.MSG_DATA_DATE_BY_DAY_FAIL,
                    Constants.ARG1_OPERATE_FAIL
                )
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraPlaybackBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        initView()
        initData()
        initListener()
    }

    override fun onPause() {
        super.onPause()
        viewBinding.cameraVideoView.onPause()
        if (isPlayback) {
            mCameraP2P.stopPlayBack(null)
        }
        mCameraP2P.removeOnP2PCameraListener()
        if (isFinishing) {
            mCameraP2P.disconnect(object : OperationDelegateCallBack {
                override fun onSuccess(i: Int, i1: Int, s: String) {}
                override fun onFailure(i: Int, i1: Int, i2: Int) {}
            })
        }
    }

    override fun onResume() {
        super.onResume()
        viewBinding.cameraVideoView.onResume()
        mCameraP2P.registerP2PCameraListener(p2pCameraListener)
        mCameraP2P.generateCameraView(viewBinding.cameraVideoView.createdView())
    }

    private val p2pCameraListener: AbsP2pCameraListener = object : AbsP2pCameraListener() {
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
            super.onReceiveFrameYUVData(
                i,
                byteBuffer,
                byteBuffer1,
                byteBuffer2,
                i1,
                i2,
                i3,
                i4,
                l,
                l1,
                l2,
                o
            )
            viewBinding.timeline.setCurrentTimeInMillisecond(l * 1000L)
        }
    }

    private fun initView() {
        setSupportActionBar(viewBinding.toolbarView)
        viewBinding.toolbarView.setNavigationOnClickListener { onBackPressed() }
        //It is best to set the aspect ratio to 16:9
        val windowManager = this.getSystemService(WINDOW_SERVICE) as WindowManager
        val width = windowManager.defaultDisplay.width
        val height: Int = width * ASPECT_RATIO_WIDTH / ASPECT_RATIO_HEIGHT
        val layoutParams = RelativeLayout.LayoutParams(width, height)
        layoutParams.addRule(RelativeLayout.BELOW, viewBinding.toolbarView.id)
        viewBinding.cameraVideoViewRl.layoutParams = layoutParams
        viewBinding.timeline.setOnBarMoveListener(object : OnBarMoveListener {
            override fun onBarMove(l: Long, l1: Long, l2: Long) {}
            override fun onBarMoveFinish(startTime: Long, endTime: Long, currentTime: Long) {
                viewBinding.timeline.setCanQueryData()
                viewBinding.timeline.setQueryNewVideoData(false)
                if (startTime != -1L && endTime != -1L) {
                    playback(startTime.toInt(), endTime.toInt(), currentTime.toInt())
                }
            }

            override fun onBarActionDown() {}
        })
        viewBinding.timeline.setOnSelectedTimeListener { _, _ -> }
    }

    private fun initData() {
        mBackDataMonthCache = HashMap()
        mBackDataDayCache = HashMap()
        devId = intent.getStringExtra(Constants.INTENT_DEV_ID)
        viewBinding.queryList.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        viewBinding.queryList.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )
        queryDateList = arrayListOf()
        adapter = CameraPlaybackTimeAdapter(queryDateList as ArrayList<TimePieceBean>)
        viewBinding.queryList.adapter = adapter

        val mDateLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        viewBinding.rvMonth.layoutManager = mDateLayoutManager
        dateList = ArrayList()
        dateAdapter = CameraPlaybackVideoDateAdapter(this, dateList)
        viewBinding.rvMonth.adapter = dateAdapter

        val cameraInstance = ThingIPCSdk.getCameraInstance()
        if (cameraInstance != null) {
            mCameraP2P = cameraInstance.createCameraP2P(devId)
        }
        viewBinding.cameraVideoView.setViewCallback(object : AbsVideoViewCallback() {
            override fun onCreated(o: Any) {
                super.onCreated(o)
                mCameraP2P.generateCameraView(viewBinding.cameraVideoView.createdView())
            }
        })
        viewBinding.cameraVideoView.createVideoView(devId)
        if (!mCameraP2P.isConnecting) {
            mCameraP2P.connect(devId, object : OperationDelegateCallBack {
                override fun onSuccess(i: Int, i1: Int, s: String) {
                }

                override fun onFailure(i: Int, i1: Int, i2: Int) {
                    mHandler.post {
                        ToastUtil.shortToast(
                            this@CameraPlaybackActivity,
                            "p2p connect failed "
                        )
                    }
                }
            })
        }
        viewBinding.cameraMute.isSelected = true
        val simpleDateFormat = SimpleDateFormat("yyyy/MM")
        val date = Date(System.currentTimeMillis())
        viewBinding.dateInputEdt.setText(simpleDateFormat.format(date))

        isSupportPlaybackDownload = isSupportPlaybackDownload()
        isSupportPlaybackDelete = isSupportPlaybackDelete()
    }

    private fun initListener() {
        viewBinding.cameraMute.setOnClickListener(this)
        viewBinding.queryBtn.setOnClickListener(this)
        viewBinding.pauseBtn.setOnClickListener(this)
        viewBinding.resumeBtn.setOnClickListener(this)
        viewBinding.stopBtn.setOnClickListener(this)

        val ipcSavePathUtils = IPCSavePathUtils(this)

        adapter?.setListener(object : CameraPlaybackTimeAdapter.OnTimeItemListener {
            override fun onClick(timePieceBean: TimePieceBean) {
                playback(timePieceBean.startTime, timePieceBean.endTime, timePieceBean.startTime)
            }

            override fun onLongClick(timePieceBean: TimePieceBean) {
                val open_storage = Constants.requestPermission(this@CameraPlaybackActivity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Constants.EXTERNAL_STORAGE_REQ_CODE,
                    "open_storage")
                if (isSupportPlaybackDownload && open_storage) {
                    ToastUtil.shortToast(this@CameraPlaybackActivity, "start download")
                    // 写文件权限申请
                    devId?.let { ipcSavePathUtils.recordPathSupportQ(it) }?.let {
                        startPlayBackDownload(timePieceBean.startTime,
                            timePieceBean.endTime,
                            it,
                            "download_" + System.currentTimeMillis() + ".mp4",
                            object : OperationDelegateCallBack {
                                override fun onSuccess(sessionId: Int, requestId: Int, data: String) {
                                    L.i(TAG, " startCloudDataDownload onSuccess")
                                    isDownloading = true
                                }

                                override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                                    L.e(TAG,
                                        " startCloudDataDownload onFailure= $errCode")
                                    isDownloading = false
                                }
                            },
                            { sessionId, requestId, pos, camera ->
                                L.i(TAG,
                                    " startCloudDataDownload onProgress= $pos")
                            },
                            object : OperationDelegateCallBack {
                                override fun onSuccess(sessionId: Int, requestId: Int, data: String) {
                                    L.i(TAG, " startCloudDataDownload Finished onSuccess")
                                    isDownloading = false
                                }

                                override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                                    L.e(TAG,
                                        " startCloudDataDownload Finished onFailure= $errCode")
                                    isDownloading = false
                                }
                            })
                    }
                }
            }
        })

        dateAdapter?.setListener(object : CameraPlaybackVideoDateAdapter.OnTimeItemListener {
            override fun onClick(date: String?) {
                showTimePieceAtDay(date)
            }
        })
    }

    private fun showTimePieceAtDay(inputStr: String?) {
        if (null != inputStr && inputStr.isNotEmpty() && inputStr.contains("/")) {
            val substring = inputStr.split("/".toRegex()).toTypedArray()
            val year = substring[0].toInt()
            val mouth = substring[1].toInt()
            val day = substring[2].toInt()
            mCameraP2P.queryRecordTimeSliceByDay(
                year,
                mouth,
                day,
                object : OperationDelegateCallBack {
                    override fun onSuccess(sessionId: Int, requestId: Int, data: String) {
                        L.e(TAG, "$inputStr --- $data")
                        parsePlaybackData(data)
                    }

                    override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                        mHandler.sendEmptyMessage(Constants.MSG_DATA_DATE_BY_DAY_FAIL)
                    }
                })
        }
    }

    private fun playback(startTime: Int, endTime: Int, playTime: Int) {
        mCameraP2P.startPlayBack(startTime, endTime, playTime, object : OperationDelegateCallBack {
            override fun onSuccess(sessionId: Int, requestId: Int, data: String?) {
                isPlayback = true
            }

            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                isPlayback = false
            }
        }, object : OperationDelegateCallBack {
            override fun onSuccess(sessionId: Int, requestId: Int, data: String?) {
                isPlayback = false
            }

            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                isPlayback = false
            }
        })
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.camera_mute -> muteClick()
            R.id.query_btn -> queryDayByMonthClick()
            R.id.pause_btn -> pauseClick()
            R.id.resume_btn -> resumeClick()
            R.id.stop_btn -> stopClick()
        }
    }

    private fun stopClick() {
        mCameraP2P.stopPlayBack(object : OperationDelegateCallBack {
            override fun onSuccess(sessionId: Int, requestId: Int, data: String) {}
            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {}
        })
        isPlayback = false
    }

    private fun resumeClick() {
        mCameraP2P.resumePlayBack(object : OperationDelegateCallBack {
            override fun onSuccess(sessionId: Int, requestId: Int, data: String) {
                isPlayback = true
            }

            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {}
        })
    }

    private fun pauseClick() {
        mCameraP2P.pausePlayBack(object : OperationDelegateCallBack {
            override fun onSuccess(sessionId: Int, requestId: Int, data: String) {
                isPlayback = false
            }

            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {}
        })
    }

    private fun startPlayback() {
        if (!queryDateList.isNullOrEmpty()) {
            queryDateList!![0].let {
                mCameraP2P.startPlayBack(
                    it.startTime,
                    it.endTime,
                    it.startTime,
                    object : OperationDelegateCallBack {
                        override fun onSuccess(sessionId: Int, requestId: Int, data: String) {
                            isPlayback = true
                        }

                        override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {}
                    },
                    object : OperationDelegateCallBack {
                        override fun onSuccess(sessionId: Int, requestId: Int, data: String) {
                            isPlayback = false
                        }

                        override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {}
                    })
            }
        } else {
            ToastUtil.shortToast(this, getString(R.string.no_data))
        }
    }

    private fun queryDayByMonthClick() {
        if (!mCameraP2P.isConnecting) {
            ToastUtil.shortToast(this@CameraPlaybackActivity, getString(R.string.connect_first))
            return
        }
        val inputStr: String = viewBinding.dateInputEdt.text.toString()
        if (TextUtils.isEmpty(inputStr)) {
            return
        }
        if (inputStr.contains("/")) {
            val substring = inputStr.split("/".toRegex()).toTypedArray()
            if (substring.size >= 2) {
                try {
                    val year = substring[0].toInt()
                    val mouth = substring[1].toInt()
                    mCameraP2P.queryRecordDaysByMonth(
                        year,
                        mouth,
                        object : OperationDelegateCallBack {
                            override fun onSuccess(sessionId: Int, requestId: Int, data: String) {
                                val monthDays = JSONObject.parseObject(data, MonthDays::class.java)
                                mBackDataMonthCache!![mCameraP2P.monthKey] = monthDays.dataDays
                                L.e(TAG, "MonthDays --- $data")
                                mHandler.sendMessage(
                                    MessageUtil.getMessage(
                                        Constants.MSG_DATA_DATE,
                                        Constants.ARG1_OPERATE_SUCCESS
                                    )
                                )
                            }

                            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {
                                mHandler.sendMessage(
                                    MessageUtil.getMessage(
                                        Constants.MSG_DATA_DATE,
                                        Constants.ARG1_OPERATE_FAIL
                                    )
                                )
                            }
                        })
                } catch (e: Exception) {
                    ToastUtil.shortToast(this@CameraPlaybackActivity, getString(R.string.input_err))
                }
            }
        }
    }

    private fun muteClick() {
        val mute: Int = if (mPlaybackMute == ICameraP2P.MUTE) ICameraP2P.UNMUTE else ICameraP2P.MUTE
        mCameraP2P.setMute(mute, object : OperationDelegateCallBack {
            override fun onSuccess(sessionId: Int, requestId: Int, data: String) {
                mPlaybackMute = Integer.valueOf(data)
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


    /**
     * 获取支持的回放倍数（建连之后）
     */
    private fun getSupportPlaySpeedList(): List<Int?>? {
        val cameraConfigInfo = ThingIPCSdk.getCameraInstance().getCameraConfig(devId)
        return cameraConfigInfo?.supportPlaySpeedList
    }

    /**
     * 开始回放成功后进行设置倍数回放
     *
     * @param speed 回放倍数
     */
    private fun setPlayBackSpeed(speed: Int) {
        mCameraP2P.setPlayBackSpeed(speed, object : OperationDelegateCallBack {
            override fun onSuccess(sessionId: Int, requestId: Int, data: String) {}
            override fun onFailure(sessionId: Int, requestId: Int, errCode: Int) {}
        })
    }

    /**
     * 是否支持回放下载
     */
    private fun isSupportPlaybackDownload(): Boolean {
        val cameraInstance = ThingIPCSdk.getCameraInstance()
        if (cameraInstance != null) {
            val cameraConfig = cameraInstance.getCameraConfig(devId)
            if (cameraConfig != null) {
                return cameraConfig.isSupportPlaybackDownload
            }
        }
        return false
    }

    /**
     * 回放视频下载，设备侧SDK 支持完整单个/多个连续片段的下载
     * 需要在开启播放后
     *
     * @param downloadStartTime 传选择开始片段的开始时间
     * @param downloadEndTime   传选择结束片段的结束时间
     * @param folderPath        下载的路径
     * @param fileName          下载保存文件名
     * @param callBack          下载开始回调
     * @param progressCallBack  下载进度回调
     * @param finishCallBack    下载结束回调
     */
    private fun startPlayBackDownload(
        downloadStartTime: Int, downloadEndTime: Int, folderPath: String, fileName: String,
        callBack: OperationDelegateCallBack,
        progressCallBack: ProgressCallBack,
        finishCallBack: OperationDelegateCallBack,
    ) {
        mCameraP2P.startPlayBackDownload(downloadStartTime, downloadEndTime, folderPath, fileName,
            callBack, progressCallBack, finishCallBack)
    }

    /**
     * 暂停回放下载
     */
    private fun pausePlayBackDownload(callBack: OperationDelegateCallBack) {
        mCameraP2P.pausePlayBackDownload(callBack)
    }

    /**
     * 恢复回放下载
     */
    private fun resumePlayBackDownload(callBack: OperationDelegateCallBack) {
        mCameraP2P.resumePlayBackDownload(callBack)
    }

    /**
     * 停止回放下载
     */
    private fun stopPlayBackDownload(callBack: OperationDelegateCallBack) {
        mCameraP2P.stopPlayBackDownload(callBack)
    }

    /**
     * 查询视频是否支持删除
     */
    private fun isSupportPlaybackDelete(): Boolean {
        val cameraInstance = ThingIPCSdk.getCameraInstance()
        if (cameraInstance != null) {
            val cameraConfig = cameraInstance.getCameraConfig(devId)
            if (cameraConfig != null) {
                return cameraConfig.isSupportPlaybackDelete
            }
        }
        return false
    }

    /**
     * 删除指定日期的视频
     *
     * @param day            日期 格式为 yyyyMMdd
     * @param callBack       操作回调
     * @param finishCallBack 结束回调
     */
    private fun deletePlaybackDataByDay(
        day: String, callBack: OperationDelegateCallBack,
        finishCallBack: OperationDelegateCallBack,
    ) {
        mCameraP2P.deletePlaybackDataByDay(day, callBack, finishCallBack)
    }
}