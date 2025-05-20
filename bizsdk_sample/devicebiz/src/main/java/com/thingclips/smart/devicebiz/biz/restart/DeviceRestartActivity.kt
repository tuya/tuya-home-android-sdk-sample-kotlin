package com.thingclips.smart.devicebiz.biz.restart

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.thingclips.smart.android.common.utils.L
import com.thingclips.smart.device.restart.sdk.DeviceRestartServiceImpl
import com.thingclips.smart.device.restart.sdk.bean.DeviceTimerWrapperBean
import com.thingclips.smart.devicebiz.biz.deviceInfo.DeviceInfoActivity
import com.thingclips.smart.devicebiz.biz.restart.utils.DeviceRestartUtils
import com.thingclips.smart.devicebiz.databinding.ActivityDeviceRestartBinding
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.sdk.api.IResultCallback

class DeviceRestartActivity : AppCompatActivity() {
    private lateinit var binding:ActivityDeviceRestartBinding
    private var deviceId: String? = null
    private var timerWrapperBean: DeviceTimerWrapperBean? = null

    companion object{
        const val TAG = "DeviceRestartActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceRestartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initData()
        initView()
    }

    private fun initData() {
        deviceId = intent.getStringExtra("deviceId")
    }

    private fun initView(){
        binding.btReboot.setOnClickListener {
            deviceId?.let {
                DeviceRestartServiceImpl.rebootImmediately(it,object : IResultCallback{
                    override fun onError(code: String?, error: String?) {
                        L.i(TAG, "code:$code,error:$error")
                    }

                    override fun onSuccess() {
                        Toast.makeText(this@DeviceRestartActivity, "restart success", Toast.LENGTH_LONG).show()
                    }
                })
            }
        }
        binding.btTimerRestart.setOnClickListener {
            val intent = Intent()
            intent.putExtra("deviceId", deviceId)
            timerWrapperBean?.let {
                intent.putExtra("timerWrapperBean",it)
            }
            intent.setClass(this@DeviceRestartActivity, SettingTimerActivity::class.java)
            startActivityForResult(intent,1001)
        }
        deviceId?.let { it ->
            DeviceRestartServiceImpl.isSupportDeviceRestart(it,object : IThingResultCallback<Boolean>{
                override fun onSuccess(result: Boolean?) {
                    result?.let {
                        if (result){
                            binding.llNoSupport.visibility = View.GONE
                            binding.llRestart.visibility = View.VISIBLE
                        }
                    }
                }

                override fun onError(errorCode: String?, errorMessage: String?) {
                    L.i(TAG, "errorCode:$errorCode,errorMessage:$errorMessage")
                }
            })
            DeviceRestartServiceImpl.getDeviceRebootTimer(it,object :IThingResultCallback<DeviceTimerWrapperBean>{
                override fun onSuccess(result: DeviceTimerWrapperBean?) {
                    clearTimerMsg()
                    result?.let { res ->
                        timerWrapperBean = res
                        runOnUiThread {
                            binding.tvTime.text = "time:${res.time}"
                            binding.tvLoop.text = "loop:${DeviceRestartUtils.setRepeatShowTime(this@DeviceRestartActivity,res.loops)}"
                            binding.tvStatus.text = "status:${res.isStatus}"
                        }
                    }
                }

                override fun onError(errorCode: String?, errorMessage: String?) {
                    L.i(TAG, "errorCode:$errorCode,errorMessage:$errorMessage")
                    timerWrapperBean = null
                }
            })
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode==1001){
            data?.let {
                val bean = data.extras?.get("timerWrapperBean")as DeviceTimerWrapperBean?
                bean?.let {
                    timerWrapperBean = it
                    runOnUiThread {
                        binding.tvTime.text = "time:${it.time}"
                        binding.tvLoop.text = "loop:${DeviceRestartUtils.setRepeatShowTime(this@DeviceRestartActivity,it.loops)}"
                        binding.tvStatus.text = "status:${it.isStatus}"
                    }
                }

            }
        }
    }

    private fun clearTimerMsg(){
        binding.tvTime.text = ""
        binding.tvLoop.text = ""
        binding.tvStatus.text = ""
    }

}