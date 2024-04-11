package com.tuya.lock.demo.ble.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class PasswordMainActivity : AppCompatActivity() {

    companion object {
        fun startActivity(context: Context?, devId: String?) {
            val intent = Intent(context, PasswordMainActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            context?.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_main)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        val deviceId = intent.getStringExtra(Constant.DEVICE_ID)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        val isProDevice = tuyaLockManager.getBleLockV2(deviceId).isProDevice
        findViewById<View>(R.id.password_dynamic).setOnClickListener { v: View ->
            //动态密码
            PasswordDynamicActivity.startActivity(v.context, deviceId)
        }
        findViewById<View>(R.id.unassigned_list).setOnClickListener { v: View ->
            //获取可分配的离线不限次数密码列表
            PasswordSingleRevokeListActivity.startActivity(v.context, deviceId)
        }
        findViewById<View>(R.id.lock_device_config).setOnClickListener { v: View ->
            //设备信息
            PasswordLockDeviceConfigActivity.startActivity(v.context, deviceId)
        }
        val lock_pro_password_list = findViewById<View>(R.id.lock_pro_password_list)
        val online_list_single_wrap = findViewById<View>(R.id.online_list_single_wrap)
        //单次在线密码
        online_list_single_wrap.setOnClickListener { v: View ->
            PasswordOldOnlineListActivity.startActivity(v.context, deviceId, Constant.TYPE_SINGLE)
        }
        if (isProDevice) {
            online_list_single_wrap.visibility = View.GONE
            lock_pro_password_list.visibility = View.VISIBLE
        } else {
            online_list_single_wrap.visibility = View.VISIBLE
            lock_pro_password_list.visibility = View.GONE
        }
        lock_pro_password_list.setOnClickListener { v: View ->
            PasswordProListActivity.startActivity(v.context, deviceId)
        }

        //多次在线密码
        val online_list_multiple = findViewById<TextView>(R.id.online_list_multiple)
        findViewById<View>(R.id.online_list_multiple_wrap).setOnClickListener { v: View ->
            if (isProDevice) {
                PasswordProOnlineDetailActivity.startActivity(v.context, null, deviceId, 0)
            } else {
                PasswordOldOnlineListActivity.startActivity(
                    v.context,
                    deviceId,
                    Constant.TYPE_MULTIPLE
                )
            }
        }
        if (isProDevice) {
            online_list_multiple.text = resources.getString(R.string.password_custom)
        } else {
            online_list_multiple.text = resources.getString(R.string.password_periodic)
        }

        //离线单次密码
        findViewById<View>(R.id.offline_list_single_wrap).setOnClickListener { v: View ->
            if (isProDevice) {
                PasswordProOfflineAddActivity.startActivity(
                    v.context,
                    deviceId,
                    Constant.TYPE_SINGLE
                )
            } else {
                PasswordOldOfflineListActivity.startActivity(
                    v.context,
                    deviceId,
                    Constant.TYPE_SINGLE
                )
            }
        }

        //离线不限次数密码
        findViewById<View>(R.id.offline_list_multiple_wrap).setOnClickListener { v: View ->
            if (isProDevice) {
                PasswordProOfflineAddActivity.startActivity(
                    v.context,
                    deviceId,
                    Constant.TYPE_MULTIPLE
                )
            } else {
                PasswordOldOfflineListActivity.startActivity(
                    v.context,
                    deviceId,
                    Constant.TYPE_MULTIPLE
                )
            }
        }

        //全部清空码
        findViewById<View>(R.id.offline_list_clear_wrap).setOnClickListener { v: View ->
            if (isProDevice) {
                PasswordProOfflineAddActivity.startActivity(
                    v.context,
                    deviceId,
                    Constant.TYPE_CLEAR_ALL
                )
            } else {
                PasswordOldOfflineListActivity.startActivity(
                    v.context,
                    deviceId,
                    Constant.TYPE_CLEAR_ALL
                )
            }
        }
    }

}