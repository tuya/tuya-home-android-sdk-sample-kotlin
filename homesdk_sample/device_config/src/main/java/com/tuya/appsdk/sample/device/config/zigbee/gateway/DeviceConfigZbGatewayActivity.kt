package com.tuya.appsdk.sample.device.config.zigbee.gateway

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
import com.tuya.appsdk.sample.resource.HomeModel
import com.thingclips.smart.android.hardware.bean.HgwBean
import com.thingclips.smart.home.sdk.ThingHomeSdk.getActivatorInstance
import com.thingclips.smart.home.sdk.builder.ThingGwActivatorBuilder
import com.thingclips.smart.sdk.api.IThingActivatorGetToken
import com.thingclips.smart.sdk.api.IThingSmartActivatorListener
import com.thingclips.smart.sdk.bean.DeviceBean

/**
 * Device Configuration ZigBee Gateway Sample
 *
 * @author aiwen <a href="mailto:developer@tuya.com"/>
 * @since 2/24/21 11:12 AM
 */
class DeviceConfigZbGatewayActivity : AppCompatActivity() {

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


    // Search ZigBee Gateway Device
    private fun searchGatewayDevice() {
        setPbViewVisible(true)
        val newSearcher = getActivatorInstance().newThingGwActivator().newSearcher()
        newSearcher.registerGwSearchListener {
            getNetworkConfigToken(it)
        }
    }


    // Get Network Configuration Token
    private fun getNetworkConfigToken(hgwBean: HgwBean) {
        Log.i(TAG, "getNetworkConfigToken: homeId=${homeId}")
        Log.i(TAG, "getNetworkConfigToken: GwId->${hgwBean.getGwId()}")

        getActivatorInstance().getActivatorToken(homeId,
                object : IThingActivatorGetToken {
                    override fun onSuccess(token: String) {
                        Log.i(TAG, "getNetworkConfigToken: onSuccess->${token}")
                        startNetworkConfig(token, hgwBean)
                    }

                    override fun onFailure(errorCode: String?, errorMsg: String?) {
                        setPbViewVisible(false)
                        Toast.makeText(
                                this@DeviceConfigZbGatewayActivity,
                                "Error->$errorMsg",
                                Toast.LENGTH_LONG
                        ).show()
                    }
                })
    }


    // Start network configuration -- ZigBee Gateway
    private fun startNetworkConfig(token: String, hgwBean: HgwBean) {
        val activatorBuilder = getActivatorInstance().newGwActivator(
                ThingGwActivatorBuilder()
                        .setContext(this@DeviceConfigZbGatewayActivity)
                        .setTimeOut(100)
                        .setToken(token)
                        .setHgwBean(hgwBean)
                        .setListener(object : IThingSmartActivatorListener {
                            override fun onError(errorCode: String?, errorMsg: String?) {
                                Log.i(TAG, "Activate error->$errorMsg")
                                setPbViewVisible(false)
                                Toast.makeText(
                                        this@DeviceConfigZbGatewayActivity,
                                        "Activate Error",
                                        Toast.LENGTH_LONG
                                ).show()
                            }

                            override fun onActiveSuccess(devResp: DeviceBean?) {
                                Log.i(TAG, "Activate success")
                                setPbViewVisible(false)
                                Toast.makeText(
                                        this@DeviceConfigZbGatewayActivity,
                                        "Activate success",
                                        Toast.LENGTH_LONG
                                ).show()
                                finish()
                            }

                            override fun onStep(step: String?, data: Any?) {
                                Log.i(TAG, "onStep: step->$step")
                            }

                        })
        )

        //Start configuration
        activatorBuilder.start()

        // Stop configuration
        // mTuyaActivator.stop()
        // Exit the page to destroy some cache data and monitoring data.
        // mTuyaActivator.onDestroy()

    }


    private fun setPbViewVisible(isShow: Boolean) {
        cpiLoading.visibility = if (isShow) View.VISIBLE else View.GONE
        searchButton.isEnabled = !isShow
    }
}