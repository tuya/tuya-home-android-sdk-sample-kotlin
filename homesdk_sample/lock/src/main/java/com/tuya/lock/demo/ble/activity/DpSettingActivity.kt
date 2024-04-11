package com.tuya.lock.demo.ble.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.sdk.api.IDevListener
import com.thingclips.smart.sdk.api.IResultCallback
import com.thingclips.smart.sdk.api.IThingDevice
import com.thingclips.smart.sdk.optimus.lock.utils.LockUtil
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.DialogUtils.showNumberEdit

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class DpSettingActivity : AppCompatActivity() {

    private var IThingDevice: IThingDevice? = null
    private var set_time_delay_switch: SwitchCompat? = null
    private var set_time_delay_select: TextView? = null
    private var mDevId: String? = null
    private var timeInt = 0
    private var language_select_group: RadioGroup? = null
    private var language_china: RadioButton? = null
    private var language_english: RadioButton? = null

    companion object {
        fun startActivity(context: Context, devId: String?) {
            val intent = Intent(context, DpSettingActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dp_setting)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        IThingDevice = ThingHomeSdk.newDeviceInstance(mDevId)
        IThingDevice!!.registerDevListener(listener)
        language_select_group = findViewById(R.id.language_select_group)
        language_china = findViewById(R.id.language_china)
        language_english = findViewById(R.id.language_english)
        set_time_delay_switch = findViewById(R.id.set_time_delay_switch)
        set_time_delay_select = findViewById(R.id.set_time_delay_select)
        val time_delay_select_wrap = findViewById<LinearLayout>(R.id.time_delay_select_wrap)
        time_delay_select_wrap.setOnClickListener { v: View ->
            if (!getIsOnline()) {
                Toast.makeText(
                    v.context,
                    "device offline",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            showNumberEdit(v.context, timeInt) { number: Int ->
                timeInt = number
                val timeValue = number.toString() + "s"
                set_time_delay_select!!.text = timeValue
                //下发dp
                val dpId = LockUtil.convertCode2Id(mDevId, "auto_lock_time")
                val dpMap: MutableMap<String?, Any> =
                    HashMap()
                dpMap[dpId] = timeInt
                publishDps(dpMap)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        IThingDevice!!.unRegisterDevListener()
        IThingDevice!!.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        initUi()
    }

    private fun getIsOnline(): Boolean {
        val deviceBean = ThingHomeSdk.getDataInstance().getDeviceBean(mDevId) ?: return false
        return deviceBean.isOnline
    }

    private fun initUi() {
        val deviceBean = ThingHomeSdk.getDataInstance().getDeviceBean(mDevId) ?: return
        val isOnline = deviceBean.isOnline
        val automatic_lock_dp_id = LockUtil.convertCode2Id(mDevId, "automatic_lock")
        val auto_lock_time_dp_id = LockUtil.convertCode2Id(mDevId, "auto_lock_time")
        if (null != automatic_lock_dp_id) {
            val automatic_lock_dp_value = deviceBean.getDps()[automatic_lock_dp_id].toString()
            set_time_delay_switch!!.isEnabled = isOnline
            set_time_delay_switch!!.isChecked = automatic_lock_dp_value == "true"
        } else {
            set_time_delay_switch!!.isEnabled = false
        }
        set_time_delay_switch!!.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            val data: MutableMap<String?, Any> =
                HashMap()
            data[automatic_lock_dp_id] = isChecked
            publishDps(data)
        }
        if (null != auto_lock_time_dp_id) {
            if (null != deviceBean.getDps()[auto_lock_time_dp_id]) {
                timeInt = deviceBean.getDps()[auto_lock_time_dp_id].toString().toInt()
            }
            val timeValue = timeInt.toString() + "s"
            set_time_delay_select!!.text = timeValue
        } else {
            set_time_delay_select!!.text = "不支持"
        }
        /**
         * chinese_simplified, english, japanese, german, spanish, latin, french, russian, italian, chinese_traditional, korean
         */
        val language_id = LockUtil.convertCode2Id(mDevId, "language")
        if (TextUtils.isEmpty(language_id)) {
            findViewById<View>(R.id.language_select_wrap).visibility = View.GONE
            findViewById<View>(R.id.language_select_line).visibility = View.GONE
        } else {
            findViewById<View>(R.id.language_select_wrap).visibility = View.VISIBLE
            findViewById<View>(R.id.language_select_line).visibility = View.VISIBLE
            val language_value = deviceBean.getDps()[language_id] as String?
            if (TextUtils.equals(language_value, "chinese_simplified")) {
                language_select_group!!.check(R.id.language_china)
            } else {
                language_select_group!!.check(R.id.language_english)
            }
            language_select_group!!.setOnCheckedChangeListener { group: RadioGroup?, checkedId: Int ->
                var languageSelect = "english"
                if (checkedId == R.id.language_china) {
                    languageSelect = "chinese_simplified"
                } else if (checkedId == R.id.language_english) {
                    languageSelect = "english"
                }
                val data: MutableMap<String?, Any> =
                    HashMap()
                data[language_id] = languageSelect
                publishDps(data)
            }
        }
    }

    private val listener: IDevListener = object : IDevListener {
        override fun onDpUpdate(devId: String, dpStr: String) {}
        override fun onRemoved(devId: String) {}
        override fun onStatusChanged(devId: String, online: Boolean) {
            set_time_delay_switch!!.isEnabled = online
            language_select_group!!.isEnabled = online
        }

        override fun onNetworkStatusChanged(devId: String, status: Boolean) {}
        override fun onDevInfoUpdate(devId: String) {}
    }


    private fun publishDps(data: Map<String?, Any>) {
        IThingDevice!!.publishDps(JSONObject.toJSONString(data), object : IResultCallback {
            override fun onError(code: String, message: String) {
                Log.e(
                    Constant.TAG,
                    "publishDps code:$code, message:$message"
                )
            }

            override fun onSuccess() {
                Log.i(Constant.TAG, "publishDps onSuccess")
            }
        })
    }
}