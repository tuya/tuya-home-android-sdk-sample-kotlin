package com.tuya.appbizsdk.activator.dual

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
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
import com.thingclips.smart.activator.core.kit.bean.ThingActiveWifiInfoBean
import com.thingclips.smart.activator.core.kit.bean.ThingDeviceActiveErrorBean
import com.thingclips.smart.activator.core.kit.bean.ThingDeviceActiveLimitBean
import com.thingclips.smart.activator.core.kit.bean.WifiInfoRequestBean
import com.thingclips.smart.activator.core.kit.builder.ThingDeviceActiveBuilder
import com.thingclips.smart.activator.core.kit.callback.ThingActivatorScanCallback
import com.thingclips.smart.activator.core.kit.constant.ThingDeviceActiveModeEnum
import com.thingclips.smart.activator.core.kit.listener.IResultResponse
import com.thingclips.smart.activator.core.kit.listener.IThingDeviceActiveListener
import com.thingclips.smart.activator.core.kit.utils.isSupportObtainWifiListBeforeActive
import com.thingclips.smart.android.ble.api.ScanType
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.sdk.bean.DeviceBean
import com.tuya.appbizsdk.activator.R
import com.tuya.appsdk.sample.resource.HomeModel

class DeviceConfigWifiListActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DeviceConfigWifiList"
        const val REQUEST_CODE = 1002
    }

    private val homeId: Long by lazy {
        HomeModel.INSTANCE.getCurrentHome(this)
    }

    private lateinit var etPassword: TextInputEditText
    private lateinit var btSearch: Button
    private lateinit var cpiLoading: CircularProgressIndicator
    private lateinit var lvWifiList: ListView
    private lateinit var tvStatus: TextView

    private var activeManager: IThingActiveManager? = null
    private var scanKey: ThingActivatorScanKey? = null
    private var currentSsid: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_config_wifi_list_activity)
        initToolbar()
        initView()
        checkPermission()
    }

    private fun initView() {
        etPassword = findViewById(R.id.etPassword)
        btSearch = findViewById(R.id.btnSearch)
        cpiLoading = findViewById(R.id.cpiLoading)
        lvWifiList = findViewById(R.id.lvWifiList)
        tvStatus = findViewById(R.id.tvStatus)

        btSearch.setOnClickListener {
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
        toolbar.setTitle(R.string.device_config_wifi_list_title)
    }

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
                ), REQUEST_CODE
            )
        }
    }

    private fun setPbViewVisible(isShow: Boolean) {
        cpiLoading.visibility = if (isShow) View.VISIBLE else View.GONE
        btSearch.isEnabled = !isShow
    }

    private fun setStatus(status: String) {
        tvStatus.text = status
    }

    override fun onDestroy() {
        super.onDestroy()
        ThingActivatorCoreKit.getScanDeviceManager().stopScan(scanKey)
        activeManager?.stopActive()
    }

    // Scan for dual-mode devices
    private fun scanDualBleDevice() {
        setPbViewVisible(true)
        setStatus("Searching for devices...")
        
        scanKey = ThingActivatorCoreKit.getScanDeviceManager().startBlueToothDeviceSearch(
            60 * 1000,
            arrayListOf(ScanType.SINGLE),
            object : ThingActivatorScanCallback {
                override fun deviceFound(deviceBean: ThingActivatorScanDeviceBean) {
                    Log.i(TAG, "Device found: uniqueId=${deviceBean.uniqueId}")
                    
                    // Check if device supports WiFi list retrieval
                    if (deviceBean.isSupportObtainWifiListBeforeActive()) {
                        requestWifiList(deviceBean)
                    } else {
                        setStatus("Device does not support WiFi list retrieval")
                        setPbViewVisible(false)
                    }
                }

                override fun deviceRepeat(deviceBean: ThingActivatorScanDeviceBean) {}
                override fun deviceUpdate(deviceBean: ThingActivatorScanDeviceBean) {}
                override fun scanFailure(failureBean: ThingActivatorScanFailureBean) {
                    setStatus("Device scan failed: ${failureBean.errorMsg}")
                    setPbViewVisible(false)
                }
                override fun scanFinish() {
                    if (tvStatus.text.isEmpty()) {
                        setStatus("No compatible devices found")
                        setPbViewVisible(false)
                    }
                }
            }
        )
    }

    // Request WiFi list from device
    private fun requestWifiList(deviceBean: ThingActivatorScanDeviceBean) {
        setStatus("Getting WiFi list from device...")
        
        val wifiReqBean = WifiInfoRequestBean().apply {
            uuid = deviceBean.uniqueId
            size = 10
            timeout = 5000L
            modeEnum = ThingDeviceActiveModeEnum.BLE_WIFI
            scanDeviceBean = deviceBean
        }

        ThingActivatorCoreKit.getActiveManager().newThingActiveManager().requestWifiList(
            wifiReqBean,
            object : IResultResponse<List<ThingActiveWifiInfoBean>> {
                override fun onError(errorCode: String?, errorMessage: String?) {
                    setStatus("Failed to get WiFi list: $errorMessage")
                    setPbViewVisible(false)
                }

                override fun onSuccess(result: List<ThingActiveWifiInfoBean>) {
                    setPbViewVisible(false)
                    displayWifiList(result, deviceBean)
                }
            }
        )
    }

    // Display WiFi list and handle selection
    private fun displayWifiList(wifiList: List<ThingActiveWifiInfoBean>, deviceBean: ThingActivatorScanDeviceBean) {
        if (wifiList.isEmpty()) {
            setStatus("No WiFi networks found")
            return
        }

        setStatus("Select a WiFi network")
        
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            wifiList.map { it.ssid }
        )
        lvWifiList.adapter = adapter

        lvWifiList.setOnItemClickListener { _, _, position, _ ->
            currentSsid = wifiList[position].ssid.toString()
            startActivator(deviceBean)
        }
    }

    // Start device activation
    private fun startActivator(deviceBean: ThingActivatorScanDeviceBean) {
        val password = etPassword.text.toString().trim()
        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter WiFi password", Toast.LENGTH_SHORT).show()
            return
        }

        setPbViewVisible(true)
        setStatus("Connecting to $currentSsid...")

        activeManager = ThingActivatorCoreKit.getActiveManager().newThingActiveManager()
        activeManager!!.startActive(ThingDeviceActiveBuilder().apply {
            activeModel = ThingDeviceActiveModeEnum.BLE_WIFI
            ssid = currentSsid
            this.password = password
            timeOut = 60
            relationId = homeId
            listener = object : IThingDeviceActiveListener {
                override fun onActiveError(errorBean: ThingDeviceActiveErrorBean) {
                    setPbViewVisible(false)
                    setStatus("Configuration failed: ${errorBean.errMsg}")
                }

                override fun onActiveLimited(limitBean: ThingDeviceActiveLimitBean) {}

                override fun onActiveSuccess(deviceBean: DeviceBean) {
                    setPbViewVisible(false)
                    setStatus("Device configured successfully")
                    Toast.makeText(
                        this@DeviceConfigWifiListActivity,
                        "Configuration successful",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }

                override fun onBind(devId: String) {}
                override fun onFind(devId: String) {}
            }
        })
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
                    Log.i(TAG, "Permissions granted")
                } else {
                    Log.e(TAG, "Permissions denied")
                    finish()
                }
            }
        }
    }
}
