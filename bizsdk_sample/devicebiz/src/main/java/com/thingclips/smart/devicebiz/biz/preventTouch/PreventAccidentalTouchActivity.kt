package com.thingclips.smart.devicebiz.biz.preventTouch

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.thingclips.smart.android.common.utils.L
import com.thingclips.smart.android.mqtt.IThingMqttInterceptListener
import com.thingclips.smart.devicebiz.R
import com.thingclips.smart.devicebiz.databinding.ActivityPreventAccidentalTouchBinding
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.prevent.accidental.touch.PreventAccidentalTouchManager
import com.thingclips.smart.prevent.accidental.touch.bean.PreventAccidentalTouchStatusListener

class PreventAccidentalTouchActivity : AppCompatActivity() {
    private var deviceId: String? = null
    private var manager: PreventAccidentalTouchManager? = null
    private var currentStatus: Boolean = false
    private lateinit var binding: ActivityPreventAccidentalTouchBinding

    companion object {
        const val TAG = "PreventAccidentalTouchActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreventAccidentalTouchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initData()
        initView()
    }

    private val mPreventAccidentalTouchStatusListener: PreventAccidentalTouchStatusListener =
        object : PreventAccidentalTouchStatusListener {
            override fun onStatusUpdate(status: Boolean) {
                L.i(TAG,"onStatusUpdate:$status")
                runOnUiThread {
                    binding.preventSwitch.isChecked = status
                    binding.status.text = if (status) "open" else "close"
                    currentStatus = status
                }

            }
        }

    private fun initData() {
        deviceId = intent.getStringExtra("deviceId")
        deviceId?.let {
            manager = PreventAccidentalTouchManager(it)
        }
        manager?.addStatusListener(mPreventAccidentalTouchStatusListener)
        manager?.let { preventAccidentalTouchManager ->
            preventAccidentalTouchManager.getPreventAccidentalTouchStatus()
        }
    }

    private fun initView() {
        binding.preventSwitch.isChecked = currentStatus
        binding.status.text = if (currentStatus) "open" else "close"
        binding.preventSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked != currentStatus) {
                manager?.updatePreventAccidentalTouchStatus(isChecked)
            }
        }
        manager?.isSupportPreventAccidentalTouch(object : IThingResultCallback<Boolean>{
            override fun onSuccess(result: Boolean?) {
                result?.let {
                    L.i(TAG,"is support:$result")
                    if (result==true){
                        runOnUiThread {
                            binding.llNoSupport.visibility = View.GONE
                            binding.llSwitch.visibility = View.VISIBLE
                        }
                    }
                }
            }

            override fun onError(errorCode: String?, errorMessage: String?) {
                L.i(TAG, "errorCode:$errorCode,errorMessage:$errorMessage")
            }
        })
    }

    override fun onDestroy() {
        manager?.removeStatusListener(mPreventAccidentalTouchStatusListener)
        super.onDestroy()
    }
}