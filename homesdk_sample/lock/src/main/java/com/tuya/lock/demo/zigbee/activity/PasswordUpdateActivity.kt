package com.tuya.lock.demo.zigbee.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.lock.api.IThingZigBeeLock
import com.thingclips.smart.optimus.lock.api.zigbee.response.PasswordBean
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant

/**
 *
 * Created by HuiYao on 2024/3/1
 */
class PasswordUpdateActivity: AppCompatActivity() {

    private var zigBeeLock: IThingZigBeeLock? = null

    private var mPasswordData: PasswordBean.DataBean? = null

    companion object{
        fun startActivity(
            context: Context, passwordItem: PasswordBean.DataBean?,
            devId: String?
        ) {
            val intent = Intent(context, PasswordUpdateActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            //编辑的密码数据
            intent.putExtra(Constant.PASSWORD_DATA, JSONObject.toJSONString(passwordItem))
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zigbee_password_temp_update)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.title = getString(R.string.submit_edit)
        val mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        mPasswordData = JSONObject.parseObject(
            intent.getStringExtra(Constant.PASSWORD_DATA),
            PasswordBean.DataBean::class.java
        )
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        zigBeeLock = tuyaLockManager.getZigBeeLock(mDevId)
        val password_name = findViewById<EditText>(R.id.password_name)
        password_name.setText(mPasswordData?.name)
        password_name.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!TextUtils.isEmpty(s)) {
                    mPasswordData?.name = s.toString()
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        findViewById<View>(R.id.password_add).setOnClickListener { v: View? -> createPassword() }
    }

    private fun createPassword() {
        zigBeeLock!!.updateTemporaryPassword(
            mPasswordData!!.name,
            mPasswordData!!.id,
            object : IThingResultCallback<Boolean?> {
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