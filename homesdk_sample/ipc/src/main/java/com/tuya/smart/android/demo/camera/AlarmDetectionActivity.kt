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
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.tuya.smart.android.demo.camera.adapter.AlarmDetectionAdapter
import com.tuya.smart.android.demo.camera.adapter.AlarmDetectionAdapter.OnItemListener
import com.tuya.smart.android.demo.camera.databinding.ActivityCameraMessageBinding
import com.tuya.smart.android.demo.camera.utils.*
import com.tuya.smart.android.network.Business.ResultListener
import com.tuya.smart.android.network.http.BusinessResponse
import com.tuya.smart.ipc.messagecenter.bean.CameraMessageBean
import com.tuya.smart.ipc.messagecenter.bean.CameraMessageClassifyBean
import com.tuya.smart.ipc.messagecenter.business.CameraMessageBusiness
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

/**

 * TODO feature

 *侦测消息，消息列表
 * Alarm Detection Messages
 * @author houqing <a href="mailto:developer@tuya.com"/>

 * @since 2021/7/27 4:11 下午

 */
class AlarmDetectionActivity :AppCompatActivity(),View.OnClickListener{
    private lateinit var devId: String
    private lateinit var mWaitingDeleteCameraMessageList: MutableList<CameraMessageBean>
    private lateinit var mCameraMessageList: MutableList<CameraMessageBean>
    private lateinit var messageBusiness: CameraMessageBusiness
    private var selectClassify: CameraMessageClassifyBean? = null
    private var adapter: AlarmDetectionAdapter? = null
    private var day = 0
    private var year:Int = 0
    private var month:Int = 0
    private var offset = 0
    private lateinit var viewBinding:ActivityCameraMessageBinding
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
        adapter!!.updateAlarmDetectionMessage(mCameraMessageList)
        adapter!!.notifyDataSetChanged()
    }

    private fun handleAlarmDetection() {
        adapter!!.updateAlarmDetectionMessage(mCameraMessageList)
        adapter!!.notifyDataSetChanged()
    }

    private fun handlAlarmDetectionDateFail(msg: Message) {}

    private fun handlAlarmDetectionDateSuccess(msg: Message) {
        messageBusiness?.let {
            val time = DateUtils.getCurrentTime(year, month, day)
            val startTime = DateUtils.getTodayStart(time)
            val endTime = DateUtils.getTodayEnd(time) - 1L
            val jsonObject = JSONObject()
            jsonObject["msgSrcId"] = devId
            jsonObject["startTime"] = startTime
            jsonObject["endTime"] = endTime
            jsonObject["msgType"] = 4
            jsonObject["limit"] = 30
            jsonObject["keepOrig"] = true
            jsonObject["offset"] = offset
            if (null != selectClassify) {
                jsonObject["msgCodes"] = selectClassify!!.msgCode
            }
            it.getAlarmDetectionMessageList(jsonObject.toJSONString(), object : ResultListener<JSONObject> {
                    override fun onFailure(businessResponse: BusinessResponse, jsonObject: JSONObject, s: String?) {
                        mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_GET_ALARM_DETECTION, Constants.ARG1_OPERATE_FAIL))
                    }

                    override fun onSuccess(businessResponse: BusinessResponse, jsonObject: JSONObject, s: String?) {
                        val msgList: MutableList<CameraMessageBean>? = try {
                            JSONArray.parseArray(jsonObject.getString("datas"), CameraMessageBean::class.java)
                        } catch (e: Exception) {
                            null
                        }
                        if (msgList != null) {
                            offset += msgList.size
                            mCameraMessageList = msgList
                            mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_GET_ALARM_DETECTION, Constants.ARG1_OPERATE_SUCCESS))
                        } else {
                            mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_GET_ALARM_DETECTION, Constants.ARG1_OPERATE_FAIL))
                        }
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
        messageBusiness = CameraMessageBusiness()
        queryCameraMessageClassify(devId)
        viewBinding.queryList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        viewBinding.queryList.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        adapter = AlarmDetectionAdapter(this, mCameraMessageList)
        adapter!!.setListener(object : OnItemListener {
            override fun onLongClick(o: CameraMessageBean) {
                deleteCameraMessageClassify(o)
            }

            override fun onItemClick(o: CameraMessageBean) {
                //if type is video, jump to CameraCloudVideoActivity
                if (o.attachVideos?.size!! >0) {
                    // TODO: 2021/7/27
                    val intent = Intent(this@AlarmDetectionActivity, CameraCloudVideoActivity::class.java)
                    val attachVideo = o.attachVideos[0]
                    val playUrl = attachVideo.substring(0, attachVideo.lastIndexOf('@'))
                    val encryptKey = attachVideo.substring(attachVideo.lastIndexOf('@') + 1)
                    intent.putExtra("playUrl", playUrl)
                    intent.putExtra("encryptKey", encryptKey)
                    intent.putExtra("devId",devId)
                    startActivity(intent)
                }
            }
        })
        viewBinding.queryList.adapter = adapter
    }

    private fun queryCameraMessageClassify(devId: String?) {
            messageBusiness.queryAlarmDetectionClassify(devId, object : ResultListener<ArrayList<CameraMessageClassifyBean>> {
                    override fun onFailure(businessResponse: BusinessResponse, cameraMessageClassifyBeans: ArrayList<CameraMessageClassifyBean>, s: String?) {
                        mHandler.sendEmptyMessage(Constants.MOTION_CLASSIFY_FAILED)
                    }
                    override fun onSuccess(businessResponse: BusinessResponse, cameraMessageClassifyBeans: ArrayList<CameraMessageClassifyBean>, s: String?) {
                        selectClassify = cameraMessageClassifyBeans[0]
                        mHandler.sendEmptyMessage(Constants.MOTION_CLASSIFY_SUCCESS)
                    }
                })
    }


    fun deleteCameraMessageClassify(cameraMessageBean: CameraMessageBean) {
        mWaitingDeleteCameraMessageList.add(cameraMessageBean)
        //        StringBuilder ids = new StringBuilder();
        messageBusiness.deleteAlarmDetectionMessageList(cameraMessageBean.id, object : ResultListener<Boolean?> {
                override fun onFailure(businessResponse: BusinessResponse, aBoolean: Boolean?, s: String?) {
                    mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_DELETE_ALARM_DETECTION, Constants.ARG1_OPERATE_FAIL))
                }

                override fun onSuccess(businessResponse: BusinessResponse, aBoolean: Boolean?, s: String?) {
                    mCameraMessageList.removeAll(mWaitingDeleteCameraMessageList)
                    mWaitingDeleteCameraMessageList.clear()
                    mHandler.sendMessage(MessageUtil.getMessage(Constants.MSG_DELETE_ALARM_DETECTION, Constants.ARG1_OPERATE_SUCCESS))
                }
            })
    }


    override fun onClick(v: View) {
        when(v.id){
            R.id.query_btn->queryAlarmDetectionByMonth()
        }
    }
    private fun queryAlarmDetectionByMonth() {
        val inputStr = viewBinding.dateInputEdt.text.toString()
        if (TextUtils.isEmpty(inputStr)) {
            ToastUtil.shortToast(this, getString(R.string.not_input_query_data))
            return
        }
        val substring = inputStr.split("/".toRegex()).toTypedArray()
        year = substring[0].toInt()
        month = substring[1].toInt()
        val jsonObject = JSONObject()
        jsonObject["msgSrcId"] = devId
        jsonObject["timeZone"] = TimeZoneUtils.getTimezoneGCMById(TimeZone.getDefault().id)
        jsonObject["month"] = "$year-$month"
        messageBusiness.queryAlarmDetectionDaysByMonth(jsonObject.toJSONString(), object : ResultListener<JSONArray> {
                override fun onFailure(businessResponse: BusinessResponse, objects: JSONArray, s: String?) {
                    mHandler.sendEmptyMessage(Constants.ALARM_DETECTION_DATE_MONTH_FAILED)
                }

                override fun onSuccess(businessResponse: BusinessResponse, objects: JSONArray, s: String?) {
                    val dates = JSONArray.parseArray(objects.toJSONString(), Int::class.java)
                    if (dates.size > 0) {
                        dates.sort()
                        day = dates[dates.size - 1]
                    }
                    mHandler.sendEmptyMessage(Constants.ALARM_DETECTION_DATE_MONTH_SUCCESS)
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
        messageBusiness.onDestroy()
    }

}