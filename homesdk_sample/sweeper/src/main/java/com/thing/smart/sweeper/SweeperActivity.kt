package com.thing.smart.sweeper

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import kotlinx.android.synthetic.main.activity_sweeper.*

/**
 *
 * create by nielev on 2023/2/24
 */
class SweeperActivity:AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sweeper)

        setSupportActionBar(topAppBar)
        topAppBar.setNavigationOnClickListener { finish() }
        btnCommonControl.setOnClickListener { v -> // Navigate to device management
            try {
                val deviceControl =
                    Class.forName("com.tuya.appsdk.sample.device.mgt.control.activity.DeviceMgtControlActivity")
                val intent = Intent(v.context, deviceControl)
                intent.putExtra("deviceId", getIntent().getStringExtra("deviceId"))
                v.context.startActivity(intent)
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            }
        }
        btnP2pConnect.setOnClickListener { v ->
            val intent = Intent(
                v.context,
                P2pConnectActivity::class.java
            )
            intent.putExtra("deviceId", getIntent().getStringExtra("deviceId"))
            v.context.startActivity(intent)
        }
    }
}