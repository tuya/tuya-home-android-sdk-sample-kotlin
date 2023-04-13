package com.tuya.appsdk.sample.device.config.dual

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.tuya.appsdk.sample.device.config.R
import com.tuya.appsdk.sample.device.config.ble.DeviceConfigBleActivity
import com.tuya.appsdk.sample.resource.HomeModel
import com.thingclips.smart.android.ble.api.BleConfigType
import com.thingclips.smart.android.ble.api.IThingBleConfigListener
import com.thingclips.smart.android.ble.api.ScanType
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.sdk.api.IThingActivatorGetToken
import com.thingclips.smart.sdk.bean.DeviceBean

/**
 * Device Configuration Dual Device Sample
 *
 * @author aiwen <a href="mailto:developer@tuya.com"/>
 * @since 2/24/21 11:08 AM
 */
class DeviceConfigDualActivity : AppCompatActivity() {


    companion object {
        private const val TAG = "DeviceConfigDualMode"
        const val REQUEST_CODE = 1002
    }

    private val homeId: Long by lazy {
        HomeModel.INSTANCE.getCurrentHome(this)
    }

    lateinit var etSsid: TextInputEditText
    lateinit var etPassword: TextInputEditText
    lateinit var btSearch: Button
    lateinit var cpiLoading: CircularProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_config_activity)
        initToolbar()

        initView()
        checkPermission()
    }

    private fun initView() {
        etSsid = findViewById(R.id.etSsid)
        etPassword = findViewById(R.id.etPassword)
        btSearch = findViewById(R.id.btnSearch)
        cpiLoading = findViewById(R.id.cpiLoading)

        btSearch.setOnClickListener {


            val strSsid = etSsid.text.toString()
            val strPassword = etPassword.text.toString()

            if (strSsid.isEmpty() || strPassword.isEmpty()) {
                return@setOnClickListener
            }
            if (!ThingHomeSdk.getBleOperator().isBluetoothOpened) {
                Toast.makeText(this, "Please turn on bluetooth", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            scanDualBleDevice()

        }

    }


    private fun initToolbar() {
        val toolbar: Toolbar = findViewById<View>(R.id.topAppBar) as Toolbar
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.setTitle(R.string.device_config_dual_title)
    }


    // You need to check permissions before using Bluetooth devices
    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    ),
                    DeviceConfigBleActivity.REQUEST_CODE
            )
        }
    }


    private fun setPbViewVisible(isShow: Boolean) {
        cpiLoading.visibility = if (isShow) View.VISIBLE else View.GONE
        btSearch.isEnabled = !isShow
    }


    // Scan Ble Device
    private fun scanDualBleDevice() {
        setPbViewVisible(true)
        ThingHomeSdk.getBleOperator().startLeScan(60 * 1000, ScanType.SINGLE) { bean ->
            Log.i(TAG, "scanDualBleDevice: beanType=${bean.configType},uuid=${bean.uuid}")
            // Start configuration -- Dual Device
            if (bean.configType == BleConfigType.CONFIG_TYPE_WIFI.type) {

                //  Get Network Configuration Token
                ThingHomeSdk.getActivatorInstance().getActivatorToken(homeId,
                        object : IThingActivatorGetToken {
                            override fun onSuccess(token: String) {

                                // Start configuration -- Dual Ble Device
                                val param = mutableMapOf<String, String>()
                                param["ssid"] = etSsid.text.toString()
                                param["password"] = etPassword.text.toString()
                                param["token"] = token
                                ThingHomeSdk.getBleManager()
                                        .startBleConfig(homeId, bean.uuid, param as Map<String, String>,
                                                object : IThingBleConfigListener {
                                                    override fun onSuccess(bean: DeviceBean?) {
                                                        setPbViewVisible(false)
                                                        Toast.makeText(
                                                                this@DeviceConfigDualActivity,
                                                                "Config Success",
                                                                Toast.LENGTH_LONG
                                                        ).show()
                                                        finish()
                                                    }

                                                    override fun onFail(
                                                            code: String?,
                                                            msg: String?,
                                                            handle: Any?
                                                    ) {
                                                        setPbViewVisible(false)
                                                        finish()
                                                        Toast.makeText(
                                                                this@DeviceConfigDualActivity,
                                                                "Config Failed",
                                                                Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                })
                            }

                            override fun onFailure(errorCode: String?, errorMsg: String?) {
                                setPbViewVisible(false)
                                Toast.makeText(
                                        this@DeviceConfigDualActivity,
                                        "Error->$errorMsg",
                                        Toast.LENGTH_LONG
                                ).show()
                            }
                        })
            }
        }
    }


    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "onRequestPermissionsResult: agree")
                } else {
                    finish()
                    Log.e(TAG, "onRequestPermissionsResult: denied")
                }
            }
        }
    }
}