package com.tuya.lock.demo.ble.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
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
import com.thingclips.smart.sdk.optimus.lock.bean.ble.AllocOpModeBean
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.adapter.OpModeSelectListAdapter
import com.tuya.lock.demo.common.bean.UnlockInfo
import com.tuya.lock.demo.common.constant.Constant


/**
 *
 * Created by HuiYao on 2024/2/29
 */
class OpModeUnboundListActivity : AppCompatActivity() {

    private var tuyaLockDevice: IThingBleLockV2? = null
    private val adapter: OpModeSelectListAdapter by lazy {
        OpModeSelectListAdapter()
    }
    private val unlockInfoList: ArrayList<UnlockInfo> = ArrayList()

    companion object {
        fun startActivity(context: Context, devId: String?) {
            val intent = Intent(context, OpModeUnboundListActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_opmode_unbound_list)

        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }

        val mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        tuyaLockDevice = tuyaLockManager.getBleLockV2(mDevId)

        val rvList = findViewById<RecyclerView>(R.id.mode_list)
        rvList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rvList.adapter = adapter

        findViewById<Button>(R.id.select_btn).setOnClickListener { v: View ->
            MemberSelectListActivity.startActivity(
                v.context,
                mDevId,
                adapter.getSelectList()
            )
        }
    }

    override fun onResume() {
        super.onResume()
        getUnboundList()
    }

    private fun getUnboundList() {
        tuyaLockDevice!!.getProUnboundUnlockOpModeList(object :
            IThingResultCallback<java.util.ArrayList<AllocOpModeBean>> {
            override fun onSuccess(result: java.util.ArrayList<AllocOpModeBean>) {
                Log.i(
                    Constant.TAG,
                    "getProUnboundUnlockOpModeList:" + JSONArray.toJSONString(result)
                )
                unlockInfoList.clear()
                for (itemDetail in result) {
                    var type = ""
                    when (itemDetail.opMode) {
                        "1" -> type = resources.getString(R.string.mode_fingerprint)
                        "2" -> type = resources.getString(R.string.mode_card)
                        "3" -> type = resources.getString(R.string.mode_password)
                    }
                    val unlockInfo = UnlockInfo()
                    unlockInfo.count = itemDetail.unlockList.size
                    unlockInfo.type = 0
                    unlockInfo.name = type
                    unlockInfoList.add(unlockInfo)
                    for (infoBean in itemDetail.unlockList) {
                        val infoItem = UnlockInfo()
                        infoItem.type = 1
                        infoItem.name = infoBean.unlockName
                        infoItem.infoBean = infoBean
                        unlockInfoList.add(infoItem)
                    }
                }
                adapter.setData(unlockInfoList)
                adapter.notifyDataSetChanged()
            }

            override fun onError(code: String, message: String) {
                Log.e(
                    Constant.TAG,
                    "getProUnboundUnlockOpModeList failed: code = $code  message = $message"
                )
                Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
            }
        })
    }
}