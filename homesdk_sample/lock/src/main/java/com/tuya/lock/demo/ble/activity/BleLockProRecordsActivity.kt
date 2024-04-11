package com.tuya.lock.demo.ble.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingBleLockV2
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.lock.api.bean.ProRecord
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.optimus.lock.bean.ble.RecordRequest
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.adapter.RecordProListAdapter
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.view.FlowRadioGroup

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class BleLockProRecordsActivity: AppCompatActivity() {

    companion object{
        fun startActivity(context: Context?, devId: String?) {
            val intent = Intent(context, BleLockProRecordsActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            context?.startActivity(intent)
        }
    }

    private var tuyaLockDevice: IThingBleLockV2? = null
    private val listAdapter: RecordProListAdapter by lazy {
        RecordProListAdapter()
    }
    private val recordRequest = RecordRequest()
    private val logRecords = ArrayList<RecordRequest.LogRecord>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_pro_records)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        val deviceId = intent.getStringExtra(Constant.DEVICE_ID)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        tuyaLockDevice = tuyaLockManager.getBleLockV2(deviceId)
        val unlock_records_list = findViewById<RecyclerView>(R.id.unlock_records_list)
        unlock_records_list.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        unlock_records_list.adapter = listAdapter
        logRecords.add(RecordRequest.LogRecord.UNLOCK_RECORD)
        logRecords.add(RecordRequest.LogRecord.CLOSE_RECORD)
        logRecords.add(RecordRequest.LogRecord.ALARM_RECORD)
        logRecords.add(RecordRequest.LogRecord.OPERATION)
        val records_type: FlowRadioGroup = findViewById(R.id.records_type)
        records_type.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.all_records -> {
                    logRecords.clear()
                    logRecords.add(RecordRequest.LogRecord.UNLOCK_RECORD)
                    logRecords.add(RecordRequest.LogRecord.CLOSE_RECORD)
                    logRecords.add(RecordRequest.LogRecord.ALARM_RECORD)
                    logRecords.add(RecordRequest.LogRecord.OPERATION)
                }
                R.id.unlock_records -> {
                    logRecords.clear()
                    logRecords.add(RecordRequest.LogRecord.UNLOCK_RECORD)
                }
                R.id.close_records -> {
                    logRecords.clear()
                    logRecords.add(RecordRequest.LogRecord.CLOSE_RECORD)
                }
                R.id.alarm_records -> {
                    logRecords.clear()
                    logRecords.add(RecordRequest.LogRecord.ALARM_RECORD)
                }
                R.id.operation_records -> {
                    logRecords.clear()
                    logRecords.add(RecordRequest.LogRecord.OPERATION)
                }
            }
            getUnlockRecords()
        }
        getUnlockRecords()
    }

    override fun onDestroy() {
        super.onDestroy()
        tuyaLockDevice!!.onDestroy()
    }

    private fun getUnlockRecords() {
        recordRequest.setLogCategories(logRecords)
        recordRequest.limit = 10
        tuyaLockDevice!!.getProUnlockRecordList(
            recordRequest,
            object : IThingResultCallback<ProRecord> {
                override fun onSuccess(result: ProRecord) {
                    Log.i(
                        Constant.TAG,
                        "get ProUnlock RecordList success: recordBean = " + JSONObject.toJSONString(
                            result
                        )
                    )
                    listAdapter.setData(result.records)
                    listAdapter.notifyDataSetChanged()
                }

                override fun onError(errorCode: String, errorMessage: String) {
                    Log.e(
                        Constant.TAG,
                        "get ProUnlock RecordList failed: code = $errorCode  message = $errorMessage"
                    )
                    Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
                }
            })
    }
}