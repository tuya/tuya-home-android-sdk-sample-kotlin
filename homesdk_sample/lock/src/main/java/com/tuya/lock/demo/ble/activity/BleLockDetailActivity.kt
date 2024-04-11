package com.tuya.lock.demo.ble.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingBleLockV2
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.lock.api.callback.ConnectV2Listener
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.api.IDevListener
import com.thingclips.smart.sdk.api.IResultCallback
import com.thingclips.smart.sdk.api.IThingDevice
import com.thingclips.smart.sdk.optimus.lock.bean.ble.BLELockUser
import com.thingclips.smart.sdk.optimus.lock.bean.ble.DataPoint
import com.thingclips.smart.sdk.optimus.lock.utils.LockUtil
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.CopyLinkTextHelper

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class BleLockDetailActivity : AppCompatActivity() {

    private var IThingDevice: IThingDevice? = null
    private var device_state_view: TextView? = null
    private var tuyaLockDevice: IThingBleLockV2? = null
    private var device_connect_btn: Button? = null
    private var isPublishSync = false
    private var deviceOnlineState = 0
    private var btn_unlock: Button? = null

    private val tuyaLockManager: IThingLockManager by lazy {
        ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
    }

    companion object {
        fun startActivity(context: Context?, devId: String?) {
            val intent = Intent(context, BleLockDetailActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            context?.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_ble_detail)

        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val deviceId = intent.getStringExtra(Constant.DEVICE_ID)

        tuyaLockDevice = tuyaLockManager.getBleLockV2(deviceId)
        tuyaLockDevice?.publishSyncBatchData()
        val isProDevice = tuyaLockDevice?.isProDevice


        IThingDevice = ThingHomeSdk.newDeviceInstance(deviceId)
        IThingDevice?.registerDevListener(listener)

        val deviceIdName = "device ID: $deviceId"
        findViewById<TextView>(R.id.device_info_view).text = deviceIdName

        val deviceBean = ThingHomeSdk.getDataInstance().getDeviceBean(deviceId)
        if (null != deviceBean) {
            toolbar.title = deviceBean.getName()
        }

        btn_unlock = findViewById(R.id.btn_unlock)

        device_state_view = findViewById(R.id.device_state_view)
        device_connect_btn = findViewById(R.id.device_connect_btn)
        device_connect_btn?.setOnClickListener { v: View ->
            val connectIng =
                resources.getString(R.string.submit_connect) + "..."
            device_connect_btn?.text = connectIng
            device_connect_btn?.isEnabled = false
            tuyaLockDevice?.autoConnect(object : ConnectV2Listener {
                override fun onStatusChanged(online: Boolean) {
                    device_connect_btn?.visibility = View.GONE
                    device_connect_btn?.isEnabled = true
                    Toast.makeText(
                        v.context,
                        "connect success",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onError(code: String, error: String) {
                    device_connect_btn?.isEnabled = true
                    device_connect_btn?.text = resources.getString(R.string.submit_connect)
                    Toast.makeText(
                        v.context,
                        error,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
        showState()
        findViewById<View>(R.id.device_state_layout).setOnClickListener { v: View ->
            //蓝牙连接
            ConnectStateActivity.startActivity(v.context, deviceId)
        }

        findViewById<View>(R.id.btn_get_device_info).setOnClickListener { v: View ->
            CopyLinkTextHelper.copyText(v.context, deviceId)
            Toast.makeText(
                v.context,
                "copy success",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<View>(R.id.door_lock_member).setOnClickListener { v: View ->
            //成员管理
            MemberListActivity.startActivity(v.context, deviceId)
        }

        findViewById<View>(R.id.ble_unlock_and_lock).setOnClickListener { v: View ->
            //蓝牙解锁和落锁
            BleSwitchLockActivity.startActivity(v.context, deviceId)
        }

        findViewById<View>(R.id.lock_record_list).setOnClickListener { v: View ->
            if (isProDevice == true) {
                //门锁记录 pro
                BleLockProRecordsActivity.startActivity(v.context, deviceId)
            } else {
                //门锁记录 老版本
                BleLockRecordsActivity.startActivity(v.context, deviceId)
            }
        }

        findViewById<View>(R.id.unlock_mode_management).setOnClickListener { v: View ->
            //解锁方式管理
            val intent =
                Intent(v.context, OpModeListActivity::class.java)
            intent.putExtra(Constant.DEVICE_ID, deviceId)
            v.context.startActivity(intent)
        }

        findViewById<View>(R.id.password_management).setOnClickListener { v: View ->
            //临时密码
            PasswordMainActivity.startActivity(v.context, deviceId)
        }


        //校验远程语音是否有对应dp
        val voice_settings_view = findViewById<TextView>(R.id.voice_settings)
        val dpId = LockUtil.convertCode2Id(deviceId, DataPoint.UNLOCK_VOICE_REMOTE)
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
                VoiceSettingActivity.startActivity(v.context, deviceId)
            }
        }

        //校验远程开关锁dp是否存在
        val door_lock_settings = findViewById<TextView>(R.id.door_lock_settings)
        val remote_dpId = LockUtil.convertCode2Id(deviceId, DataPoint.REMOTE_NO_DP_KEY)
        if (TextUtils.isEmpty(remote_dpId)) {
            val doorStr = resources.getString(R.string.lock_remote_set) + "(not support)"
            door_lock_settings.text = doorStr
            door_lock_settings.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
        } else {
            door_lock_settings.text = resources.getString(R.string.lock_remote_set)
            door_lock_settings.setCompoundDrawablesRelativeWithIntrinsicBounds(
                0,
                0,
                R.drawable.ic_next,
                0
            )
            door_lock_settings.setOnClickListener { v: View ->
                //远程开锁设置
                LockSettingActivity.startActivity(v.context, deviceId)
            }
        }

        findViewById<View>(R.id.device_delete).setOnClickListener { v: View ->
            DeleteDeviceActivity.startActivity(
                v.context,
                deviceId
            )
        }

        findViewById<View>(R.id.open_dp_demo).setOnClickListener { v: View ->
            DpSettingActivity.startActivity(
                v.context,
                deviceId
            )
        }

        if (null != deviceBean && deviceBean.isOnline) {
            publishSyncBatchData()
        }

        btn_unlock?.setOnClickListener {
            btn_unlock?.isEnabled = false
            if (deviceOnlineState == 1) {
                bleUnlock()
            } else if (deviceOnlineState == 2) {
                farUnlock()
            }
        }
    }

    private val listener: IDevListener = object : IDevListener {
        override fun onDpUpdate(devId: String, dpStr: String) {}
        override fun onRemoved(devId: String) {}
        override fun onStatusChanged(devId: String, online: Boolean) {
            if (online) {
                publishSyncBatchData()
            }
            showState()
        }

        override fun onNetworkStatusChanged(devId: String, status: Boolean) {}
        override fun onDevInfoUpdate(devId: String) {}
    }

    private fun publishSyncBatchData() {
        if (isPublishSync) {
            return
        }
        isPublishSync = true
        tuyaLockDevice!!.publishSyncBatchData()
    }

    private fun showState() {
        val isBLEConnected = tuyaLockDevice!!.isBLEConnected
        val isOnline = tuyaLockDevice!!.isOnline
        btn_unlock!!.isEnabled = isOnline
        if (!isBLEConnected && isOnline) {
            device_state_view!!.text = resources.getString(R.string.connected_gateway)
            device_connect_btn!!.visibility = View.GONE
            deviceOnlineState = 2
        } else if (isBLEConnected && isOnline) {
            device_state_view!!.text = resources.getString(R.string.connected_bluetooth)
            device_connect_btn!!.visibility = View.GONE
            deviceOnlineState = 1
        } else {
            device_state_view!!.text = resources.getString(R.string.device_offline)
            device_connect_btn!!.visibility = View.VISIBLE
            deviceOnlineState = 0
        }
    }

    override fun onResume() {
        super.onResume()
        device_connect_btn!!.text = resources.getString(R.string.submit_connect)
        device_connect_btn!!.isEnabled = true
    }

    override fun onDestroy() {
        super.onDestroy()
        IThingDevice!!.unRegisterDevListener()
        IThingDevice!!.onDestroy()
    }

    private fun bleUnlock() {
        tuyaLockDevice!!.getCurrentMemberDetail(object : IThingResultCallback<BLELockUser> {
            override fun onSuccess(result: BLELockUser) {
                Log.i(Constant.TAG, "getCurrentUser:" + JSONObject.toJSONString(result))
                tuyaLockDevice!!.bleUnlock(result.lockUserId, object : IResultCallback {
                    override fun onError(code: String, error: String) {
                        Log.i(
                            Constant.TAG,
                            "bleUnlock onError code:$code, error:$error"
                        )
                        Toast.makeText(applicationContext, error, Toast.LENGTH_SHORT).show()
                        btn_unlock!!.isEnabled = true
                    }

                    override fun onSuccess() {
                        Toast.makeText(applicationContext, "unlock success", Toast.LENGTH_SHORT)
                            .show()
                        btn_unlock!!.isEnabled = true
                    }
                })
            }

            override fun onError(code: String, error: String) {
                Log.e(
                    Constant.TAG,
                    "getCurrentUser onError code:$code, error:$error"
                )
                Toast.makeText(applicationContext, error, Toast.LENGTH_SHORT).show()
                btn_unlock!!.isEnabled = true
            }
        })
    }

    private fun farUnlock() {
        Log.i(Constant.TAG, "remoteSwitchLock")
        tuyaLockDevice!!.remoteSwitchLock(true, object : IResultCallback {
            override fun onError(code: String, error: String) {
                Log.e(
                    Constant.TAG,
                    "remoteSwitchLock unlock onError code:$code, error:$error"
                )
                Toast.makeText(applicationContext, error, Toast.LENGTH_SHORT).show()
                btn_unlock!!.isEnabled = true
            }

            override fun onSuccess() {
                Toast.makeText(applicationContext, "remote unlock success", Toast.LENGTH_SHORT)
                    .show()
                btn_unlock!!.isEnabled = true
            }
        })
    }

}