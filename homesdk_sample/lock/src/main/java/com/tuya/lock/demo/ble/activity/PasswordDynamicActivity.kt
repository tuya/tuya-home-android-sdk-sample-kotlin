package com.tuya.lock.demo.ble.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingBleLockV2
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.optimus.lock.bean.DynamicPasswordBean
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class PasswordDynamicActivity: AppCompatActivity() {

    companion object{
        fun startActivity(context: Context?, devId: String?) {
            val intent = Intent(context, PasswordDynamicActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            context?.startActivity(intent)
        }
    }

    private var tuyaLockDevice: IThingBleLockV2? = null
    private var dynamic_number: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_dynamic)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        val deviceId = intent.getStringExtra(Constant.DEVICE_ID)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        tuyaLockDevice = tuyaLockManager.getBleLockV2(deviceId)
        dynamic_number = findViewById<TextView>(R.id.dynamic_number)
    }

    override fun onDestroy() {
        super.onDestroy()
        tuyaLockDevice!!.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        getDynamicPasswordData()
    }

    private fun getDynamicPasswordData() {
        tuyaLockDevice!!.getLockDynamicPassword(object : IThingResultCallback<DynamicPasswordBean> {
            override fun onSuccess(result: DynamicPasswordBean) {
                dynamic_number!!.text = result.dynamicPassword
            }

            override fun onError(errorCode: String, errorMessage: String) {
                dynamic_number!!.text = errorMessage
            }
        })
    }
}