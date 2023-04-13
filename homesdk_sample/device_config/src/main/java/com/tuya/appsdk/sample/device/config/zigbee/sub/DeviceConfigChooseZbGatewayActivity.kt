package com.tuya.appsdk.sample.device.config.zigbee.sub

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tuya.appsdk.sample.device.config.R
import com.tuya.appsdk.sample.device.config.zigbee.adapter.ZigBeeGatewayListAdapter
import com.tuya.appsdk.sample.resource.HomeModel
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.home.sdk.bean.HomeBean
import com.thingclips.smart.home.sdk.callback.IThingHomeResultCallback
import com.thingclips.smart.sdk.bean.DeviceBean

/**
 * Choose Gateway
 *
 * @author aiwen <a href="mailto:developer@tuya.com"/>
 * @since 2/25/21 9:45 AM
 */
class DeviceConfigChooseZbGatewayActivity : AppCompatActivity() {

    lateinit var adapter: ZigBeeGatewayListAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_config_zb_choose_gateway_activity)
        initToolbar()
        initView()
    }

    private fun initView() {
        val rvList = findViewById<RecyclerView>(R.id.rvList)
        adapter = ZigBeeGatewayListAdapter(this)


        // Set List
        val linearLayoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        rvList.layoutManager = linearLayoutManager
        rvList.adapter = adapter
        getZigBeeGatewayList()
    }

    // Get ZigBee Gateway List
    private fun getZigBeeGatewayList() {
        val currentHomeId = HomeModel.INSTANCE.getCurrentHome(this)
        ThingHomeSdk.newHomeInstance(currentHomeId).getHomeDetail(object : IThingHomeResultCallback {
            override fun onSuccess(bean: HomeBean?) {
                val deviceList = bean?.deviceList as ArrayList<DeviceBean>
                val zigBeeGatewayList = deviceList.filter {
                    it.isZigBeeWifi
                }
                adapter.data = zigBeeGatewayList as ArrayList<DeviceBean>
                adapter.notifyDataSetChanged()
            }

            override fun onError(errorCode: String?, errorMsg: String?) {
                Toast.makeText(
                        this@DeviceConfigChooseZbGatewayActivity,
                        "Error->$errorMsg",
                        Toast.LENGTH_LONG
                ).show()
            }
        })
    }


    // init Toolbar
    private fun initToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.setTitle(R.string.device_config_choose_gateway_title)
    }
}