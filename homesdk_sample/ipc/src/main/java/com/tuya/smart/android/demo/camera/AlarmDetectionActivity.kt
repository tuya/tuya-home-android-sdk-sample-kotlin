package com.tuya.smart.android.demo.camera

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.thingclips.smart.android.camera.sdk.ThingIPCSdk
import com.thingclips.smart.android.camera.sdk.api.IThingCameraMessage
import com.tuya.smart.android.demo.camera.adapter.AlarmDetectionAdapter
import com.tuya.smart.android.demo.camera.adapter.AlarmDetectionAdapter.OnItemListener
import com.tuya.smart.android.demo.camera.databinding.ActivityCameraMessageBinding
import com.tuya.smart.android.demo.camera.utils.*
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.ipc.messagecenter.bean.CameraMessageBean
import com.thingclips.smart.ipc.messagecenter.bean.CameraMessageClassifyBean
import java.text.SimpleDateFormat
import java.util.*

/**
 * Alarm Detection Messages
 * @author houqing <a href="mailto:developer@tuya.com"/>
 * @since 2021/7/27 4:11 PM
 */
class AlarmDetectionActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var devId: String
    private lateinit var mWaitingDeleteCameraMessageList: MutableList<CameraMessageBean>
    private lateinit var mCameraMessageList: MutableList<CameraMessageBean>
    private var selectClassify: CameraMessageClassifyBean? = null
    private var adapter: AlarmDetectionAdapter? = null
    private var day = 0
    private var year: Int = 0
    private var month: Int = 0
    private var offset = 0
    private var mTyCameraMessage: IThingCameraMessage? = null
    private lateinit var viewBinding: ActivityCameraMessageBinding
    private val mHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                Constants.ALARM_DETECTION_DATE_MONTH_FAILED -> handlAlarmDetectionDateFail(msg)
                Constants.ALARM_DETECTION_DATE_MONTH_SUCCESS -> handlAlarmDetectionDateSuccess(msg)
                Constants.MSG_GET_ALARM_DETECTION -> handleAlarmDetection()
                Constants.MSG_DELETE_ALARM_DETECTION -> handleDeleteAlarmDetection()
            }
            super.handleMessage(msg)
        }
    }

    private fun handleDeleteAlarmDetection() {
        mCameraMessageList.removeAll(mWaitingDeleteCameraMessageList)
        adapter?.updateAlarmDetectionMessage(mCameraMessageList)
        adapter?.notifyDataSetChanged()
    }

    private fun handleAlarmDetection() {
        adapter?.updateAlarmDetectionMessage(mCameraMessageList)
        adapter?.notifyDataSetChanged()
    }

    private fun handlAlarmDetectionDateFail(msg: Message) {}

    private fun handlAlarmDetectionDateSuccess(msg: Message) {
        if (null != mTyCameraMessage && selectClassify != null) {
            val time = DateUtils.getCurrentTime(year, month, day)
            val startTime = DateUtils.getTodayStart(time).toInt()
            val endTime = (DateUtils.getTodayEnd(time) - 1).toInt()
            mTyCameraMessage?.getAlarmDetectionMessageList(
                devId,
                startTime,
                endTime,
                selectClassify!!.msgCode,
                offset,
                30,
                object : IThingResultCallback<List<CameraMessageBean>?> {
                    override fun onSuccess(result: List<CameraMessageBean>?) {
                        if (result != null) {
                            offset += result.size
                            mCameraMessageList = result.toMutableList()
                            mHandler.sendMessage(
                                MessageUtil.getMessage(
                                    Constants.MSG_GET_ALARM_DETECTION,
                                    Constants.ARG1_OPERATE_SUCCESS
                                )
                            )
                        } else {
                            mHandler.sendMessage(
                                MessageUtil.getMessage(
                                    Constants.MSG_GET_ALARM_DETECTION,
                                    Constants.ARG1_OPERATE_FAIL
                                )
                            )
                        }
                    }

                    override fun onError(errorCode: String, errorMessage: String) {
                        mHandler.sendMessage(
                            MessageUtil.getMessage(
                                Constants.MSG_GET_ALARM_DETECTION,
                                Constants.ARG1_OPERATE_FAIL
                            )
                        )
                    }
                })
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraMessageBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        viewBinding.toolbarView.setNavigationOnClickListener { onBackPressed() }
        devId = intent.getStringExtra(Constants.INTENT_DEV_ID).toString()
        initView()
        initData()
        initListener()
    }

    private fun initListener() {
        viewBinding.queryBtn.setOnClickListener(this)
    }

    private fun initView() {
        val simpleDateFormat = SimpleDateFormat("yyyy/MM/dd")
        val date = Date(System.currentTimeMillis())
        viewBinding.dateInputEdt.hint = simpleDateFormat.format(date)
        viewBinding.dateInputEdt.setText(simpleDateFormat.format(date))
    }

    private fun initData() {
        mWaitingDeleteCameraMessageList = arrayListOf()
        mCameraMessageList = arrayListOf()
        mTyCameraMessage = ThingIPCSdk.getMessage()?.createCameraMessage()
        queryCameraMessageClassify(devId)
        viewBinding.queryList.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        viewBinding.queryList.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )
        adapter = AlarmDetectionAdapter(this, mCameraMessageList)
        adapter!!.setListener(object : OnItemListener {
            override fun onLongClick(o: CameraMessageBean) {
                deleteCameraMessageClassify(o)
            }

            override fun onItemClick(o: CameraMessageBean) {
                //if type is video, jump to CameraCloudVideoActivity
                if (o.attachVideos?.size!! > 0) {
                    // TODO: 2021/7/27
                    val intent =
                        Intent(this@AlarmDetectionActivity, CameraCloudVideoActivity::class.java)
                    val attachVideo = o.attachVideos[0]
                    val playUrl = attachVideo.substring(0, attachVideo.lastIndexOf('@'))
                    val encryptKey = attachVideo.substring(attachVideo.lastIndexOf('@') + 1)
                    intent.putExtra("playUrl", playUrl)
                    intent.putExtra("encryptKey", encryptKey)
                    intent.putExtra("devId", devId)
                    startActivity(intent)
                }
            }
        })
        viewBinding.queryList.adapter = adapter
    }

    private fun queryCameraMessageClassify(devId: String?) {
        mTyCameraMessage?.queryAlarmDetectionClassify(
            devId,
            object : IThingResultCallback<List<CameraMessageClassifyBean>> {
                override fun onSuccess(result: List<CameraMessageClassifyBean>) {
                    selectClassify = result[0]
                    mHandler.sendEmptyMessage(Constants.MOTION_CLASSIFY_SUCCESS)
                }

                override fun onError(errorCode: String, errorMessage: String) {
                    mHandler.sendEmptyMessage(Constants.MOTION_CLASSIFY_FAILED)
                }
            })
    }


    fun deleteCameraMessageClassify(cameraMessageBean: CameraMessageBean) {
        mWaitingDeleteCameraMessageList.add(cameraMessageBean)
        val ids: MutableList<String> = ArrayList()
        ids.add(cameraMessageBean.id)
        mTyCameraMessage?.deleteMotionMessageList(ids, object : IThingResultCallback<Boolean?> {
            override fun onSuccess(result: Boolean?) {
                mCameraMessageList.removeAll(mWaitingDeleteCameraMessageList)
                mWaitingDeleteCameraMessageList.clear()
                mHandler.sendMessage(
                    MessageUtil.getMessage(
                        Constants.MSG_DELETE_ALARM_DETECTION,
                        Constants.ARG1_OPERATE_SUCCESS
                    )
                )
            }

            override fun onError(errorCode: String, errorMessage: String) {
                mHandler.sendMessage(
                    MessageUtil.getMessage(
                        Constants.MSG_DELETE_ALARM_DETECTION,
                        Constants.ARG1_OPERATE_FAIL
                    )
                )
            }
        })
    }


    override fun onClick(v: View) {
        when (v.id) {
            R.id.query_btn -> queryAlarmDetectionByMonth()
        }
    }

    private fun queryAlarmDetectionByMonth() {
        val inputStr: String = viewBinding.dateInputEdt.text.toString()
        if (TextUtils.isEmpty(inputStr)) {
            ToastUtil.shortToast(this, getString(R.string.not_input_query_data))
            return
        }
        val substring = inputStr.split("/".toRegex()).toTypedArray()
        year = substring[0].toInt()
        month = substring[1].toInt()
        mTyCameraMessage?.queryMotionDaysByMonth(
            devId,
            year,
            month,
            object : IThingResultCallback<List<String>> {
                override fun onSuccess(result: List<String>) {
                    if (result.isNotEmpty()) {
                        Collections.sort(result)
                        day = result[result.size - 1].toInt()
                    }
                    mHandler.sendEmptyMessage(Constants.ALARM_DETECTION_DATE_MONTH_SUCCESS)
                }

                override fun onError(errorCode: String, errorMessage: String) {
                    mHandler.sendEmptyMessage(Constants.ALARM_DETECTION_DATE_MONTH_FAILED)
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
        mTyCameraMessage?.destroy()
    }

}