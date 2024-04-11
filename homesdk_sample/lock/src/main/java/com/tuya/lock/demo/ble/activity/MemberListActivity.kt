package com.tuya.lock.demo.ble.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson.JSONArray
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingBleLockV2
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.api.IResultCallback
import com.thingclips.smart.sdk.optimus.lock.bean.ble.MemberInfoBean
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.adapter.MemberListAdapter
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.DialogUtils

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class MemberListActivity: AppCompatActivity() {

    private var tuyaLockDevice: IThingBleLockV2? = null
    private val adapter: MemberListAdapter by lazy {
        MemberListAdapter()
    }
    private var is_need_alloc_unlock_view: TextView? = null

    companion object {
        fun startActivity(context: Context?, devId: String?) {
            val intent = Intent(context, MemberListActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            context?.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_list)

        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }

        val mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        tuyaLockDevice = tuyaLockManager.getBleLockV2(mDevId)

        findViewById<View>(R.id.user_add).setOnClickListener { v: View ->
            MemberDetailActivity.startActivity(
                v.context,
                null,
                mDevId,
                0
            )
        }

        val rvList = findViewById<RecyclerView>(R.id.user_list)
        rvList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        is_need_alloc_unlock_view = findViewById(R.id.is_need_alloc_unlock)
        is_need_alloc_unlock_view?.setOnClickListener { v: View ->
            OpModeUnboundListActivity.startActivity(
                v.context,
                mDevId
            )
        }

        adapter.setDevId(mDevId)
        adapter.setProDevice(tuyaLockDevice!!.isProDevice)
        adapter.deleteUser(object : MemberListAdapter.Callback {
            override fun remove(infoBean: MemberInfoBean?, position: Int) {
                DialogUtils.showDelete(this@MemberListActivity) { _, _ ->
                    deleteLockUser(
                        infoBean,
                        position
                    )
                }
            }
        })
        rvList.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        getLockUser()
        isNeedAllocUnlock()
    }

    override fun onDestroy() {
        super.onDestroy()
        tuyaLockDevice!!.onDestroy()
    }

    /**
     * 获取是否有未分配的解锁方式
     */
    private fun isNeedAllocUnlock() {
        tuyaLockDevice!!.isProNeedAllocUnlockOpMode(object : IThingResultCallback<Boolean> {
            override fun onSuccess(result: Boolean) {
                val isNeedAllocTitle =
                    resources.getString(R.string.lock_opMode_unassigned) + ": " + result
                is_need_alloc_unlock_view!!.text = isNeedAllocTitle
            }

            override fun onError(code: String, message: String) {
                Log.e(
                    Constant.TAG,
                    "isProNeedAllocUnlockOpMode failed: code = $code  message = $message"
                )
                //                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            }
        })
    }

    /**
     * 获取门锁成员
     */
    private fun getLockUser() {
        tuyaLockDevice!!.getProLockMemberList(object :
            IThingResultCallback<ArrayList<MemberInfoBean>?> {
            override fun onSuccess(result: ArrayList<MemberInfoBean>?) {
                Log.i(
                    Constant.TAG,
                    "getProLockMemberList success: lockUserBean = " + JSONArray.toJSONString(result)
                )
                adapter.setData(result!!.toMutableList())
                adapter.notifyDataSetChanged()
            }

            override fun onError(code: String, message: String) {
                Log.e(
                    Constant.TAG,
                    "getProLockMemberList failed: code = $code  message = $message"
                )
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    /**
     * 删除成员
     */
    private fun deleteLockUser(infoBean: MemberInfoBean?, position: Int) {
        tuyaLockDevice!!.removeProLockMember(infoBean, object : IResultCallback {
            override fun onError(code: String, error: String) {
                Log.e(
                    Constant.TAG,
                    "delete lock user failed: code = $code  message = $error"
                )
                Toast.makeText(applicationContext, error, Toast.LENGTH_SHORT).show()
            }

            override fun onSuccess() {
                Log.i(Constant.TAG, "delete lock user success")
                Toast.makeText(applicationContext, "delete lock user success", Toast.LENGTH_SHORT)
                    .show()
                adapter.remove(position)
            }
        })
    }
}