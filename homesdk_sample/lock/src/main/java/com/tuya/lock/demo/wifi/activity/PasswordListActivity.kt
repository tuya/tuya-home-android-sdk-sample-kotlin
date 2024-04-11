package com.tuya.lock.demo.wifi.activity

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
import com.thingclips.smart.android.common.utils.L
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.lock.api.IThingWifiLock
import com.thingclips.smart.optimus.lock.api.bean.TempPassword
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.DialogUtils
import com.tuya.lock.demo.wifi.adapter.PasswordListAdapter

/**
 *
 * Created by HuiYao on 2024/3/1
 */
class PasswordListActivity : AppCompatActivity() {

    private var wifiLock: IThingWifiLock? = null
    private val adapter: PasswordListAdapter by lazy {
        PasswordListAdapter()
    }
    private var password_list: RecyclerView? = null
    private var error_view: TextView? = null

    companion object{
        fun startActivity(context: Context, devId: String?) {
            val intent = Intent(context, PasswordListActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_password_list)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        val mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        wifiLock = tuyaLockManager.getWifiLock(mDevId)
        error_view = findViewById(R.id.error_view)
        password_list = findViewById(R.id.password_list)
        password_list!!.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.VERTICAL,
            false
        )
        adapter.addCallback(object : PasswordListAdapter.Callback {
            override fun remove(bean: TempPassword?, position: Int) {
                DialogUtils.showDelete(
                    this@PasswordListActivity
                ) { _, _ -> deletePasscode(bean!!.id, position) }
            }

        })
        password_list!!.adapter = adapter
        findViewById<View>(R.id.password_add).setOnClickListener { v: View ->
            PasswordDetailActivity.startActivity(
                v.context,
                mDevId
            )
        }
    }

    private fun deletePasscode(passwordId: Int, position: Int) {
        wifiLock!!.deleteTempPassword(passwordId, object : IThingResultCallback<Boolean> {
            override fun onSuccess(result: Boolean) {
                Log.i(Constant.TAG, "deleteOnlineTempPassword onSuccess: $result")
                showToast("delete success")
                adapter.remove(position)
            }

            override fun onError(errorCode: String, errorMessage: String) {
                Log.e(
                    Constant.TAG,
                    "deleteOnlineTempPassword failed: code = $errorCode  message = $errorMessage"
                )
                showToast(errorMessage)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        getOfflineTempPasswordList()
    }

    override fun onDestroy() {
        super.onDestroy()
        wifiLock!!.onDestroy()
    }

    /**
     * 密码列表
     */
    private fun getOfflineTempPasswordList() {
        wifiLock!!.getTempPasswords(object : IThingResultCallback<List<TempPassword>> {
            override fun onSuccess(result: List<TempPassword>) {
                L.i(
                    Constant.TAG,
                    "getOnlineTempPasswordList success: $result"
                )
                if (result.isEmpty()) {
                    showError("No content")
                } else {
                    adapter.setData(result.toMutableList())
                    adapter.notifyDataSetChanged()
                    password_list!!.visibility = View.VISIBLE
                    error_view!!.visibility = View.GONE
                }
            }

            override fun onError(errorCode: String, errorMessage: String) {
                L.e(
                    Constant.TAG,
                    "getTempPasswords failed: code = $errorCode  message = $errorMessage"
                )
                showError(errorMessage)
            }
        })
    }

    private fun showError(msg: String) {
        password_list!!.visibility = View.GONE
        error_view!!.visibility = View.VISIBLE
        error_view!!.text = msg
    }

    private fun showToast(msg: String) {
        runOnUiThread {
            Toast.makeText(
                applicationContext,
                msg,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}