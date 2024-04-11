package com.tuya.lock.demo.ble.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingBleLockV2
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.lock.api.bean.Record
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.adapter.RecordListAdapter
import com.tuya.lock.demo.common.constant.Constant

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class BleLockRecordsActivity: AppCompatActivity() {

    companion object{
        fun startActivity(context: Context?, devId: String?) {
            val intent = Intent(context, BleLockRecordsActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            context?.startActivity(intent)
        }
    }

    private var tuyaLockDevice: IThingBleLockV2? = null
    private val listAdapter: RecordListAdapter by lazy {
        RecordListAdapter()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_records)
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
        listAdapter.setDevice(deviceId)
        val records_type = findViewById<RadioGroup>(R.id.records_type)
        records_type.setOnCheckedChangeListener { group: RadioGroup?, checkedId: Int ->
            if (checkedId == R.id.unlock_records) {
                getUnlockRecords()
            } else if (checkedId == R.id.alarm_records) {
                getAlarmRecords()
            } else if (checkedId == R.id.hijack_records) {
                getHijackRecords()
            }
        }
        getUnlockRecords()
    }

    override fun onDestroy() {
        super.onDestroy()
        tuyaLockDevice!!.onDestroy()
    }

    private fun getAlarmRecords() {
        tuyaLockDevice!!.getAlarmRecordList(0, 10, object : IThingResultCallback<Record> {
            override fun onSuccess(result: Record) {
                Log.i(
                    Constant.TAG,
                    "get alarm records success: recordBean = " + JSONObject.toJSONString(result)
                )
                listAdapter.setData(result.datas)
                listAdapter.notifyDataSetChanged()
            }

            override fun onError(errorCode: String, errorMessage: String) {
                Log.e(
                    Constant.TAG,
                    "get alarm records failed: code = $errorCode  message = $errorMessage"
                )
                Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getHijackRecords() {
        tuyaLockDevice!!.getHijackRecords(0, 10, object : IThingResultCallback<Record> {
            override fun onSuccess(result: Record) {
                Log.i(
                    Constant.TAG,
                    "getHijackRecords success: recordBean = " + JSONObject.toJSONString(result)
                )
                listAdapter.setData(result.datas)
                listAdapter.notifyDataSetChanged()
            }

            override fun onError(errorCode: String, errorMessage: String) {
                Log.e(
                    Constant.TAG,
                    "getHijackRecords failed: code = $errorCode  message = $errorMessage"
                )
                Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getUnlockRecords() {
        tuyaLockDevice!!.getUnlockRecordList(0, 10, object : IThingResultCallback<Record> {
            override fun onSuccess(result: Record) {
                Log.i(
                    Constant.TAG,
                    "get alarm records success: recordBean = " + JSONObject.toJSONString(result)
                )
                listAdapter.setData(result.datas)
                listAdapter.notifyDataSetChanged()
            }

            override fun onError(errorCode: String, errorMessage: String) {
                Log.e(
                    Constant.TAG,
                    "get alarm records failed: code = $errorCode  message = $errorMessage"
                )
                Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

}