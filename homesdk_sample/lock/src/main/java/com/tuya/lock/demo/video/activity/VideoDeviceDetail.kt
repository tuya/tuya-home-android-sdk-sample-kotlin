package com.tuya.lock.demo.video.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.thingclips.sdk.os.ThingOSDevice
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.sdk.api.IDevListener
import com.thingclips.smart.sdk.api.IThingDevice
import com.thingclips.smart.sdk.optimus.lock.bean.ble.DataPoint
import com.thingclips.smart.sdk.optimus.lock.utils.LockUtil
import com.thingclips.smart.sdk.optimus.lock.utils.StandardDpConverter
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.activity.DeleteDeviceActivity
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.view.LockButtonProgressView
import com.tuya.lock.demo.wifi.activity.GoogleVoiceSettingActivity
import com.tuya.lock.demo.wifi.activity.MemberListActivity
import java.util.Locale

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class VideoDeviceDetail: AppCompatActivity() {
    private var unlock_btn: LockButtonProgressView? = null
    private var closed_door_view: TextView? = null
    private var anti_lock_view: TextView? = null
    private var child_lock_view: TextView? = null
    private var power_view: TextView? = null
    private var door_record_view: TextView? = null
    private var ITuyaDevice: IThingDevice? = null
    private var member_list_view: TextView? = null
    private var mDevId: String? = null

    companion object{
        fun startActivity(context: Context?, devId: String?) {
            val intent = Intent(context, VideoDeviceDetail::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            context?.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_device_detail)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        ITuyaDevice = ThingHomeSdk.newDeviceInstance(mDevId)
        ITuyaDevice!!.registerDevListener(deviceListener)
        initView()
        deviceOnline()
    }

    private val deviceListener: IDevListener = object : IDevListener {
        override fun onDpUpdate(devId: String, dpStr: String) {
            val dpData = StandardDpConverter.convertIdToCodeMap(
                dpStr,
                StandardDpConverter.getSchemaMap(mDevId)
            )
            dealAddUnlockMode(dpData)
        }

        override fun onRemoved(devId: String) {}
        override fun onStatusChanged(devId: String, online: Boolean) {
            deviceOnline()
        }

        override fun onNetworkStatusChanged(devId: String, status: Boolean) {}
        override fun onDevInfoUpdate(devId: String) {}
    }

    private fun dealAddUnlockMode(dpData: Map<String, Any>) {
        for (key in dpData.keys) {
            val lockResponse = dpData[key].toString()
            when (key) {
                "closed_opened" -> checkDoorOpen(lockResponse)
                "reverse_lock" -> checkReverseOpen(java.lang.Boolean.parseBoolean(lockResponse))
                "child_lock" -> checkChildOpen(java.lang.Boolean.parseBoolean(lockResponse))
                "residual_electricity", "battery_state" -> checkResidual(lockResponse)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        initData()
    }

    override fun onDestroy() {
        super.onDestroy()
        ITuyaDevice!!.onDestroy()
    }

    private fun initView() {
        unlock_btn = findViewById(R.id.unlock_btn)
        closed_door_view = findViewById(R.id.closed_door_view)
        anti_lock_view = findViewById(R.id.anti_lock_view)
        child_lock_view = findViewById(R.id.child_lock_view)
        power_view = findViewById(R.id.power_view)
        door_record_view = findViewById(R.id.door_record_view)
        member_list_view = findViewById(R.id.member_list_view)
    }

    private fun initData() {
        val deviceBean = ThingOSDevice.getDeviceBean(mDevId)

        //门是否关闭
        if (null != deviceBean && deviceBean.getDpCodes().containsKey("closed_opened")) {
            val closedCode = deviceBean.getDpCodes()["closed_opened"] as String?
            checkDoorOpen(closedCode)
            closed_door_view!!.visibility = View.VISIBLE
        } else {
            closed_door_view!!.visibility = View.GONE
        }

        //是否反锁
        if (null != deviceBean && deviceBean.getDpCodes().containsKey("reverse_lock")) {
            val reverseCode = deviceBean.getDpCodes()["reverse_lock"] as Boolean?
            checkReverseOpen(reverseCode)
            anti_lock_view!!.visibility = View.VISIBLE
        } else {
            anti_lock_view!!.visibility = View.GONE
        }

        //是否童锁
        if (null != deviceBean && deviceBean.getDpCodes().containsKey("child_lock")) {
            val childCode = deviceBean.getDpCodes()["child_lock"] as Boolean?
            checkChildOpen(childCode)
            child_lock_view!!.visibility = View.VISIBLE
        } else {
            child_lock_view!!.visibility = View.GONE
        }

        //电量
        if (null != deviceBean) {
            if (deviceBean.getDpCodes().containsKey("battery_state")) {
                val state = deviceBean.getDpCodes()["battery_state"].toString()
                checkResidual(state)
            } else if (deviceBean.getDpCodes().containsKey("residual_electricity")) {
                val residualCode = deviceBean.getDpCodes()["residual_electricity"].toString()
                checkResidual(residualCode)
            }
        } else {
            power_view!!.text = getString(R.string.zigbee_battery_not_support)
        }
        /**
         * 开门记录
         */
        door_record_view!!.setOnClickListener { v: View? ->
            LogRecordListActivity.startActivity(
                this,
                mDevId
            )
        }
        /**
         * 家庭成员入口
         */
        member_list_view!!.setOnClickListener {
            //WIFI
            MemberListActivity.startActivity(
                this,
                mDevId
            )
        }
        /**
         * 解绑
         */
        findViewById<View>(R.id.device_delete).setOnClickListener { v: View ->
            DeleteDeviceActivity.startActivity(
                v.context,
                mDevId
            )
        }

        /**
         * 谷歌语音密码
         */
        //校验远程语音是否有对应dp
        val voice_settings_view = findViewById<TextView>(R.id.device_google_password)
        val dpId = LockUtil.convertCode2Id(mDevId, DataPoint.UNLOCK_VOICE_REMOTE)
        if (TextUtils.isEmpty(dpId)) {
            val voiceStr = resources.getString(R.string.voice_set_title) + "(not support)"
            voice_settings_view.text = voiceStr
            voice_settings_view.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        } else {
            voice_settings_view.text = resources.getString(R.string.voice_set_title)
            voice_settings_view.setCompoundDrawablesRelativeWithIntrinsicBounds(
                0,
                0,
                R.drawable.ic_next,
                0
            )
            voice_settings_view.setOnClickListener { v: View ->
                //远程语音设置
                GoogleVoiceSettingActivity.startActivity(
                    this,
                    mDevId
                )
            }
        }

    }

    private fun deviceOnline() {
        val deviceBean = ThingOSDevice.getDeviceBean(mDevId)
        //设备是否在线
        if (deviceBean.isOnline) {
            unlock_btn!!.setTitle(getString(R.string.device_online))
        } else {
            unlock_btn!!.setTitle(getString(R.string.zigbee_device_offline))
        }
        unlock_btn!!.isEnabled = false
    }

    private fun checkDoorOpen(closedOpened: String?) {
        //门是否关闭
        if (TextUtils.equals(closedOpened, "open")) {
            closed_door_view!!.text = getString(R.string.zigbee_door_not_closed)
        } else {
            closed_door_view!!.text = getString(R.string.zigbee_door_is_closed)
        }
    }

    private fun checkReverseOpen(reverseCode: Boolean?) {
        if (reverseCode!!) {
            anti_lock_view!!.text = getString(R.string.zigbee_door_locked)
        } else {
            anti_lock_view!!.text = getString(R.string.zigbee_door_not_locked)
        }
    }

    private fun checkChildOpen(childCode: Boolean?) {
        if (childCode!!) {
            child_lock_view!!.text = getString(R.string.zigbee_child_lock_open)
        } else {
            child_lock_view!!.text = getString(R.string.zigbee_child_lock_off)
        }
    }

    private fun checkResidual(residualCode: String) {
        val residual = String.format(Locale.CHINA, getString(R.string.zigbee_power), residualCode)
        power_view!!.text = residual
    }

    private fun showToast(msg: String) {
        runOnUiThread {
            Toast.makeText(
                this@VideoDeviceDetail,
                msg,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}