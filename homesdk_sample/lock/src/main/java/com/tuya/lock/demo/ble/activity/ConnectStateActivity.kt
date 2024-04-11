package com.tuya.lock.demo.ble.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.thingclips.smart.optimus.lock.api.IThingBleLockV2
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class ConnectStateActivity : AppCompatActivity() {

    private var tuyaLockDevice: IThingBleLockV2? = null

    companion object {
        fun startActivity(context: Context?, devId: String?) {
            val intent = Intent(context, ConnectStateActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            context?.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_connect_state)

        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }

        val mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        tuyaLockDevice = tuyaLockManager.getBleLockV2(mDevId)

        var isBLEConnected = false
        if (null != tuyaLockDevice) {
            isBLEConnected = tuyaLockDevice!!.isBLEConnected
        }

        val isBLEConnectedStr = "isBLEConnected: $isBLEConnected"
        findViewById<TextView>(R.id.local_ble_connect).text = isBLEConnectedStr

        var isOnline = false
        if (null != tuyaLockDevice) {
            isOnline = tuyaLockDevice!!.isOnline
        }
        val isOnlineStr = "isOnline: $isOnline"
        findViewById<TextView>(R.id.ble_online).text = isOnlineStr
    }
}