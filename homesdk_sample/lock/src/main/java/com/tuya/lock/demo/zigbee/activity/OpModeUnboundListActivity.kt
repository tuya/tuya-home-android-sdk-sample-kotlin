package com.tuya.lock.demo.zigbee.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.android.common.utils.L
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.lock.api.IThingZigBeeLock
import com.thingclips.smart.optimus.lock.api.zigbee.response.OpModeBean
import com.thingclips.smart.optimus.lock.api.zigbee.response.UnAllocOpModeBean
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.api.IThingDataCallback
import com.thingclips.smart.sdk.optimus.lock.bean.ZigBeeDatePoint
import com.thingclips.smart.sdk.optimus.lock.utils.LockUtil
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.activity.ShowCodeActivity
import com.tuya.lock.demo.common.bean.ZigbeeUnlockInfo
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.OpModeUtils
import com.tuya.lock.demo.zigbee.adapter.OpModeSelectListAdapter

/**
 *
 * Created by HuiYao on 2024/3/1
 */
class OpModeUnboundListActivity: AppCompatActivity() {

    private var zigBeeLock: IThingZigBeeLock? = null
    private val adapter: OpModeSelectListAdapter by lazy {
        OpModeSelectListAdapter()
    }
    private var mDevId: String? = null

    private val unAllocOpModeArrayList = ArrayList<UnAllocOpModeBean>()

    companion object{
        fun startActivity(context: Context, devId: String?) {
            val intent = Intent(context, OpModeUnboundListActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zigbee_opmode_unbound_list)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        zigBeeLock = tuyaLockManager.getZigBeeLock(mDevId)
        val rvList = findViewById<RecyclerView>(R.id.mode_list)
        rvList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rvList.adapter = adapter
        val select_btn = findViewById<Button>(R.id.select_btn)
        select_btn.setOnClickListener { v: View ->
            MemberSelectListActivity.startActivity(
                v.context,
                mDevId,
                adapter.getSelectList(),
                0
            )
        }
        findViewById<View>(R.id.code_btn).setOnClickListener { v: View? ->
            ShowCodeActivity.startActivity(
                this@OpModeUnboundListActivity,
                JSONObject.toJSONString(unAllocOpModeArrayList)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        getUnboundList()
    }

    override fun onDestroy() {
        super.onDestroy()
        unAllocOpModeArrayList.clear()
    }

    private fun getUnboundList() {
        zigBeeLock!!.getUnAllocOpMode(object : IThingDataCallback<ArrayList<UnAllocOpModeBean>> {
            override fun onSuccess(result: ArrayList<UnAllocOpModeBean>) {
                L.i(Constant.TAG, "getProUnboundUnlockOpModeList:" + JSONArray.toJSONString(result))
                unAllocOpModeArrayList.clear()
                unAllocOpModeArrayList.addAll(result)
                val unlockInfoList: MutableList<ZigbeeUnlockInfo> = ArrayList<ZigbeeUnlockInfo>()
                val fingerList: MutableList<ZigbeeUnlockInfo> = ArrayList<ZigbeeUnlockInfo>()
                val passwordList: MutableList<ZigbeeUnlockInfo> = ArrayList<ZigbeeUnlockInfo>()
                val cradList: MutableList<ZigbeeUnlockInfo> = ArrayList<ZigbeeUnlockInfo>()

                //指纹标题
                val fingerInfo = ZigbeeUnlockInfo()
                fingerInfo.dpCode = ZigBeeDatePoint.UNLOCK_FINGERPRINT
                fingerInfo.name = OpModeUtils.getTypeName(
                    this@OpModeUnboundListActivity,
                    ZigBeeDatePoint.UNLOCK_FINGERPRINT
                )
                fingerInfo.type = 0
                fingerList.add(fingerInfo)
                //密码标题
                val passwordInfo = ZigbeeUnlockInfo()
                passwordInfo.dpCode = ZigBeeDatePoint.UNLOCK_PASSWORD
                passwordInfo.name = OpModeUtils.getTypeName(
                    this@OpModeUnboundListActivity,
                    ZigBeeDatePoint.UNLOCK_PASSWORD
                )
                passwordInfo.type = 0
                passwordList.add(passwordInfo)
                //卡片标题
                val cardInfo = ZigbeeUnlockInfo()
                cardInfo.dpCode = ZigBeeDatePoint.UNLOCK_CARD
                cardInfo.name = OpModeUtils.getTypeName(
                    this@OpModeUnboundListActivity,
                    ZigBeeDatePoint.UNLOCK_CARD
                )
                cardInfo.type = 0
                cradList.add(cardInfo)
                for (itemDetail in result) {
                    for (unlockInfo in itemDetail.unlockInfo) {
                        val opModeBean = OpModeBean()
                        opModeBean.opmodeId = unlockInfo.opmodeId
                        opModeBean.unlockName = unlockInfo.unlockName
                        opModeBean.unlockId = unlockInfo.unlockId
                        val dpCode = LockUtil.convertId2Code(mDevId, itemDetail.opmode)
                        val infoItem = ZigbeeUnlockInfo()
                        infoItem.type = 1
                        infoItem.dpCode = dpCode
                        infoItem.infoBean = opModeBean
                        if (TextUtils.equals(dpCode, ZigBeeDatePoint.UNLOCK_FINGERPRINT)) {
                            fingerList.add(infoItem)
                        } else if (TextUtils.equals(dpCode, ZigBeeDatePoint.UNLOCK_PASSWORD)) {
                            passwordList.add(infoItem)
                        } else if (TextUtils.equals(dpCode, ZigBeeDatePoint.UNLOCK_CARD)) {
                            cradList.add(infoItem)
                        }
                    }
                }
                unlockInfoList.addAll(fingerList)
                unlockInfoList.addAll(passwordList)
                unlockInfoList.addAll(cradList)
                adapter.setData(unlockInfoList)
                adapter.notifyDataSetChanged()
            }

            override fun onError(code: String, message: String) {
                runOnUiThread {
                    L.e(
                        Constant.TAG,
                        "getProUnboundUnlockOpModeList failed: code = $code  message = $message"
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
}