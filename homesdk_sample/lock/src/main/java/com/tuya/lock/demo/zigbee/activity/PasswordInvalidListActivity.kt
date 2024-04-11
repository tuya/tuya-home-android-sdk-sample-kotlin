package com.tuya.lock.demo.zigbee.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.lock.api.IThingZigBeeLock
import com.thingclips.smart.optimus.lock.api.zigbee.response.PasswordBean
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.DialogUtils
import com.tuya.lock.demo.zigbee.adapter.PasswordListAdapter

/**
 *
 * Created by HuiYao on 2024/3/1
 */
class PasswordInvalidListActivity:AppCompatActivity() {

    private var zigBeeLock: IThingZigBeeLock? = null
    private val adapter: PasswordListAdapter by lazy {
        PasswordListAdapter()
    }
    private var password_list: RecyclerView? = null
    private var error_view: TextView? = null

    companion object{
        fun startActivity(context: Context, devId: String?) {
            val intent = Intent(
                context,
                PasswordInvalidListActivity::class.java
            )
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zigbee_invalid_password_list)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        val mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        zigBeeLock = tuyaLockManager.getZigBeeLock(mDevId)
        error_view = findViewById(R.id.error_view)
        password_list = findViewById(R.id.password_list)
        password_list?.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.VERTICAL,
            false
        )
        adapter.setDevId(mDevId)
        adapter.hideDelete()
        password_list?.adapter = adapter
        findViewById<View>(R.id.password_add).setOnClickListener { v: View? ->
            DialogUtils.showClear(
                this@PasswordInvalidListActivity
            ) { _, _ -> clearData() }
        }
    }

    override fun onResume() {
        super.onResume()
        getOfflineTempPasswordList()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    /**
     * 密码列表
     */
    private fun getOfflineTempPasswordList() {
        zigBeeLock!!.getInvalidPasswordList(0, 100, object : IThingResultCallback<PasswordBean> {
            override fun onSuccess(result: PasswordBean) {
                Log.i(Constant.TAG, "getOnlineTempPasswordList success: $result")
                adapter.setData(result.datas)
                adapter.notifyDataSetChanged()
                if (result.datas.size == 0) {
                    showError("暂无内容")
                } else {
                    password_list!!.visibility = View.VISIBLE
                    error_view!!.visibility = View.GONE
                }
            }

            override fun onError(errorCode: String, errorMessage: String) {
                Log.e(
                    Constant.TAG,
                    "getOnlineTempPasswordList failed: code = $errorCode  message = $errorMessage"
                )
                showError(errorMessage)
            }
        })
    }

    private fun clearData() {
        zigBeeLock?.removeInvalidPassword(object : IThingResultCallback<String?> {
            override fun onSuccess(result: String?) {
                Toast.makeText(
                    this@PasswordInvalidListActivity,
                    "clear onSuccess",
                    Toast.LENGTH_SHORT
                ).show()
                getOfflineTempPasswordList()
            }

            override fun onError(errorCode: String, errorMessage: String) {
                showError(errorMessage)
            }
        })
    }

    private fun showError(msg: String) {
        password_list!!.visibility = View.GONE
        error_view!!.visibility = View.VISIBLE
        error_view!!.text = msg
    }
}