package com.tuya.lock.demo.wifi.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson.JSONArray
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.lock.api.IThingWifiLock
import com.thingclips.smart.optimus.lock.api.bean.WifiLockUser
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.DialogUtils
import com.tuya.lock.demo.wifi.adapter.MemberListAdapter

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class MemberListActivity : AppCompatActivity() {

    private var wifiLock: IThingWifiLock? = null
    private val adapter: MemberListAdapter by lazy {
        MemberListAdapter()
    }

    companion object {
        fun startActivity(context: Context, devId: String?) {
            val intent = Intent(context, MemberListActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_member_list)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        val mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        wifiLock = tuyaLockManager.getWifiLock(mDevId)
        findViewById<View>(R.id.user_add).setOnClickListener { v: View ->
            //添加成员
            MemberDetailActivity.startActivity(
                v.context,
                null,
                mDevId,
                0
            )
        }
        val rvList = findViewById<RecyclerView>(R.id.user_list)
        rvList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        adapter.setDevId(mDevId)
        adapter.deleteUser(object : MemberListAdapter.Callback {
            override fun remove(infoBean: WifiLockUser?, position: Int) {
                DialogUtils.showDelete(
                    this@MemberListActivity
                ) { _, _ -> deleteLockUser(infoBean!!.userId, position) }
            }

        })
        rvList.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        getLockUser()
    }

    override fun onDestroy() {
        super.onDestroy()
        wifiLock!!.onDestroy()
    }

    /**
     * 获取门锁成员
     */
    private fun getLockUser() {
        wifiLock!!.getLockUsers(object : IThingResultCallback<List<WifiLockUser>> {
            override fun onSuccess(result: List<WifiLockUser>) {
                Log.i(
                    Constant.TAG,
                    "getMemberList success: lockUserBean = " + JSONArray.toJSONString(result)
                )
                adapter.setData(result)
                adapter.notifyDataSetChanged()
            }

            override fun onError(code: String, message: String) {
                Log.e(
                    Constant.TAG,
                    "getMemberList failed: code = $code  message = $message"
                )
                showToast(message)
            }
        })
    }

    /**
     * 删除成员
     */
    private fun deleteLockUser(userId: String, position: Int) {
        wifiLock!!.deleteLockUser(userId, object : IThingResultCallback<Boolean?> {
            override fun onSuccess(result: Boolean?) {
                Log.i(Constant.TAG, "delete lock user success")
                showToast("delete lock user success")
                adapter.remove(position)
            }

            override fun onError(code: String, error: String) {
                Log.e(
                    Constant.TAG,
                    "delete lock user failed: code = $code  message = $error"
                )
                showToast(error)
            }
        })
    }

    private fun showToast(msg: String) {
        runOnUiThread {
            Toast.makeText(
                applicationContext,
                msg,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

}