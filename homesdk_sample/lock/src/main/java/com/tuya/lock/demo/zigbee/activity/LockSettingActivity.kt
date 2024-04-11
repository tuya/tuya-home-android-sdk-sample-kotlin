package com.tuya.lock.demo.zigbee.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.lock.api.IThingZigBeeLock
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.api.IResultCallback
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant

/**
 *
 * Created by HuiYao on 2024/3/1
 */
class LockSettingActivity: AppCompatActivity() {
    private var zigBeeLock: IThingZigBeeLock? = null
    private var remote_unlock_available: TextView? = null
    private var isOpen = true
    private var set_remote_open: RadioButton? = null
    private var set_remote_close: RadioButton? = null

    companion object{
        fun startActivity(context: Context, devId: String?) {
            val intent = Intent(context, LockSettingActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_setting)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        val mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        zigBeeLock = tuyaLockManager.getZigBeeLock(mDevId)
        remote_unlock_available = findViewById(R.id.remote_unlock_available)
        val set_remote_group = findViewById<RadioGroup>(R.id.set_remote_group)
        set_remote_open = findViewById(R.id.set_remote_open)
        set_remote_close = findViewById(R.id.set_remote_close)
        set_remote_group.setOnCheckedChangeListener { group: RadioGroup?, checkedId: Int ->
            isOpen = checkedId == R.id.set_remote_open
        }
        findViewById<View>(R.id.remote_button).setOnClickListener { v: View? -> setVoicePassword() }
    }

    override fun onResume() {
        super.onResume()
        isRemoteUnlockAvailable()
    }

    private fun isRemoteUnlockAvailable() {
        zigBeeLock!!.fetchRemoteUnlockType(object : IThingResultCallback<Boolean> {
            override fun onSuccess(result: Boolean) {
                Log.i(Constant.TAG, "get remote unlock available success:$result")
                remote_unlock_available!!.text = result.toString()
                if (result) {
                    set_remote_open!!.isChecked = true
                } else {
                    set_remote_close!!.isChecked = true
                }
            }

            override fun onError(code: String, message: String) {
                Log.e(
                    Constant.TAG,
                    "get remote unlock available failed: code = $code  message = $message"
                )
                remote_unlock_available!!.text = message
            }
        })
    }

    private fun setVoicePassword() {
        zigBeeLock!!.setRemoteUnlockType(isOpen, object : IResultCallback {
            override fun onError(code: String, error: String) {
                Log.e(
                    Constant.TAG,
                    "setRemoteUnlockType failed: code = $code  message = $error"
                )
                Toast.makeText(applicationContext, error, Toast.LENGTH_SHORT).show()
            }

            override fun onSuccess() {
                Toast.makeText(
                    applicationContext,
                    getString(R.string.zigbee_operation_suc),
                    Toast.LENGTH_SHORT
                ).show()
                remote_unlock_available!!.text = isOpen.toString()
            }
        })
    }

}