package com.tuya.lock.demo.wifi.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.lock.api.IThingWifiLock
import com.thingclips.smart.optimus.lock.api.bean.Record
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.optimus.lock.utils.LockUtil
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.wifi.adapter.RecordListAdapter

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class AlarmRecordListActivity : AppCompatActivity() {
    private var wifiLock: IThingWifiLock? = null
    private val adapter: RecordListAdapter by lazy {
        RecordListAdapter()
    }
    private var recyclerView: RecyclerView? = null
    private var error_view: TextView? = null

    private var mDevId: String? = null

    companion object{
        fun startActivity(context: Context, devId: String?) {
            val intent = Intent(context, AlarmRecordListActivity::class.java)
            intent.putExtra(Constant.DEVICE_ID, devId)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zigbee_record_list)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        wifiLock = tuyaLockManager.getWifiLock(mDevId)
        error_view = findViewById(R.id.error_view)
        recyclerView = findViewById(R.id.record_list_view)
        adapter.setDevice(mDevId)
        recyclerView!!.adapter = adapter
    }

    override fun onDestroy() {
        super.onDestroy()
        wifiLock!!.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        val dpIds: MutableList<String> = ArrayList()
        dpIds.add(LockUtil.convertCode2Id(mDevId, "hijack"))
        dpIds.add(LockUtil.convertCode2Id(mDevId, "alarm_lock"))
        dpIds.add(LockUtil.convertCode2Id(mDevId, "doorbell"))
        wifiLock!!.getRecords(dpIds, 0, 30, object : IThingResultCallback<Record> {
            override fun onSuccess(result: Record) {
                if (result.datas.size == 0) {
                    showError(getString(R.string.zigbee_no_content))
                } else {
                    adapter.setData(result.datas)
                    adapter.notifyDataSetChanged()
                    recyclerView!!.visibility = View.VISIBLE
                    error_view!!.visibility = View.GONE
                }
            }

            override fun onError(errorCode: String, errorMessage: String) {
                showError(errorMessage)
            }
        })
    }

    private fun showError(msg: String) {
        recyclerView!!.post {
            recyclerView!!.visibility = View.GONE
            error_view!!.visibility = View.VISIBLE
            error_view!!.text = msg
        }
    }
}