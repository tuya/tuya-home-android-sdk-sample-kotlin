package com.tuya.lock.demo.zigbee.activity

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
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.lock.api.IThingZigBeeLock
import com.thingclips.smart.optimus.lock.api.zigbee.response.MemberInfoBean
import com.thingclips.smart.optimus.lock.api.zigbee.response.UnAllocOpModeBean
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.api.IResultCallback
import com.thingclips.smart.sdk.api.IThingDataCallback
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.DialogUtils
import com.tuya.lock.demo.zigbee.adapter.MemberListAdapter

/**
 *
 * Created by HuiYao on 2024/3/1
 */
class MemberListActivity:AppCompatActivity() {

    private var zigBeeLock: IThingZigBeeLock? = null
    private val adapter: MemberListAdapter by lazy {
        MemberListAdapter()
    }
    private var mCurrentBean: MemberInfoBean? = null
    private var is_need_alloc_unlock_view: TextView? = null

    companion object{
        fun startActivity(context: Context, devId: String?) {
            val intent = Intent(context, MemberListActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zigbee_member_list)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        val mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        zigBeeLock = tuyaLockManager.getZigBeeLock(mDevId)
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
        is_need_alloc_unlock_view = findViewById(R.id.is_need_alloc_unlock)
        is_need_alloc_unlock_view?.setOnClickListener { v: View ->
            //未分配解锁方式列表
            OpModeUnboundListActivity.startActivity(v.context, mDevId)
        }
        adapter.setDevId(mDevId)
        adapter.deleteUser(object : MemberListAdapter.Callback{
            override fun remove(infoBean: MemberInfoBean, position: Int) {
                if (mCurrentBean!!.userType == 10 || mCurrentBean!!.userType == 50) {
                    DialogUtils.showDelete(
                        this@MemberListActivity
                    ) { _, _ -> deleteLockUser(infoBean, position) }
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
    }

    /**
     * 获取是否有未分配的解锁方式
     */
    private fun isNeedAllocUnlock() {
        zigBeeLock!!.getUnAllocOpMode(object : IThingDataCallback<ArrayList<UnAllocOpModeBean?>?> {
            override fun onSuccess(result: ArrayList<UnAllocOpModeBean?>?) {
                if (null != result && result.size > 0) {
                    val isNeedAllocTitle = resources.getString(R.string.lock_opMode_unassigned)
                    is_need_alloc_unlock_view!!.text = isNeedAllocTitle
                    is_need_alloc_unlock_view!!.visibility = View.VISIBLE
                    findViewById<View>(R.id.is_need_alloc_unlock_line).visibility = View.VISIBLE
                } else {
                    is_need_alloc_unlock_view!!.visibility = View.GONE
                    findViewById<View>(R.id.is_need_alloc_unlock_line).visibility = View.GONE
                }
            }

            override fun onError(code: String, message: String) {
                is_need_alloc_unlock_view!!.visibility = View.GONE
                findViewById<View>(R.id.is_need_alloc_unlock_line).visibility = View.GONE
                Log.e(
                    Constant.TAG,
                    "isProNeedAllocUnlockOpMode failed: code = $code  message = $message"
                )
            }
        })
    }

    /**
     * 获取门锁成员
     */
    private fun getLockUser() {
        zigBeeLock?.getMemberList(object : IThingDataCallback<ArrayList<MemberInfoBean>> {
            override fun onSuccess(result: ArrayList<MemberInfoBean>) {
                Log.i(
                    Constant.TAG,
                    "getMemberList success: lockUserBean = " + JSONArray.toJSONString(result)
                )
                adapter.setData(result)
                adapter.notifyDataSetChanged()
            }

            override fun onError(code: String, message: String) {
                runOnUiThread {
                    Log.e(
                        Constant.TAG,
                        "getMemberList failed: code = $code  message = $message"
                    )
                    Toast.makeText(
                        applicationContext,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
        zigBeeLock?.getMemberInfo(object : IThingDataCallback<MemberInfoBean?> {
            override fun onSuccess(currentBean: MemberInfoBean?) {
                mCurrentBean = currentBean
            }

            override fun onError(code: String, message: String) {
                runOnUiThread {
                    Log.e(
                        Constant.TAG,
                        "getMemberList failed: code = $code  message = $message"
                    )
                    Toast.makeText(
                        applicationContext,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    /**
     * 删除成员
     */
    private fun deleteLockUser(infoBean: MemberInfoBean, position: Int) {
        zigBeeLock!!.removeMember(infoBean, object : IResultCallback {
            override fun onError(code: String, error: String) {
                runOnUiThread {
                    Log.e(
                        Constant.TAG,
                        "delete lock user failed: code = $code  message = $error"
                    )
                    Toast.makeText(
                        applicationContext,
                        error,
                        Toast.LENGTH_SHORT
                    ).show()
                }
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