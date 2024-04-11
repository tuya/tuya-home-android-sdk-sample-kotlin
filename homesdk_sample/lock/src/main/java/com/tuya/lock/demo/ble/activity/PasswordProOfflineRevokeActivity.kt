package com.tuya.lock.demo.ble.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingBleLockV2
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.optimus.lock.bean.ble.OfflinePasswordRevokeRequest
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class PasswordProOfflineRevokeActivity : AppCompatActivity() {

    companion object {
        fun startActivity(
            context: Context?,
            devId: String?,
            unlockBindingId: String?,
            name: String?
        ) {
            val intent = Intent(context, PasswordProOfflineRevokeActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            intent.putExtra("unlockBindingId", unlockBindingId)
            intent.putExtra("name", name)
            context?.startActivity(intent)
        }
    }

    private var tuyaLockDevice: IThingBleLockV2? = null
    private var password_content: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_offline_add_2)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        val mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        tuyaLockDevice = tuyaLockManager.getBleLockV2(mDevId)
        password_content = findViewById<TextView>(R.id.password_content)
    }

    override fun onResume() {
        super.onResume()
        getProSingleRevokeOfflinePassword()
    }

    override fun onDestroy() {
        super.onDestroy()
        tuyaLockDevice!!.onDestroy()
    }

    private fun getProSingleRevokeOfflinePassword() {
        val unlockBindingId = intent.getStringExtra("unlockBindingId")
        val name = intent.getStringExtra("name")
        val revokeRequest = OfflinePasswordRevokeRequest()
        revokeRequest.name = name
        revokeRequest.passwordId = unlockBindingId
        tuyaLockDevice!!.getProSingleRevokeOfflinePassword(
            revokeRequest,
            object : IThingResultCallback<String?> {
                override fun onSuccess(result: String?) {
                    Log.i(
                        Constant.TAG,
                        "setOfflineTempPasswordName success :" + JSONObject.toJSONString(result)
                    )
                    password_content!!.text = result
                    Toast.makeText(applicationContext, "success", Toast.LENGTH_SHORT).show()
                }

                override fun onError(errorCode: String, errorMessage: String) {
                    Log.e(
                        Constant.TAG,
                        "setOfflineTempPasswordName failed: code = $errorCode  message = $errorMessage"
                    )
                    Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
                }
            })
    }
}