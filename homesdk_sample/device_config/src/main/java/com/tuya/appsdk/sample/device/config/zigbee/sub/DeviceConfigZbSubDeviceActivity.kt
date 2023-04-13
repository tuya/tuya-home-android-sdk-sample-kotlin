package com.tuya.appsdk.sample.device.config.zigbee.sub

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.tuya.appsdk.sample.device.config.R
import com.tuya.appsdk.sample.device.config.util.sp.Preference
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.home.sdk.builder.ThingGwSubDevActivatorBuilder
import com.thingclips.smart.sdk.api.IThingSmartActivatorListener
import com.thingclips.smart.sdk.bean.DeviceBean

/**
 * Device Configuration ZigBee sub device Mode Sample
 *
 * @author aiwen <a href="mailto:developer@tuya.com"/>
 * @since 2/24/21 11:13 AM
 */
class DeviceConfigZbSubDeviceActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DeviceConfigZbSubDevice"
        const val CURRENT_GATEWAY_NAME = "current_gateway_name"
        const val CURRENT_GATEWAY_ID = "current_gateway_id"
        const val REQUEST_CODE = 1003
    }

    lateinit var btSearch: Button
    lateinit var tvCurrentGateway: TextView
    lateinit var cpiLoading: CircularProgressIndicator

    var currentGatewayName: String by Preference(this, CURRENT_GATEWAY_NAME, "")
    var currentGatewayId: String by Preference(this, CURRENT_GATEWAY_ID, "")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_config_zb_sub_device_activity)
        initToolbar()
        initView()
    }

    private fun initView() {
        // init gatewayName and gatewayId
        currentGatewayName = ""
        currentGatewayId = ""

        btSearch = findViewById(R.id.btnSearch)
        cpiLoading = findViewById(R.id.cpiLoading)
        tvCurrentGateway = findViewById(R.id.tv_current_gateway_name)

        // choose zigBee gateway
        findViewById<TextView>(R.id.tv_current_zb_gateway).setOnClickListener {
            startActivityForResult(
                    Intent(this, DeviceConfigChooseZbGatewayActivity::class.java),
                    REQUEST_CODE
            )
        }

        btSearch.setOnClickListener {
            subDeviceConfiguration()
        }
    }


    // Sub-device Configuration
    private fun subDeviceConfiguration() {

        if (tvCurrentGateway.text.isEmpty()) {
            Toast.makeText(this, "Please select gateway first", Toast.LENGTH_LONG).show()
            return
        }

        Log.i(TAG, "subDeviceConfiguration: currentGatewayId=${currentGatewayId}")
        setPbViewVisible(true)
        val builder = ThingGwSubDevActivatorBuilder()
                .setDevId(currentGatewayId)
                .setTimeOut(100)
                .setListener(object : IThingSmartActivatorListener {
                    override fun onError(errorCode: String?, errorMsg: String?) {

                        setPbViewVisible(false)
                        Toast.makeText(
                                this@DeviceConfigZbSubDeviceActivity,
                                "Active Error->$errorMsg",
                                Toast.LENGTH_LONG
                        ).show()
                    }

                    override fun onActiveSuccess(devResp: DeviceBean?) {
                        setPbViewVisible(false)
                        Toast.makeText(
                                this@DeviceConfigZbSubDeviceActivity,
                                "Active Success",
                                Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }

                    override fun onStep(step: String?, data: Any?) {
                        Log.i(TAG, "onStep: step->$step")
                    }
                })

        val tuyaGWSubActivator = ThingHomeSdk.getActivatorInstance().newGwSubDevActivator(builder)

        // Start network configuration
        tuyaGWSubActivator.start()
        // Stop network configuration
        // tuyaGWSubActivator.stop();
        // Destroy
        // tuyaGWSubActivator.onDestroy()
    }


    private fun initToolbar() {
        val toolbar: Toolbar = findViewById<View>(R.id.topAppBar) as Toolbar
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.setTitle(R.string.device_config_zb_sub_device_title)
    }


    private fun setPbViewVisible(isShow: Boolean) {
        cpiLoading.visibility = if (isShow) View.VISIBLE else View.GONE
        btSearch.isEnabled = !isShow
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            tvCurrentGateway.text = currentGatewayName
        }
    }
}