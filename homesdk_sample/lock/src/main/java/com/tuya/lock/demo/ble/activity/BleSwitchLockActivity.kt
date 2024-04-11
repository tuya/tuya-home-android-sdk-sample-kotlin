package com.tuya.lock.demo.ble.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingBleLockV2
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.api.IResultCallback
import com.thingclips.smart.sdk.optimus.lock.bean.ble.BLELockUser
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class BleSwitchLockActivity : AppCompatActivity() {
    companion object {
        fun startActivity(context: Context?, devId: String?) {
            val intent = Intent(context, BleSwitchLockActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            context?.startActivity(intent)
        }
    }

    private var tuyaLockDevice: IThingBleLockV2? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_and_unlock)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        val deviceId = intent.getStringExtra(Constant.DEVICE_ID)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        tuyaLockDevice = tuyaLockManager.getBleLockV2(deviceId)

        /* *
         * 近场开锁
         */
        findViewById<View>(R.id.ble_new_version_unlock).setOnClickListener { v: View? -> bleUnlock() }

        /* *
         * 近场关锁
         */
        findViewById<View>(R.id.ble_new_version_lock).setOnClickListener { v: View? -> bleManualLock() }

        /* *
         * 远程开锁
         */
        findViewById<View>(R.id.ble_far_unlock).setOnClickListener { v: View? -> farUnlock() }

        /* *
         * 远程关锁
         */
        findViewById<View>(R.id.ble_far_lock).setOnClickListener { v: View? -> farLock() }
    }

    override fun onDestroy() {
        super.onDestroy()
        tuyaLockDevice!!.onDestroy()
    }

    private fun bleUnlock() {
        tuyaLockDevice!!.getCurrentMemberDetail(object : IThingResultCallback<BLELockUser> {
            override fun onSuccess(result: BLELockUser) {
                Log.i(Constant.TAG, "getCurrentUser:" + JSONObject.toJSONString(result))
                tuyaLockDevice!!.bleUnlock(result.lockUserId, object : IResultCallback {
                    override fun onError(code: String, error: String) {
                        Log.i(
                            Constant.TAG,
                            "bleUnlock onError code:$code, error:$error"
                        )
                        Toast.makeText(applicationContext, error, Toast.LENGTH_SHORT).show()
                    }

                    override fun onSuccess() {
                        Toast.makeText(applicationContext, "unlock success", Toast.LENGTH_SHORT)
                            .show()
                    }
                })
            }

            override fun onError(code: String, error: String) {
                Log.e(
                    Constant.TAG,
                    "getCurrentUser onError code:$code, error:$error"
                )
                Toast.makeText(applicationContext, error, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun bleManualLock() {
        tuyaLockDevice!!.bleManualLock(object : IResultCallback {
            override fun onError(code: String, error: String) {
                Log.i(
                    Constant.TAG,
                    "bleManualLock onError code:$code, error:$error"
                )
                Toast.makeText(applicationContext, error, Toast.LENGTH_SHORT).show()
            }

            override fun onSuccess() {
                Toast.makeText(applicationContext, "lock success", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun farUnlock() {
        Log.i(Constant.TAG, "remoteSwitchLock")
        tuyaLockDevice!!.remoteSwitchLock(true, object : IResultCallback {
            override fun onError(code: String, error: String) {
                Log.e(
                    Constant.TAG,
                    "remoteSwitchLock unlock onError code:$code, error:$error"
                )
                Toast.makeText(applicationContext, error, Toast.LENGTH_SHORT).show()
            }

            override fun onSuccess() {
                Toast.makeText(applicationContext, "remote unlock success", Toast.LENGTH_SHORT)
                    .show()
            }
        })
    }

    private fun farLock() {
        tuyaLockDevice!!.remoteSwitchLock(false, object : IResultCallback {
            override fun onError(code: String, error: String) {
                Log.e(
                    Constant.TAG,
                    "remoteSwitchLock lock onError code:$code, error:$error"
                )
                Toast.makeText(applicationContext, error, Toast.LENGTH_SHORT).show()
            }

            override fun onSuccess() {
                Toast.makeText(applicationContext, "remote lock success", Toast.LENGTH_SHORT).show()
            }
        })
    }

}