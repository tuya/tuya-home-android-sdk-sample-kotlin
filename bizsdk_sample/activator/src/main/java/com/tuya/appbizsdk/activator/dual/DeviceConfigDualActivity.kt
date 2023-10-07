package com.tuya.appbizsdk.activator.dual

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
import com.thingclips.smart.activator.core.kit.ThingActivatorCoreKit
import com.thingclips.smart.activator.core.kit.active.inter.IThingActiveManager
import com.thingclips.smart.activator.core.kit.bean.ThingActivatorScanDeviceBean
import com.thingclips.smart.activator.core.kit.bean.ThingActivatorScanFailureBean
import com.thingclips.smart.activator.core.kit.bean.ThingActivatorScanKey
import com.thingclips.smart.activator.core.kit.bean.ThingDeviceActiveErrorBean
import com.thingclips.smart.activator.core.kit.bean.ThingDeviceActiveLimitBean
import com.thingclips.smart.activator.core.kit.builder.ThingDeviceActiveBuilder
import com.thingclips.smart.activator.core.kit.callback.ThingActivatorScanCallback
import com.thingclips.smart.activator.core.kit.constant.ThingDeviceActiveModeEnum
import com.thingclips.smart.activator.core.kit.listener.IThingDeviceActiveListener
import com.thingclips.smart.android.ble.api.ScanType
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.sdk.bean.DeviceBean
import com.tuya.appbizsdk.activator.R
import com.tuya.appbizsdk.activator.ble.DeviceConfigBleActivity
import com.tuya.appsdk.sample.resource.HomeModel

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

    private var activeManager: IThingActiveManager? = null
    private var scanKey: ThingActivatorScanKey? = null

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
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ), DeviceConfigBleActivity.REQUEST_CODE
            )
        }
    }


    private fun setPbViewVisible(isShow: Boolean) {
        cpiLoading.visibility = if (isShow) View.VISIBLE else View.GONE
        btSearch.isEnabled = !isShow
    }

    override fun onDestroy() {
        super.onDestroy()
        ThingActivatorCoreKit.getScanDeviceManager().stopScan(scanKey)
        activeManager?.stopActive()
    }


    // Scan Ble Device
    private fun scanDualBleDevice() {
        setPbViewVisible(true)
        scanKey = ThingActivatorCoreKit.getScanDeviceManager().startBlueToothDeviceSearch(60 * 1000,
            arrayListOf(ScanType.SINGLE),
            object : ThingActivatorScanCallback {
                override fun deviceFound(deviceBean: ThingActivatorScanDeviceBean) {
                    Log.i(TAG, "scanDualBleDevice: uniqueId=${deviceBean.uniqueId}")
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

            })
    }

    private fun startActivator(deviceBean: ThingActivatorScanDeviceBean) {
        val thingDeviceActiveModeEnum = deviceBean.supprotActivatorTypeList[0]
        if (thingDeviceActiveModeEnum == ThingDeviceActiveModeEnum.MULT_MODE || thingDeviceActiveModeEnum == ThingDeviceActiveModeEnum.BLE_WIFI) {
            activeManager = ThingActivatorCoreKit.getActiveManager().newThingActiveManager()
            activeManager!!.startActive(ThingDeviceActiveBuilder().apply {
                activeModel = thingDeviceActiveModeEnum
                ssid = etSsid.text.toString().trim()
                password = etPassword.text.toString().trim()
                timeOut = 60
                relationId = homeId
                listener = object : IThingDeviceActiveListener {
                    override fun onActiveError(errorBean: ThingDeviceActiveErrorBean) {
                        setPbViewVisible(false)
                        finish()
                        Toast.makeText(
                            this@DeviceConfigDualActivity, "Config Failed", Toast.LENGTH_LONG
                        ).show()
                    }

                    override fun onActiveLimited(limitBean: ThingDeviceActiveLimitBean) {
                    }

                    override fun onActiveSuccess(deviceBean: DeviceBean) {
                        setPbViewVisible(false)
                        Toast.makeText(
                            this@DeviceConfigDualActivity, "Config Success", Toast.LENGTH_LONG
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
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
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