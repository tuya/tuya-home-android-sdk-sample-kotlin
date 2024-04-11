package com.tuya.lock.demo.wifi.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.lock.api.IThingWifiLock
import com.thingclips.smart.optimus.lock.api.TempPasswordBuilder
import com.thingclips.smart.optimus.lock.api.bean.TempPassword
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.Utils

/**
 *
 * Created by HuiYao on 2024/3/1
 */
class PasswordDetailActivity: AppCompatActivity() {

    private var wifiLock: IThingWifiLock? = null


    private var dataBean: TempPassword? = null

    private var passwordValue: String? = null
    private var mFrom = 0


    companion object{
        fun startActivity(context: Context, devId: String?) {
            val intent = Intent(context, PasswordDetailActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            context.startActivity(intent)
        }

        fun startEditActivity(context: Context, devId: String?, bean: TempPassword?) {
            val intent = Intent(context, PasswordDetailActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            intent.putExtra(Constant.PASSWORD_DATA, JSONObject.toJSONString(bean))
            intent.putExtra(Constant.FROM, 1)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_password_temp_add)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        val mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        mFrom = intent.getIntExtra(Constant.FROM, 0)
        dataBean = JSONObject.parseObject(
            intent.getStringExtra(Constant.PASSWORD_DATA),
            TempPassword::class.java
        )
        toolbar.title = resources.getString(R.string.zigbee_temp_pwd)
        if (null == dataBean) {
            dataBean = TempPassword()
        }
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        wifiLock = tuyaLockManager.getWifiLock(mDevId)
        val password_name = findViewById<EditText>(R.id.password_name)
        password_name.setText(dataBean!!.name)
        password_name.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!TextUtils.isEmpty(s)) {
                    dataBean!!.name = s.toString()
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        val password_content = findViewById<EditText>(R.id.password_content)
        password_content.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!TextUtils.isEmpty(s)) {
                    passwordValue = s.toString()
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        val password_effective_time = findViewById<EditText>(R.id.password_effective_time)
        if (dataBean!!.effectiveTime == 0L) {
            dataBean!!.effectiveTime = System.currentTimeMillis()
        }
        var effectiveTime = dataBean!!.effectiveTime
        if (effectiveTime.toString().length == 10) {
            effectiveTime *= 1000
        }
        password_effective_time.setText(Utils.getDateDay(effectiveTime))
        password_effective_time.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!TextUtils.isEmpty(s)) {
                    dataBean!!.effectiveTime = Utils.getStampTime(s.toString())
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        val password_invalid_time = findViewById<EditText>(R.id.password_invalid_time)
        if (dataBean!!.invalidTime == 0L) {
            dataBean!!.invalidTime = System.currentTimeMillis() + 7 * 86400000L
        }
        var invalidTime = dataBean!!.invalidTime
        if (invalidTime.toString().length == 10) {
            invalidTime *= 1000
        }
        password_invalid_time.setText(Utils.getDateDay(invalidTime))
        password_invalid_time.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!TextUtils.isEmpty(s)) {
                    dataBean!!.invalidTime = Utils.getStampTime(s.toString())
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        if (mFrom == 1) {
            findViewById<View>(R.id.password_content_wrap).visibility = View.GONE
            findViewById<View>(R.id.password_content_line).visibility = View.GONE
        } else {
            findViewById<View>(R.id.password_content_wrap).visibility = View.VISIBLE
            findViewById<View>(R.id.password_content_line).visibility = View.VISIBLE
        }
        findViewById<View>(R.id.password_effective_time_main).visibility = View.VISIBLE
        findViewById<View>(R.id.password_effective_time_line).visibility = View.VISIBLE
        findViewById<View>(R.id.password_invalid_time_main).visibility = View.VISIBLE
        findViewById<View>(R.id.password_invalid_time_line).visibility = View.VISIBLE
        findViewById<View>(R.id.password_add).setOnClickListener { v: View? -> createPassword() }
    }

    override fun onDestroy() {
        super.onDestroy()
        wifiLock!!.onDestroy()
    }

    private fun createPassword() {
        val builder = TempPasswordBuilder()
        builder.password(passwordValue)
        builder.name(dataBean!!.name)
        builder.invalidTime(dataBean!!.invalidTime)
        builder.effectiveTime(dataBean!!.effectiveTime)
        Log.i(Constant.TAG, "request:$builder")
        wifiLock!!.createTempPassword(builder, object : IThingResultCallback<Boolean?> {
            override fun onSuccess(result: Boolean?) {
                Toast.makeText(applicationContext, "onSuccess", Toast.LENGTH_SHORT).show()
                finish()
            }

            override fun onError(errorCode: String, errorMessage: String) {
                Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }
}