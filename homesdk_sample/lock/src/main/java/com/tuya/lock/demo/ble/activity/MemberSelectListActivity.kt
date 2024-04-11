package com.tuya.lock.demo.ble.activity

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
import com.thingclips.smart.optimus.lock.api.IThingBleLockV2
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.optimus.lock.bean.ble.MemberInfoBean
import com.thingclips.smart.sdk.optimus.lock.bean.ble.UnlockInfoBean
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.adapter.MemberSelectListAdapter
import com.tuya.lock.demo.common.constant.Constant

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class MemberSelectListActivity : AppCompatActivity() {

    private var tuyaLockDevice: IThingBleLockV2? = null
    private val adapter: MemberSelectListAdapter by lazy {
        MemberSelectListAdapter()
    }
    private var unlockInfoBean: List<UnlockInfoBean>? = null

    companion object {
        fun startActivity(context: Context, devId: String?, infoBean: List<UnlockInfoBean?>?) {
            val intent = Intent(context, MemberSelectListActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            intent.putExtra(Constant.UNLOCK_INFO_BEAN, JSONArray.toJSONString(infoBean))
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_select_list)

        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }

        val mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        val allocString = intent.getStringExtra(Constant.UNLOCK_INFO_BEAN)
        unlockInfoBean = JSONArray.parseArray(
            allocString,
            UnlockInfoBean::class.java
        )


        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        tuyaLockDevice = tuyaLockManager.getBleLockV2(mDevId)

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
        tuyaLockDevice!!.getProLockMemberList(object :
            IThingResultCallback<ArrayList<MemberInfoBean>?> {
            override fun onSuccess(result: ArrayList<MemberInfoBean>?) {
                adapter.setData(result)
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

    private fun allocProUnlockOpMode(memberInfoBean: MemberInfoBean?) {
        val unlockIds: MutableList<String> = ArrayList()
        for (infoBean in unlockInfoBean!!) {
            unlockIds.add(infoBean.unlockId)
        }
        if (unlockIds.size == 0) {
            Toast.makeText(
                applicationContext,
                resources.getString(R.string.no_unlock_mode_selected),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        tuyaLockDevice!!.allocProUnlockOpMode(
            memberInfoBean!!.userId,
            unlockIds,
            object : IThingResultCallback<Boolean?> {
                override fun onSuccess(result: Boolean?) {
                    Toast.makeText(applicationContext, "alloc onSuccess", Toast.LENGTH_SHORT).show()
                    finish()
                }

                override fun onError(code: String, message: String) {
                    Log.e(
                        Constant.TAG,
                        "allocProUnlockOpMode failed: code = $code  message = $message"
                    )
                    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                }
            })
    }
}