package com.tuya.appbizsdk.activator.ble

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
import com.thingclips.smart.activator.core.kit.ThingActivatorCoreKit
import com.thingclips.smart.activator.core.kit.active.inter.IThingActiveManager
import com.thingclips.smart.activator.core.kit.bean.ThingActivatorScanDeviceBean
import com.thingclips.smart.activator.core.kit.bean.ThingActivatorScanFailureBean
import com.thingclips.smart.activator.core.kit.bean.ThingActivatorScanKey
import com.thingclips.smart.activator.core.kit.bean.ThingDeviceActiveErrorBean
import com.thingclips.smart.activator.core.kit.bean.ThingDeviceActiveLimitBean
import com.thingclips.smart.activator.core.kit.builder.ThingDeviceActiveBuilder
import com.thingclips.smart.activator.core.kit.callback.ThingActivatorScanCallback
import com.thingclips.smart.activator.core.kit.listener.IThingDeviceActiveListener
import com.thingclips.smart.android.ble.api.ScanType
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.sdk.bean.DeviceBean
import com.tuya.appbizsdk.activator.R
import com.tuya.appsdk.sample.resource.HomeModel

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

    var activatorScanKey: ThingActivatorScanKey? = null

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
        setPbViewVisible(true)

        // Scan Single Ble Device
        activatorScanKey =
            ThingActivatorCoreKit.getScanDeviceManager().startBlueToothDeviceSearch(
                60 * 1000,
                arrayListOf(ScanType.SINGLE),
                object : ThingActivatorScanCallback {
                    override fun deviceFound(deviceBean: ThingActivatorScanDeviceBean) {
                        Log.i(TAG, "scanSingleBleDevice: uniqueId=${deviceBean.uniqueId}")
                        startActivator(deviceBean)
                    }

                    override fun deviceRepeat(deviceBean: ThingActivatorScanDeviceBean) {
                    }

                    override fun deviceUpdate(deviceBean: ThingActivatorScanDeviceBean) {
                    }

                    override fun scanFailure(failureBean: ThingActivatorScanFailureBean) {

                    }

                    override fun scanFinish() {

                    }

                }
            )
    }


    override fun onDestroy() {
        super.onDestroy()
        ThingActivatorCoreKit.getScanDeviceManager().stopScan(activatorScanKey)
    }

    private fun startActivator(deviceBean: ThingActivatorScanDeviceBean) {
        val activeManager = ThingActivatorCoreKit.getActiveManager().newThingActiveManager()
        activeManager.startActive(ThingDeviceActiveBuilder().apply {
            activeModel = deviceBean.supprotActivatorTypeList[0]
            setActivatorScanDeviceBean(deviceBean)
            timeOut = 60
            relationId = HomeModel.INSTANCE.getCurrentHome(this@DeviceConfigBleActivity)
            listener = object : IThingDeviceActiveListener {
                override fun onActiveError(errorBean: ThingDeviceActiveErrorBean) {
                    setPbViewVisible(false)
                    Toast.makeText(
                        this@DeviceConfigBleActivity,
                        "Config Failed",
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onActiveLimited(limitBean: ThingDeviceActiveLimitBean) {
                }

                override fun onActiveSuccess(deviceBean: DeviceBean) {
                    setPbViewVisible(false)
                    Toast.makeText(
                        this@DeviceConfigBleActivity,
                        "Config Success",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }

                override fun onBind(devId: String) {
                }

                override fun onFind(devId: String) {
                }

            }
        })
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