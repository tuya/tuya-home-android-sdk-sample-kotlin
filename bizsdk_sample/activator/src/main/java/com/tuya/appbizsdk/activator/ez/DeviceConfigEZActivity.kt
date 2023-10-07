/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2021 Tuya Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tuya.appbizsdk.activator.ez

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.thingclips.smart.activator.core.kit.ThingActivatorCoreKit
import com.thingclips.smart.activator.core.kit.ThingActivatorDeviceCoreKit
import com.thingclips.smart.activator.core.kit.active.inter.IThingActiveManager
import com.thingclips.smart.activator.core.kit.bean.ThingDeviceActiveErrorBean
import com.thingclips.smart.activator.core.kit.bean.ThingDeviceActiveLimitBean
import com.thingclips.smart.activator.core.kit.builder.ThingDeviceActiveBuilder
import com.thingclips.smart.activator.core.kit.constant.ThingDeviceActiveModeEnum
import com.thingclips.smart.activator.core.kit.listener.IThingDeviceActiveListener
import com.thingclips.smart.sdk.api.IThingActivatorGetToken
import com.thingclips.smart.sdk.bean.DeviceBean
import com.tuya.appbizsdk.activator.R
import com.tuya.appsdk.sample.resource.HomeModel


/**
 * Device Configuration EZ Mode Sample
 *
 * @author qianqi <a href="mailto:developer@tuya.com">Contact me.</a>
 * @since 2021/1/5 5:13 PM
 */
class DeviceConfigEZActivity : AppCompatActivity(), View.OnClickListener {
    companion object {
        const val TAG = "DeviceConfigEZ"
    }

    lateinit var cpiLoading: CircularProgressIndicator
    lateinit var btnSearch: Button
    lateinit var mContentTv: TextView

    private var activeManager: IThingActiveManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_config_activity)

        val toolbar: Toolbar = findViewById<View>(R.id.topAppBar) as Toolbar
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.title = getString(R.string.device_config_ez_title)
        mContentTv = findViewById(R.id.content_tv)
        mContentTv.text = getString(R.string.device_config_ez_description)

        cpiLoading = findViewById(R.id.cpiLoading)
        btnSearch = findViewById(R.id.btnSearch)
        btnSearch.setOnClickListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        activeManager?.stopActive()
    }

    override fun onClick(v: View?) {
        val strSsid = findViewById<EditText>(R.id.etSsid).text.toString()
        val strPassword = findViewById<EditText>(R.id.etPassword).text.toString()

        v?.id?.let { it ->
            if (it == R.id.btnSearch) {
                val homeId = HomeModel.INSTANCE.getCurrentHome(this)
                // Get Network Configuration Token
                ThingActivatorDeviceCoreKit.getActivatorInstance()
                    .getActivatorToken(homeId, object : IThingActivatorGetToken {
                        override fun onSuccess(mToken: String) {
                            // Start network configuration -- EZ mode
                            activeManager =
                                ThingActivatorCoreKit.getActiveManager().newThingActiveManager()
                            activeManager!!.startActive(ThingDeviceActiveBuilder().apply {
                                activeModel = ThingDeviceActiveModeEnum.EZ
                                ssid = strSsid
                                password = strPassword
                                token = mToken
                                timeOut = 100
                                context = this@DeviceConfigEZActivity
                                listener = object : IThingDeviceActiveListener {

                                    override fun onActiveError(errorBean: ThingDeviceActiveErrorBean) {
                                        cpiLoading.visibility = View.GONE
                                        btnSearch.isClickable = true

                                        Toast.makeText(
                                            this@DeviceConfigEZActivity,
                                            "Activate error-->${errorBean.errMsg}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }

                                    override fun onActiveLimited(limitBean: ThingDeviceActiveLimitBean) {
                                    }

                                    override fun onActiveSuccess(deviceBean: DeviceBean) {
                                        cpiLoading.visibility = View.GONE

                                        Log.i(TAG, "Activate success")
                                        Toast.makeText(
                                            this@DeviceConfigEZActivity,
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

                            //Show loading progress, disable btnSearch clickable
                            cpiLoading.visibility = View.VISIBLE
                            btnSearch.isClickable = false
                        }

                        override fun onFailure(s: String, s1: String) {}
                    })
            }
        }
    }
}