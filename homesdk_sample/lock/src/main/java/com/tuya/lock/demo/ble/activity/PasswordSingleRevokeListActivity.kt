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
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingBleLockV2
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.lock.api.bean.OfflineTempPassword
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.adapter.PasswordSingleRevokeListAdapter
import com.tuya.lock.demo.common.constant.Constant

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class PasswordSingleRevokeListActivity: AppCompatActivity() {

    companion object{
        fun startActivity(context: Context?, devId: String?) {
            val intent = Intent(context, PasswordSingleRevokeListActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            context?.startActivity(intent)
        }
    }

    private var tuyaLockDevice: IThingBleLockV2? = null
    private val adapter: PasswordSingleRevokeListAdapter by lazy {
        PasswordSingleRevokeListAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_single_revoke_list)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        val mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        tuyaLockDevice = tuyaLockManager.getBleLockV2(mDevId)
        val rvList = findViewById<RecyclerView>(R.id.password_list)
        rvList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        adapter.setDevId(mDevId)
        rvList.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        getOfflineTempPasswordList()
    }

    override fun onDestroy() {
        super.onDestroy()
        tuyaLockDevice!!.onDestroy()
    }

    private fun getOfflineTempPasswordList() {
        tuyaLockDevice!!.getSingleRevokePasswordList(object :
            IThingResultCallback<ArrayList<OfflineTempPassword>> {
            override fun onSuccess(result: ArrayList<OfflineTempPassword>) {
                Log.i(
                    Constant.TAG,
                    "getSingleRevokePasswordList success: $result"
                )
                adapter.setData(result)
                adapter.notifyDataSetChanged()
            }

            override fun onError(errorCode: String, errorMessage: String) {
                Log.e(
                    Constant.TAG,
                    "getSingleRevokePasswordList failed: code = $errorCode  message = $errorMessage"
                )
                Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }
}