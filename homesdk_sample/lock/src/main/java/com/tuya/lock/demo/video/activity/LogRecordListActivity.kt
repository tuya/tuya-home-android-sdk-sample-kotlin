package com.tuya.lock.demo.video.activity

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
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.thinglock.videolock.api.IVideoLockManager
import com.thingclips.thinglock.videolock.bean.LogsListBean
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.video.adapter.RecordListAdapter

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class LogRecordListActivity : AppCompatActivity() {

    private var lockManager: IVideoLockManager? = null
    private val adapter: RecordListAdapter by lazy {
        RecordListAdapter()
    }
    private var recyclerView: RecyclerView? = null
    private var error_view: TextView? = null

    companion object {
        fun startActivity(context: Context, devId: String?) {
            val intent = Intent(context, LogRecordListActivity::class.java)
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
        toolbar.title = getString(R.string.lock_log_list)
        val mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        lockManager = tuyaLockManager.newVideoLockManagerInstance(mDevId)
        error_view = findViewById(R.id.error_view)
        recyclerView = findViewById(R.id.record_list_view)
        adapter.setDevice(mDevId)
        recyclerView!!.adapter = adapter
    }

    override fun onDestroy() {
        super.onDestroy()
        lockManager!!.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        lockManager!!.baseAbilityManager.getLogList(
            "",
            "",
            false,
            null,
            System.currentTimeMillis(),
            "",
            20,
            0,
            "",
            object : IThingResultCallback<LogsListBean> {
                override fun onSuccess(result: LogsListBean) {
                    if (result.getRecords().size == 0) {
                        showError(getString(R.string.zigbee_no_content))
                    } else {
                        adapter.setData(result.getRecords())
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