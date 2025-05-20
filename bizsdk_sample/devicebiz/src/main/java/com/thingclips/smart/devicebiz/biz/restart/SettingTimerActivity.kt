package com.thingclips.smart.devicebiz.biz.restart

import android.app.TimePickerDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.thingclips.smart.android.common.utils.L
import com.thingclips.smart.device.restart.sdk.DeviceRestartServiceImpl
import com.thingclips.smart.device.restart.sdk.bean.DeviceTimerWrapperBean
import com.thingclips.smart.devicebiz.R
import com.thingclips.smart.devicebiz.databinding.ActivityDeviceRestartBinding
import com.thingclips.smart.devicebiz.databinding.ActivitySettingTimerBinding
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.sdk.api.IResultCallback


class SettingTimerActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingTimerBinding
    private var deviceId: String? = null
    private var timerWrapperBean: DeviceTimerWrapperBean? = null
    private var timerWrapperDemoBean: DeviceTimerWrapperBean? = null
    private var timerWrapperDemoBean2: DeviceTimerWrapperBean? = null

    companion object {
        const val TAG = "SettingTimerActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initData()
        initView()
    }

    fun initData() {
        deviceId = intent.getStringExtra("deviceId")
        timerWrapperBean = intent.extras?.get("timerWrapperBean") as DeviceTimerWrapperBean?
        timerWrapperDemoBean = DeviceTimerWrapperBean()
        timerWrapperDemoBean!!.time = "17:01"
        timerWrapperDemoBean!!.loops = "1100000"
        timerWrapperDemoBean!!.isStatus = true
        timerWrapperDemoBean2 = DeviceTimerWrapperBean()
        timerWrapperDemoBean2!!.time = "18:01"
        timerWrapperDemoBean2!!.loops = "1111111"
        timerWrapperDemoBean2!!.isStatus = false
        if (timerWrapperBean!=null){
            timerWrapperDemoBean2!!.tid = timerWrapperBean!!.tid
        }
    }

    fun initView() {
        showCurrentTimer()
        binding.btSelect.setOnClickListener {
            DeviceRestartServiceImpl.addDeviceRebootTimer(
                deviceId!!,
                timerWrapperDemoBean!!,
                object : IThingResultCallback<String> {
                    override fun onSuccess(result: String?) {
                        result?.let {
                            timerWrapperBean = timerWrapperDemoBean
                            timerWrapperBean?.tid = it
                            timerWrapperDemoBean2?.tid = it
                            showCurrentTimer()
                        }
                    }

                    override fun onError(errorCode: String?, errorMessage: String?) {
                        L.i(TAG, "errorCode$errorCode,errorMessage:$errorMessage")
                    }
                })
        }

        binding.useTest.setOnClickListener {
            L.i(TAG,"timerWrapperDemoBean2:$timerWrapperDemoBean2")
            DeviceRestartServiceImpl.updateDeviceRebootTimer(
                deviceId!!,
                timerWrapperDemoBean2!!,
                object : IResultCallback {
                    override fun onSuccess() {
                        timerWrapperBean = timerWrapperDemoBean2
                        showCurrentTimer()
                    }

                    override fun onError(errorCode: String?, errorMessage: String?) {
                        L.i(TAG, "errorCode$errorCode,errorMessage:$errorMessage")
                    }
                })
        }
    }

    fun showCurrentTimer() {
        timerWrapperBean?.let {
            runOnUiThread {
                binding.tvTime.text = it.time
                binding.tvLoop.text = it.loops
                binding.switchOpen.isChecked = if (it.isStatus!=null) it.isStatus else true
                L.i(TAG,"status:${it.isStatus}")
            }
        }
    }
}