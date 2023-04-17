package com.tuya.smart.android.demo.camera

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
import com.alibaba.fastjson.JSONException
import com.alibaba.fastjson.JSONObject
import com.tuya.smart.android.camera.sdk.TuyaIPCSdk
import com.tuya.smart.android.camera.timeline.OnBarMoveListener
import com.tuya.smart.android.camera.timeline.TimeBean
import com.tuya.smart.android.common.utils.L
import com.tuya.smart.android.demo.camera.adapter.CameraPlaybackTimeAdapter
import com.tuya.smart.android.demo.camera.bean.RecordInfoBean
import com.tuya.smart.android.demo.camera.bean.TimePieceBean
import com.tuya.smart.android.demo.camera.databinding.ActivityCameraPlaybackBinding
import com.tuya.smart.android.demo.camera.utils.Constants
import com.tuya.smart.android.demo.camera.utils.MessageUtil
import com.tuya.smart.android.demo.camera.utils.ToastUtil
import com.tuya.smart.camera.camerasdk.typlayer.callback.AbsP2pCameraListener
import com.tuya.smart.camera.camerasdk.typlayer.callback.OperationDelegateCallBack
import com.tuya.smart.camera.ipccamerasdk.bean.MonthDays
import com.tuya.smart.camera.ipccamerasdk.p2p.ICameraP2P
import com.tuya.smart.camera.middleware.p2p.ITuyaSmartCameraP2P
import com.tuya.smart.camera.middleware.widget.AbsVideoViewCallback
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
    private lateinit var mCameraP2P: ITuyaSmartCameraP2P<Any>
    private val ASPECT_RATIO_WIDTH = 9
    private val ASPECT_RATIO_HEIGHT = 16
    private var devId: String? = null
    private var adapter: CameraPlaybackTimeAdapter? = null
    private var queryDateList: MutableList<TimePieceBean>? = null

    private var isPlayback = false

    var mBackDataMonthCache: MutableMap<String, MutableList<String>>? = null
    var mBackDataDayCache: MutableMap<String, MutableList<TimePieceBean>>? = null
    private var mPlaybackMute = ICameraP2P.MUTE
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
            val days = mBackDataMonthCache?.get(mCameraP2P.monthKey)
            try {
                if (days!!.isEmpty()) {
                    showErrorToast()
                    return
                }
                val inputStr: String = viewBinding.dateInputEdt.text.toString()
                if (inputStr.isNotEmpty() && inputStr.contains("/")) {
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
            } catch (e: JSONException) {
                e.printStackTrace()
            }
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
            o: Any
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

    private fun initListener() {
        viewBinding.cameraMute.setOnClickListener(this)
        viewBinding.queryBtn.setOnClickListener(this)
        viewBinding.startBtn.setOnClickListener(this)
        viewBinding.pauseBtn.setOnClickListener(this)
        viewBinding.resumeBtn.setOnClickListener(this)
        viewBinding.stopBtn.setOnClickListener(this)
        adapter?.setListener(object : CameraPlaybackTimeAdapter.OnTimeItemListener {
            override fun onClick(timePieceBean: TimePieceBean) {
                playback(timePieceBean.startTime, timePieceBean.endTime, timePieceBean.startTime)
            }
        })
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

        val cameraInstance = TuyaIPCSdk.getCameraInstance()
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
        val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd")
        val date = Date(System.currentTimeMillis())
        viewBinding.dateInputEdt.setText(simpleDateFormat.format(date))
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
            R.id.start_btn -> startPlayback()
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
            if (substring.size > 2) {
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
}