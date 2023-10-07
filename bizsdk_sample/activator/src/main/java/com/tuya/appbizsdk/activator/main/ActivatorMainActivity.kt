package com.tuya.appbizsdk.activator.main

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tuya.appbizsdk.activator.R
import com.tuya.appbizsdk.activator.ap.DeviceConfigAPActivity
import com.tuya.appbizsdk.activator.ble.DeviceConfigBleActivity
import com.tuya.appbizsdk.activator.dual.DeviceConfigDualActivity
import com.tuya.appbizsdk.activator.ez.DeviceConfigEZActivity
import com.tuya.appbizsdk.activator.qrcode.QrCodeConfigActivity
import com.tuya.appbizsdk.activator.scan.DeviceConfigQrCodeDeviceActivity
import com.tuya.appbizsdk.activator.zigbee.gateway.DeviceConfigZbGatewayActivity
import com.tuya.appbizsdk.activator.zigbee.sub.DeviceConfigZbSubDeviceActivity
import com.tuya.appsdk.sample.resource.HomeModel

class ActivatorMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_config_view_func)
        HomeModel.INSTANCE.setCurrentHome(this, intent.getLongExtra("homeId", -0L))
        initView()
    }

    private fun initView() {
// EZ Mode
        findViewById<TextView>(R.id.tvEzMode).setOnClickListener {
            if (!HomeModel.INSTANCE.checkHomeId(this)) {
                Toast.makeText(
                    this,
                    getString(R.string.home_current_home_tips),
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            it.context.startActivity(Intent(it.context, DeviceConfigEZActivity::class.java))
        }

        // AP Mode
        findViewById<TextView>(R.id.tvApMode).setOnClickListener {
            if (!HomeModel.INSTANCE.checkHomeId(this)) {
                Toast.makeText(
                    this,
                    this.getString(R.string.home_current_home_tips),
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            it.context.startActivity(Intent(it.context, DeviceConfigAPActivity::class.java))
        }

        // Ble Low Energy
        findViewById<TextView>(R.id.tv_ble).setOnClickListener {
            if (!HomeModel.INSTANCE.checkHomeId(this)) {
                Toast.makeText(
                    this,
                    this.getString(R.string.home_current_home_tips),
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            it.context.startActivity(Intent(it.context, DeviceConfigBleActivity::class.java))
        }

        // Dual Mode
        findViewById<TextView>(R.id.tv_dual_mode).setOnClickListener {
            if (!HomeModel.INSTANCE.checkHomeId(this)) {
                Toast.makeText(
                    this,
                    this.getString(R.string.home_current_home_tips),
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            it.context.startActivity(Intent(it.context, DeviceConfigDualActivity::class.java))

        }


        // ZigBee Gateway
        findViewById<TextView>(R.id.tv_zigBee_gateway).setOnClickListener {
            if (!HomeModel.INSTANCE.checkHomeId(this)) {
                Toast.makeText(
                    this,
                    this.getString(R.string.home_current_home_tips),
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            it.context.startActivity(Intent(it.context, DeviceConfigZbGatewayActivity::class.java))

        }

        // ZigBee Sub Device
        findViewById<TextView>(R.id.tv_zigBee_subDevice).setOnClickListener {
            if (!HomeModel.INSTANCE.checkHomeId(this)) {
                Toast.makeText(
                    this,
                    this.getString(R.string.home_current_home_tips),
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            it.context.startActivity(
                Intent(
                    it.context,
                    DeviceConfigZbSubDeviceActivity::class.java
                )
            )

        }

        // Scan Qr Code
        findViewById<TextView>(R.id.tv_qrcode_subDevice).setOnClickListener {
            if (!HomeModel.INSTANCE.checkHomeId(this)) {
                Toast.makeText(
                    this,
                    this.getString(R.string.home_current_home_tips),
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            it.context.startActivity(
                Intent(
                    it.context,
                    DeviceConfigQrCodeDeviceActivity::class.java
                )
            )

        }
        //  Qr Code
        findViewById<TextView>(R.id.tv_qr_code).setOnClickListener {
            if (!HomeModel.INSTANCE.checkHomeId(this)) {
                Toast.makeText(
                    this,
                    getString(R.string.home_current_home_tips),
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            it.context.startActivity(
                Intent(
                    it.context,
                    QrCodeConfigActivity::class.java
                )
            )
        }
    }
}