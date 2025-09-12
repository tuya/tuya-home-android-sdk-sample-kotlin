package com.thing.smart.sweeper

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.thing.smart.sweeper.databinding.ActivitySweeperBinding

/**
 *
 * create by nielev on 2023/2/24
 */
class SweeperActivity: AppCompatActivity() {

    private lateinit var binding: ActivitySweeperBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySweeperBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topAppBar)
        binding.topAppBar.setNavigationOnClickListener { finish() }
        binding.btnCommonControl.setOnClickListener { v ->
            try {
                val deviceControl =
                    Class.forName("com.tuya.appsdk.sample.device.mgt.control.activity.DeviceMgtControlActivity")
                val intent = Intent(this, deviceControl)
                intent.putExtra("deviceId", intent.getStringExtra("deviceId"))
                startActivity(intent)
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            }
        }
        binding.btnP2pConnect.setOnClickListener { v ->
            val intent = Intent(
                this,
                P2pConnectActivity::class.java
            )
            intent.putExtra("deviceId", intent.getStringExtra("deviceId"))
            startActivity(intent)
        }
    }
}