package com.tuya.appbizsdk.activator.zigbee.gateway

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.thingclips.smart.activator.core.kit.ThingActivatorCoreKit
import com.thingclips.smart.activator.core.kit.active.inter.IThingActiveManager
import com.thingclips.smart.activator.core.kit.bean.ThingActivatorScanDeviceBean
import com.thingclips.smart.activator.core.kit.bean.ThingActivatorScanFailureBean
import com.thingclips.smart.activator.core.kit.bean.ThingDeviceActiveErrorBean
import com.thingclips.smart.activator.core.kit.bean.ThingDeviceActiveLimitBean
import com.thingclips.smart.activator.core.kit.builder.ThingDeviceActiveBuilder
import com.thingclips.smart.activator.core.kit.callback.ThingActivatorScanCallback
import com.thingclips.smart.activator.core.kit.constant.ThingDeviceActiveModeEnum
import com.thingclips.smart.activator.core.kit.listener.IThingDeviceActiveListener
import com.tuya.appsdk.sample.resource.HomeModel
import com.thingclips.smart.sdk.bean.DeviceBean
import com.tuya.appbizsdk.activator.R

/**
 * Device Configuration ZigBee Gateway Sample
 *
 * @author aiwen <a href="mailto:developer@tuya.com"/>
 * @since 2/24/21 11:12 AM
 */
class DeviceConfigZbGatewayActivity : AppCompatActivity() {

    private var activeManager: IThingActiveManager? = null

    companion object {
        private const val TAG = "DeviceConfigZbGateway"
    }


    private val homeId: Long by lazy {
        HomeModel.INSTANCE.getCurrentHome(this@DeviceConfigZbGatewayActivity)
    }


    lateinit var cpiLoading: CircularProgressIndicator
    lateinit var searchButton: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_config_info_hint_activity)
        initToolbar()
        initView()
    }


    private fun initView() {
        findViewById<TextView>(R.id.tv_hint_info).text =
            getString(R.string.device_config_zb_gateway_hint)
        cpiLoading = findViewById(R.id.cpiLoading)

        searchButton = findViewById(R.id.bt_search)
        searchButton.setOnClickListener {
            searchGatewayDevice()
        }
    }

    private fun initToolbar() {
        val toolbar: Toolbar = findViewById<View>(R.id.topAppBar) as Toolbar
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.setTitle(R.string.device_config_zb_gateway_title)
    }


    override fun onDestroy() {
        super.onDestroy()
        activeManager?.stopActive()
    }

    // Search ZigBee Gateway Device
    private fun searchGatewayDevice() {
        setPbViewVisible(true)
        ThingActivatorCoreKit.getScanDeviceManager().startLocalGatewayDeviceSearch(
            60 * 1000,
            object : ThingActivatorScanCallback {
                override fun deviceFound(deviceBean: ThingActivatorScanDeviceBean) {
                    startNetworkConfig(deviceBean)
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

    // Start network configuration -- ZigBee Gateway
    private fun startNetworkConfig(bean: ThingActivatorScanDeviceBean) {

        activeManager = ThingActivatorCoreKit.getActiveManager().newThingActiveManager()
        activeManager!!.startActive(ThingDeviceActiveBuilder().apply {
            activeModel = ThingDeviceActiveModeEnum.WN
            timeOut = 60
            context = this@DeviceConfigZbGatewayActivity
            setActivatorScanDeviceBean(bean)
            listener = object : IThingDeviceActiveListener {
                override fun onActiveError(errorBean: ThingDeviceActiveErrorBean) {
                    Log.i(TAG, "Activate error->${errorBean.errMsg}")
                    setPbViewVisible(false)
                    Toast.makeText(
                        this@DeviceConfigZbGatewayActivity,
                        "Activate Error",
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onActiveLimited(limitBean: ThingDeviceActiveLimitBean) {
                }

                override fun onActiveSuccess(deviceBean: DeviceBean) {
                    Log.i(TAG, "Activate success")
                    setPbViewVisible(false)
                    Toast.makeText(
                        this@DeviceConfigZbGatewayActivity,
                        "Activate success",
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


    private fun setPbViewVisible(isShow: Boolean) {
        cpiLoading.visibility = if (isShow) View.VISIBLE else View.GONE
        searchButton.isEnabled = !isShow
    }
}