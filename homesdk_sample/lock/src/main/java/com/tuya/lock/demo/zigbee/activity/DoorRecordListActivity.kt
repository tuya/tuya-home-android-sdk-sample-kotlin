package com.tuya.lock.demo.zigbee.activity

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
import com.thingclips.smart.optimus.lock.api.IThingZigBeeLock
import com.thingclips.smart.optimus.lock.api.zigbee.request.RecordRequest
import com.thingclips.smart.optimus.lock.api.zigbee.response.RecordBean
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.optimus.lock.bean.ZigBeeDatePoint
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.zigbee.adapter.RecordListAdapter

/**
 *
 * Created by HuiYao on 2024/3/1
 */
class DoorRecordListActivity : AppCompatActivity() {

    private var zigBeeLock: IThingZigBeeLock? = null
    private var adapter: RecordListAdapter? = null

    private var recyclerView: RecyclerView? = null
    private var error_view: TextView? = null

    companion object{
        fun startActivity(context: Context, devId: String?) {
            val intent = Intent(context, DoorRecordListActivity::class.java)
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
        toolbar.setTitle(R.string.zigbee_door_record_list_title)
        val mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        zigBeeLock = tuyaLockManager.getZigBeeLock(mDevId)
        error_view = findViewById(R.id.error_view)
        recyclerView = findViewById(R.id.record_list_view)
        adapter = RecordListAdapter()
        adapter!!.setDevice(mDevId)
        recyclerView?.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        val dpIds: MutableList<String> = ArrayList()
        dpIds.add(zigBeeLock!!.convertCode2Id(ZigBeeDatePoint.UNLOCK_CARD))
        dpIds.add(zigBeeLock!!.convertCode2Id(ZigBeeDatePoint.UNLOCK_PASSWORD))
        dpIds.add(zigBeeLock!!.convertCode2Id(ZigBeeDatePoint.UNLOCK_DYNAMIC))
        dpIds.add(zigBeeLock!!.convertCode2Id(ZigBeeDatePoint.UNLOCK_FINGERPRINT))
        dpIds.add(zigBeeLock!!.convertCode2Id(ZigBeeDatePoint.UNLOCK_TEMPORARY))
        dpIds.add(zigBeeLock!!.convertCode2Id(ZigBeeDatePoint.UNLOCK_KEY))
        dpIds.add(zigBeeLock!!.convertCode2Id(ZigBeeDatePoint.UNLOCK_REMOTE))
        dpIds.add(zigBeeLock!!.convertCode2Id(ZigBeeDatePoint.OPEN_INSIDE))
        val recordRequest = RecordRequest()
        recordRequest.limit = 30
        recordRequest.offset = 0
        recordRequest.dpIds = dpIds
        zigBeeLock!!.getUnlockRecordList(recordRequest, object : IThingResultCallback<RecordBean> {
            override fun onSuccess(result: RecordBean) {
                adapter!!.setData(result.datas)
                adapter!!.notifyDataSetChanged()
                if (result.datas.size == 0) {
                    showError(getString(R.string.zigbee_no_content))
                } else {
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
        recyclerView!!.visibility = View.GONE
        error_view!!.visibility = View.VISIBLE
        error_view!!.text = msg
    }
}