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

package com.tuya.appsdk.sample.device.config.ap

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.tuya.appsdk.sample.device.config.R
import com.tuya.appsdk.sample.resource.HomeModel
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.home.sdk.builder.ActivatorBuilder
import com.thingclips.smart.sdk.api.IThingActivator
import com.thingclips.smart.sdk.api.IThingActivatorGetToken
import com.thingclips.smart.sdk.api.IThingSmartActivatorListener
import com.thingclips.smart.sdk.bean.DeviceBean
import com.thingclips.smart.sdk.enums.ActivatorModelEnum


/**
 * Device Configuration AP Mode Sample
 *
 * @author qianqi <a href="mailto:developer@tuya.com">Contact me.</a>
 * @since 2021/1/5 5:13 PM
 */
class DeviceConfigAPActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        const val TAG = "DeviceConfigEZ"
    }

    lateinit var cpiLoading: CircularProgressIndicator
    lateinit var btnSearch: Button
    lateinit var mToken: String
    private var mTuyaActivator: IThingActivator? = null
    lateinit var strSsid: String
    lateinit var strPassword: String
    lateinit var mContentTv: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_config_activity)

        val toolbar: Toolbar = findViewById<View>(R.id.topAppBar) as Toolbar
        toolbar.setNavigationOnClickListener {
            finish()
        }
        toolbar.title = getString(R.string.device_config_ap_title)
        mContentTv=findViewById(R.id.content_tv)
        mContentTv.text=getString(R.string.device_config_ap_description)

        cpiLoading = findViewById(R.id.cpiLoading)
        btnSearch = findViewById(R.id.btnSearch)
        btnSearch.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        strSsid = findViewById<EditText>(R.id.etSsid).text.toString()
        strPassword = findViewById<EditText>(R.id.etPassword).text.toString()

        v?.id?.let {
            if (it == R.id.btnSearch) {
                val homeId = HomeModel.INSTANCE.getCurrentHome(this)
                // Get Network Configuration Token
                ThingHomeSdk.getActivatorInstance().getActivatorToken(homeId,
                        object : IThingActivatorGetToken {
                            override fun onSuccess(token: String) {
                                mToken = token
                                // Start network configuration -- AP mode

                                onClickSetting()
                                //Stop configuration
//                                mTuyaActivator.stop()
                                //Exit the page to destroy some cache data and monitoring data.
//                                mTuyaActivator.onDestroy()
                            }

                            override fun onFailure(s: String, s1: String) {

                            }
                        })
            }
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onRestart() {
        super.onRestart()
        //Show loading progress, disable btnSearch clickable
        cpiLoading.visibility = View.VISIBLE
        btnSearch.isClickable = false
        cpiLoading.isIndeterminate = true
        val builder = ActivatorBuilder()
                .setSsid(strSsid)
                .setContext(this)
                .setPassword(strPassword)
                .setActivatorModel(ActivatorModelEnum.THING_AP)
                .setTimeOut(100)
                .setToken(mToken)
                .setListener(object : IThingSmartActivatorListener {

                    @Override
                    override fun onStep(step: String?, data: Any?) {
                        Log.i(TAG, "$step --> $data")
                    }

                    override fun onActiveSuccess(devResp: DeviceBean?) {
                        cpiLoading.visibility = View.GONE

                        Log.i(TAG, "Activate success")
                        Toast.makeText(
                                this@DeviceConfigAPActivity,
                                "Activate success",
                                Toast.LENGTH_LONG
                        ).show()

                        finish()
                    }

                    override fun onError(
                            errorCode: String?,
                            errorMsg: String?
                    ) {
                        cpiLoading.visibility = View.GONE
                        btnSearch.isClickable = true

                        Toast.makeText(
                                this@DeviceConfigAPActivity,
                                "Activate error-->$errorMsg",
                                Toast.LENGTH_LONG
                        ).show()
                    }
                }
                )
        mTuyaActivator =
            ThingHomeSdk.getActivatorInstance().newActivator(builder)
        //Start configuration
        mTuyaActivator?.start()


    }

    /**
     *
     * wifi setting
     */
    private fun onClickSetting() {
        var wifiSettingsIntent = Intent("android.settings.WIFI_SETTINGS")
        if (null == wifiSettingsIntent.resolveActivity(packageManager)) {
            wifiSettingsIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
        }
        if (null == wifiSettingsIntent.resolveActivity(packageManager)){
            return
        }
        startActivity(wifiSettingsIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        mTuyaActivator?.onDestroy()
    }

}