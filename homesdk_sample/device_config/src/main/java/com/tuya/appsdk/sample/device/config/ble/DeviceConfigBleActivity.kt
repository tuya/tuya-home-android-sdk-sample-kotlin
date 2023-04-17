package com.tuya.appsdk.sample.device.config.ble

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.tuya.appsdk.sample.device.config.R
import com.tuya.appsdk.sample.resource.HomeModel
import com.thingclips.smart.android.ble.api.BleConfigType
import com.thingclips.smart.android.ble.api.IThingBleConfigListener
import com.thingclips.smart.android.ble.api.ScanType
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.sdk.bean.DeviceBean

/**
 * Device Configuration Ble Low Energy Sample
 *
 * @author aiwen <a href="mailto:developer@tuya.com"/>
 * @since 2/24/21 10:36 AM
 */
class DeviceConfigBleActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DeviceConfigBleActivity"
        const val REQUEST_CODE = 1001
    }

    lateinit var cpiLoading: CircularProgressIndicator
    lateinit var searchButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_config_info_hint_activity)
        initToolbar()
        initView()
        checkPermission()
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
                    REQUEST_CODE
            )
        }
    }

    private fun initView() {
        findViewById<TextView>(R.id.tv_hint_info).text = getString(R.string.device_config_ble_hint)
        cpiLoading = findViewById(R.id.cpiLoading)
        searchButton = findViewById(R.id.bt_search)

        searchButton.setOnClickListener {
            // Check Bluetooth is Opened
            if (!ThingHomeSdk.getBleOperator().isBluetoothOpened) {
                Toast.makeText(this, "Please turn on bluetooth", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            scanSingleBleDevice()
        }
    }


    private fun scanSingleBleDevice() {
        val currentHomeId = HomeModel.INSTANCE.getCurrentHome(this)
        setPbViewVisible(true)

        // Scan Single Ble Device
        ThingHomeSdk.getBleOperator().startLeScan(60 * 1000, ScanType.SINGLE) { bean ->
            Log.i(TAG, "scanSingleBleDevice: deviceUUID=${bean.uuid}")
            // Start configuration -- Single Ble Device
            if (bean?.configType == BleConfigType.CONFIG_TYPE_SINGLE.type) {
                ThingHomeSdk.getBleManager().startBleConfig(currentHomeId, bean.uuid, null,
                        object : IThingBleConfigListener {
                            override fun onSuccess(bean: DeviceBean?) {
                                setPbViewVisible(false)
                                Toast.makeText(
                                        this@DeviceConfigBleActivity,
                                        "Config Success",
                                        Toast.LENGTH_LONG
                                ).show()
                                finish()
                            }

                            override fun onFail(code: String?, msg: String?, handle: Any?) {
                                setPbViewVisible(false)
                                Toast.makeText(
                                        this@DeviceConfigBleActivity,
                                        "Config Failed",
                                        Toast.LENGTH_LONG
                                ).show()
                            }
                        })
            }
        }
    }


    private fun initToolbar() {
        val toolbar: Toolbar = findViewById<View>(R.id.topAppBar) as Toolbar
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.setTitle(R.string.device_config_ble_title)
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


    private fun setPbViewVisible(isShow: Boolean) {
        cpiLoading.visibility = if (isShow) View.VISIBLE else View.GONE
        searchButton.isEnabled = !isShow
    }
}