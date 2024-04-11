package com.tuya.lock.demo.ble.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingBleLockV2
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.lock.api.bean.OfflineTempPassword
import com.thingclips.smart.optimus.lock.api.enums.OfflineTempPasswordType
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.Utils.getDateDay
import com.tuya.lock.demo.common.utils.Utils.getStampTime

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class PasswordProOfflineAddActivity : AppCompatActivity() {

    companion object {
        fun startActivity(context: Context?, devId: String?, passwordType: String) {
            val intent = Intent(context, PasswordProOfflineAddActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            intent.putExtra(Constant.PASSWORD_TYPE, passwordType)
            context?.startActivity(intent)
        }
    }

    private var tuyaLockDevice: IThingBleLockV2? = null
    private var pwdType: OfflineTempPasswordType? = null
    private var gmtStart: Long = 0
    private var gmtExpired: Long = 0
    private var password_content: TextView? = null
    private var name = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_offline_add)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        val mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        tuyaLockDevice = tuyaLockManager.getBleLockV2(mDevId)
        when (intent.getStringExtra(Constant.PASSWORD_TYPE)) {
            Constant.TYPE_SINGLE -> {
                pwdType = OfflineTempPasswordType.SINGLE
                toolbar.title = resources.getString(R.string.password_one_time)
            }

            Constant.TYPE_MULTIPLE -> {
                pwdType = OfflineTempPasswordType.MULTIPLE
                toolbar.title = resources.getString(R.string.offline_password)
            }

            Constant.TYPE_CLEAR_ALL -> {
                pwdType = OfflineTempPasswordType.CLEAR_ALL
                toolbar.title = resources.getString(R.string.passwords_clear)
            }
        }
        val password_offline_add_start = findViewById<EditText>(R.id.password_offline_add_start)
        gmtStart = System.currentTimeMillis()
        password_offline_add_start.setText(getDateDay(gmtStart))
        password_offline_add_start.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!TextUtils.isEmpty(s)) {
                    val selectTime = s.toString()
                    gmtStart = getStampTime(selectTime)
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        val password_offline_add_name = findViewById<EditText>(R.id.password_offline_add_name)
        password_offline_add_name.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!TextUtils.isEmpty(s)) {
                    name = s.toString()
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        val password_offline_add_expired = findViewById<EditText>(R.id.password_offline_add_expired)
        gmtExpired = System.currentTimeMillis() + 7 * 86400000L
        password_offline_add_expired.setText(getDateDay(gmtExpired))
        password_offline_add_expired.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!TextUtils.isEmpty(s)) {
                    val selectTime = s.toString()
                    gmtExpired = getStampTime(selectTime)
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        password_content = findViewById(R.id.password_content)
        findViewById<View>(R.id.password_offline_add).setOnClickListener { v: View? -> getProOfflinePassword() }
    }

    override fun onDestroy() {
        super.onDestroy()
        tuyaLockDevice!!.onDestroy()
    }

    private fun getProOfflinePassword() {
        tuyaLockDevice!!.getProOfflinePassword(
            pwdType,
            gmtStart,
            gmtExpired,
            name,
            object : IThingResultCallback<OfflineTempPassword?> {
                override fun onSuccess(result: OfflineTempPassword?) {
                    Log.i(
                        Constant.TAG,
                        "setOfflineTempPasswordName success :" + JSONObject.toJSONString(result)
                    )
                    password_content!!.text = JSONObject.toJSONString(result)
                    Toast.makeText(applicationContext, "success", Toast.LENGTH_SHORT).show()
                }

                override fun onError(errorCode: String, errorMessage: String) {
                    Log.e(
                        Constant.TAG,
                        "setOfflineTempPasswordName failed: code = $errorCode  message = $errorMessage"
                    )
                    Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
                }
            })
    }
}