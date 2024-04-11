package com.tuya.lock.demo.wifi.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.lock.api.IThingWifiLock
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant

/**
 *
 * Created by HuiYao on 2024/3/1
 */
class PasswordDynamicActivity: AppCompatActivity() {

    private var wifiLock: IThingWifiLock? = null
    private var dynamic_number: TextView? = null
    private var progress_view: ProgressBar? = null

    companion object{
        fun startActivity(context: Context, devId: String?) {
            val intent = Intent(context, PasswordDynamicActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zigbee_password_dynamic)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        val deviceId = intent.getStringExtra(Constant.DEVICE_ID)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        wifiLock = tuyaLockManager.getWifiLock(deviceId)
        dynamic_number = findViewById(R.id.dynamic_number)
        progress_view = findViewById(R.id.progress_view)
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer.cancel()
        wifiLock!!.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        getDynamicPasswordData()
    }

    private fun getDynamicPasswordData() {
        wifiLock!!.getDynamicPassword(object : IThingResultCallback<String?> {
            override fun onSuccess(result: String?) {
                dynamic_number!!.text = result
                countDownTimer.cancel()
                countDownTimer.start()
            }

            override fun onError(errorCode: String, errorMessage: String) {
                dynamic_number!!.text = errorMessage
                progress_view!!.progress = 0
            }
        })
    }

    private val countDownTimer: CountDownTimer = object : CountDownTimer(300000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            val value = (millisUntilFinished / 1000).toInt()
            progress_view!!.post { progress_view!!.progress = value }
        }

        override fun onFinish() {
            getDynamicPasswordData()
        }
    }
}