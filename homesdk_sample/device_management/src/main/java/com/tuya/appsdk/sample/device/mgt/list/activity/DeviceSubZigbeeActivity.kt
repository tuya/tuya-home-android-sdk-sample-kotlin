package com.tuya.appsdk.sample.device.mgt.list.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tuya.appsdk.sample.device.mgt.R
import com.tuya.appsdk.sample.device.mgt.list.adapter.DeviceMgtAdapter
import com.tuya.appsdk.sample.device.mgt.list.enum.DeviceListTypePage
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.sdk.api.IThingDataCallback
import com.thingclips.smart.sdk.bean.DeviceBean


/**
 * ZigBee Sub-device List Sample
 *
 * @author aiwen <a href="mailto:developer@tuya.com"/>
 * @since 2/25/21 2:26 PM
 */
class DeviceSubZigbeeActivity : AppCompatActivity() {


    lateinit var adapter: DeviceMgtAdapter

    lateinit var deviceId: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_mgt_activity_list)
        deviceId = intent.getStringExtra("deviceId").toString()



        initToolbar()

        val rvList = findViewById<RecyclerView>(R.id.rvList)
        rvList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        adapter = DeviceMgtAdapter(DeviceListTypePage.ZIGBEE_SUB_DEVICE_LIST)
        rvList.adapter = adapter


        getZbSubDeviceList()

    }


    // Get Sub-devices
    private fun getZbSubDeviceList() {
        ThingHomeSdk.newGatewayInstance(deviceId)
                .getSubDevList(object : IThingDataCallback<List<DeviceBean>> {
                    override fun onSuccess(result: List<DeviceBean>?) {
                        result?.let {
                            adapter.data = it as ArrayList<DeviceBean>
                            adapter.notifyDataSetChanged()
                        }
                    }

                    override fun onError(errorCode: String?, errorMessage: String?) {
                        Toast.makeText(
                                this@DeviceSubZigbeeActivity,
                                "Error->$errorMessage",
                                Toast.LENGTH_LONG
                        ).show()
                    }

                })

    }


    private fun initToolbar() {
        val toolbar: Toolbar = findViewById<View>(R.id.topAppBar) as Toolbar
        toolbar.setNavigationOnClickListener {
            finish()
        }

        toolbar.title = getString(R.string.device_zb_sub_device_list)
    }

}