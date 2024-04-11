package com.tuya.lock.demo.zigbee.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson.JSONArray
import com.thingclips.smart.android.common.utils.L
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.lock.api.IThingZigBeeLock
import com.thingclips.smart.optimus.lock.api.zigbee.response.MemberInfoBean
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.api.IThingDataCallback
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.zigbee.adapter.MemberSelectListAdapter

/**
 *
 * Created by HuiYao on 2024/3/1
 */
class MemberSelectListActivity: AppCompatActivity() {

    private var iTuyaZigBeeLock: IThingZigBeeLock? = null
    private val adapter: MemberSelectListAdapter by lazy {
        MemberSelectListAdapter()
    }
    private var unlockIds: List<String>? = null
    private var errorView: TextView? = null
    private var from = 0 //0：未关联成员，1：记录列表进来


    companion object{
        fun startActivity(context: Context, devId: String?, unlockIds: List<String?>?, from: Int) {
            val intent = Intent(context, MemberSelectListActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            intent.putExtra(Constant.UNLOCK_INFO_BEAN, JSONArray.toJSONString(unlockIds))
            intent.putExtra(Constant.FROM, from)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zigbee_member_select_list)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        from = intent.getIntExtra(Constant.FROM, 0)
        val mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        val allocString = intent.getStringExtra(Constant.UNLOCK_INFO_BEAN)
        unlockIds = JSONArray.parseArray(allocString, String::class.java)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        iTuyaZigBeeLock = tuyaLockManager.getZigBeeLock(mDevId)
        errorView = findViewById(R.id.error_view)
        val rvList = findViewById<RecyclerView>(R.id.mode_list)
        rvList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        adapter.setAlloc(object : MemberSelectListAdapter.Callback {
            override fun alloc(infoBean: MemberInfoBean?, position: Int) {
                allocProUnlockOpMode(infoBean)
            }
        })
        rvList.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        getProLockMemberList()
    }

    private fun getProLockMemberList() {
        iTuyaZigBeeLock!!.getMemberList(object : IThingDataCallback<ArrayList<MemberInfoBean>> {
            override fun onSuccess(result: ArrayList<MemberInfoBean>) {
                adapter.setData(result)
                adapter.notifyDataSetChanged()
            }

            override fun onError(code: String, message: String) {
                L.e(
                    Constant.TAG,
                    "getProLockMemberList failed: code = $code  message = $message"
                )
                val errorString = "$message($code)"
                errorView!!.visibility = View.VISIBLE
                errorView!!.text = errorString
            }
        })
    }

    private fun allocProUnlockOpMode(memberInfoBean: MemberInfoBean?) {
        if (unlockIds!!.isEmpty()) {
            Toast.makeText(
                applicationContext,
                resources.getString(R.string.no_unlock_mode_selected),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (from == 1) {
            iTuyaZigBeeLock!!.bindOpModeToMember(
                memberInfoBean?.userId,
                unlockIds,
                object : IThingResultCallback<Boolean?> {
                    override fun onSuccess(result: Boolean?) {
                        Toast.makeText(applicationContext, "alloc onSuccess", Toast.LENGTH_SHORT)
                            .show()
                        finish()
                    }

                    override fun onError(code: String, message: String) {
                        L.e(
                            Constant.TAG,
                            "allocProUnlockOpMode failed: code = $code  message = $message"
                        )
                        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                    }
                })
            return
        }
        iTuyaZigBeeLock!!.allocUnlockOpMode(
            memberInfoBean?.userId,
            unlockIds,
            object : IThingResultCallback<Boolean?> {
                override fun onSuccess(result: Boolean?) {
                    Toast.makeText(applicationContext, "alloc onSuccess", Toast.LENGTH_SHORT).show()
                    finish()
                }

                override fun onError(code: String, message: String) {
                    L.e(
                        Constant.TAG,
                        "allocProUnlockOpMode failed: code = $code  message = $message"
                    )
                    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                }
            })
    }
}