package com.tuya.lock.demo.zigbee.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListPopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.lock.api.IThingZigBeeLock
import com.thingclips.smart.optimus.lock.api.zigbee.request.RemotePermissionEnum
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.api.IResultCallback
import com.thingclips.smart.sdk.optimus.lock.bean.ZigBeeDatePoint
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant

/**
 *
 * Created by HuiYao on 2024/3/1
 */
class SettingActivity : AppCompatActivity() {
    private var zigBeeLock: IThingZigBeeLock? = null
    private var mDevId: String? = null
    private var remote_set_state: TextView? = null
    private var voice_set_state: TextView? = null
    private var remote_permissions_state: TextView? = null
    private var remote_permissions_view: LinearLayout? = null
    private var listPopupWindow: ListPopupWindow? = null

    companion object {
        fun startActivity(context: Context, devId: String?) {
            val intent = Intent(context, SettingActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zigbee_setting)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        zigBeeLock = tuyaLockManager.getZigBeeLock(mDevId)
        remote_set_state = findViewById(R.id.remote_set_state)
        voice_set_state = findViewById(R.id.voice_set_state)
        remote_permissions_state = findViewById(R.id.remote_permissions_state)
        remote_permissions_view = findViewById(R.id.remote_permissions_view)
        findViewById<View>(R.id.remote_set_view).setOnClickListener { v: View? ->
            LockSettingActivity.startActivity(
                this@SettingActivity,
                mDevId
            )
        }
        findViewById<View>(R.id.voice_settings).setOnClickListener { v: View? ->
            VoiceSettingActivity.startActivity(
                this@SettingActivity,
                mDevId
            )
        }
        if (!TextUtils.isEmpty(zigBeeLock?.convertCode2Id(ZigBeeDatePoint.REMOTE_UNLOCK)) ||
            !TextUtils.isEmpty(zigBeeLock?.convertCode2Id(ZigBeeDatePoint.REMOTE_NO_DP_KEY))
        ) {
            findViewById<View>(R.id.remote_set_view).visibility = View.VISIBLE
        } else {
            findViewById<View>(R.id.remote_set_view).visibility = View.GONE
        }
        if (TextUtils.isEmpty(zigBeeLock?.convertCode2Id(ZigBeeDatePoint.UNLOCK_VOICE_REMOTE))) {
            findViewById<View>(R.id.voice_settings).visibility = View.GONE
            findViewById<View>(R.id.voice_settings_line).visibility = View.GONE
        } else {
            findViewById<View>(R.id.voice_settings).visibility = View.VISIBLE
            findViewById<View>(R.id.voice_settings_line).visibility = View.VISIBLE
        }
        setRemotePermissionsUi(true)
        remote_permissions_view?.setOnClickListener {
            if (null != listPopupWindow) {
                listPopupWindow!!.show()
            }
        }
    }

    private fun setRemotePermissionsUi(isShow: Boolean) {
        if (isShow && !TextUtils.isEmpty(zigBeeLock!!.convertCode2Id(ZigBeeDatePoint.REMOTE_UNLOCK))) {
            remote_permissions_view!!.visibility = View.VISIBLE
            findViewById<View>(R.id.remote_permissions_line).visibility = View.VISIBLE
        } else {
            remote_permissions_view!!.visibility = View.GONE
            findViewById<View>(R.id.remote_permissions_line).visibility = View.GONE
        }
    }

    private fun setVoiceUnlockUi(isShow: Boolean) {
        if (isShow) {
            findViewById<View>(R.id.voice_settings).visibility = View.VISIBLE
            findViewById<View>(R.id.voice_settings_line).visibility = View.VISIBLE
        } else {
            findViewById<View>(R.id.voice_settings).visibility = View.GONE
            findViewById<View>(R.id.voice_settings_line).visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        isRemoteUnlockAvailable()
    }

    private fun isRemoteUnlockAvailable() {
        zigBeeLock!!.fetchRemoteUnlockType(object : IThingResultCallback<Boolean> {
            override fun onSuccess(result: Boolean) {
                Log.i(Constant.TAG, "get remote unlock available success:$result")
                remote_set_state!!.post {
                    if (result) {
                        remote_set_state!!.text = getString(R.string.set_voice_password_open)
                        getRemoteUnlockPermissionValue()
                        fetchRemoteVoiceUnlock()
                    } else {
                        remote_set_state!!.text = getString(R.string.set_voice_password_close)
                    }
                    setRemotePermissionsUi(result)
                    setVoiceUnlockUi(result)
                }
            }

            override fun onError(code: String, message: String) {
                Log.e(
                    Constant.TAG,
                    "get remote unlock available failed: code = $code  message = $message"
                )
            }
        })
    }

    private fun fetchRemoteVoiceUnlock() {
        if (TextUtils.isEmpty(zigBeeLock!!.convertCode2Id(ZigBeeDatePoint.UNLOCK_VOICE_REMOTE))) {
            Log.e(Constant.TAG, "fetchRemoteVoiceUnlock UNLOCK_VOICE_REMOTE is not")
            return
        }
        zigBeeLock!!.fetchRemoteVoiceUnlock(object : IThingResultCallback<Boolean> {
            override fun onSuccess(result: Boolean) {
                Log.i(Constant.TAG, "fetchRemoteVoiceUnlock success:$result")
                voice_set_state!!.post {
                    if (result) {
                        voice_set_state!!.text = getString(R.string.set_voice_password_open)
                    } else {
                        voice_set_state!!.text = getString(R.string.set_voice_password_close)
                    }
                }
            }

            override fun onError(code: String, message: String) {
                Log.e(
                    Constant.TAG,
                    "fetchRemoteVoiceUnlock failed: code = $code  message = $message"
                )
            }
        })
    }

    private fun getRemoteUnlockPermissionValue() {
        zigBeeLock!!.getRemoteUnlockPermissionValue(object :
            IThingResultCallback<RemotePermissionEnum?> {
            override fun onSuccess(result: RemotePermissionEnum?) {
                var remotePermissionsStr = ""
                when (result) {
                    RemotePermissionEnum.REMOTE_UNLOCK_ALL -> remotePermissionsStr =
                        getString(R.string.zigbee_remote_unlock_all)

                    RemotePermissionEnum.REMOTE_UNLOCK_ADMIN -> remotePermissionsStr =
                        getString(R.string.zigbee_remote_unlock_admin)

                    RemotePermissionEnum.REMOTE_NOT_DP_KEY_ALL -> remotePermissionsStr =
                        getString(R.string.zigbee_remote_not_key_all)

                    RemotePermissionEnum.REMOTE_NOT_DP_KEY_ADMIN -> remotePermissionsStr =
                        getString(R.string.zigbee_remote_not_key_admin)

                    else -> {}
                }
                remote_permissions_state!!.text = remotePermissionsStr
                showPupList()
            }

            override fun onError(errorCode: String, errorMessage: String) {}
        })
    }

    private fun showPupList() {
        listPopupWindow = ListPopupWindow(this@SettingActivity)
        listPopupWindow!!.anchorView = remote_permissions_view
        val items: MutableList<String> = ArrayList()
        items.add(getString(R.string.zigbee_remote_not_key_admin))
        items.add(getString(R.string.zigbee_remote_not_key_all))
        items.add(getString(R.string.zigbee_remote_unlock_admin))
        items.add(getString(R.string.zigbee_remote_unlock_all))
        val adapter = ArrayAdapter(
            this@SettingActivity,
            R.layout.device_zigbee_dp_enum_popup_item,
            items
        )
        listPopupWindow!!.setAdapter(adapter)
        listPopupWindow!!.setOnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            when (position) {
                0 -> {
                    setRemoteOpenState(RemotePermissionEnum.REMOTE_NOT_DP_KEY_ADMIN)
                }
                1 -> {
                    setRemoteOpenState(RemotePermissionEnum.REMOTE_NOT_DP_KEY_ALL)
                }
                2 -> {
                    setRemoteOpenState(RemotePermissionEnum.REMOTE_UNLOCK_ADMIN)
                }
                3 -> {
                    setRemoteOpenState(RemotePermissionEnum.REMOTE_UNLOCK_ALL)
                }
            }
            listPopupWindow!!.dismiss()
        }
    }

    private fun setRemoteOpenState(permissionEnum: RemotePermissionEnum) {
        zigBeeLock!!.setRemoteUnlockPermissionValue(permissionEnum, object : IResultCallback {
            override fun onError(code: String, error: String) {
                runOnUiThread {
                    Toast.makeText(
                        this@SettingActivity,
                        error,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onSuccess() {
                getRemoteUnlockPermissionValue()
            }
        })
    }
}