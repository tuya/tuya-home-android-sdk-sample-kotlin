package com.tuya.lock.demo.zigbee.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson.JSONObject
import com.thingclips.sdk.os.ThingOSDevice
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.lock.api.IThingZigBeeLock
import com.thingclips.smart.optimus.lock.api.zigbee.response.MemberInfoBean
import com.thingclips.smart.optimus.lock.api.zigbee.response.OpModeBean
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.optimus.lock.bean.ble.OpModeRemoveRequest
import com.thingclips.smart.sdk.optimus.lock.utils.LockUtil
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.activity.ShowCodeActivity
import com.tuya.lock.demo.common.bean.ZigbeeUnlockInfo
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.DialogUtils
import com.tuya.lock.demo.zigbee.adapter.OpModeListAdapter

/**
 *
 * Created by HuiYao on 2024/3/1
 */
class OpModeListActivity : AppCompatActivity() {

    private var zigBeeLock: IThingZigBeeLock? = null
    private var memberInfo: MemberInfoBean? = null
    private val adapter: OpModeListAdapter by lazy {
        OpModeListAdapter()
    }
    private var deviceId: String? = null
    private val opModeBeanArrayList = ArrayList<OpModeBean>()

    companion object{
        fun startActivity(context: Context, devId: String?, bean: MemberInfoBean?) {
            val intent = Intent(context, OpModeListActivity::class.java)
            intent.putExtra(Constant.DEVICE_ID, devId)
            intent.putExtra(Constant.USER_DATA, JSONObject.toJSONString(bean))
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unlock_mode_list)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        deviceId = intent.getStringExtra(Constant.DEVICE_ID)
        memberInfo = JSONObject.parseObject<MemberInfoBean>(
            intent.getStringExtra(Constant.USER_DATA),
            MemberInfoBean::class.java
        )
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        zigBeeLock = tuyaLockManager.getZigBeeLock(deviceId)
        findViewById<View>(R.id.show_code).setOnClickListener { v: View ->
            ShowCodeActivity.startActivity(
                v.context,
                JSONObject.toJSONString(opModeBeanArrayList)
            )
        }
    }

    private fun initData() {
        val unlockInfoList: MutableList<ZigbeeUnlockInfo> = ArrayList()
        val deviceBean = ThingOSDevice.getDeviceBean(deviceId)
        for (itemDetail in memberInfo!!.unlockDetail) {
            var dpCode = ""
            var opModeName = ""
            for ((key, schemaItem) in deviceBean.getSchemaMap()) {
                if (TextUtils.equals(key, itemDetail.dpId.toString())) {
                    opModeName = schemaItem.name
                    dpCode = schemaItem.code
                    break
                }
            }
            val unlockInfo = ZigbeeUnlockInfo()
            unlockInfo.type = 0
            unlockInfo.count = itemDetail.unlockList.size
            unlockInfo.name = opModeName + "(" + itemDetail.unlockList.size + ")"
            unlockInfo.dpCode = dpCode
            unlockInfoList.add(unlockInfo)
            for (opModeBean in opModeBeanArrayList) {
                if (TextUtils.equals(itemDetail.dpId.toString(), opModeBean.opmode)) {
                    val beanDpCode = LockUtil.convertId2Code(deviceId, opModeBean.opmode)
                    val infoItem = ZigbeeUnlockInfo()
                    infoItem.type = 1
                    infoItem.dpCode = beanDpCode
                    infoItem.infoBean = opModeBean
                    unlockInfoList.add(infoItem)
                }
            }
        }
        val recyclerView = findViewById<RecyclerView>(R.id.unlock_list)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = adapter
        adapter.setData(unlockInfoList)
        adapter.notifyDataSetChanged()
        adapter.addCallback(object : OpModeListAdapter.Callback {
            override fun edit(info: ZigbeeUnlockInfo, position: Int) {
                OpModeDetailActivity.startEditActivity(
                    this@OpModeListActivity,
                    memberInfo,
                    deviceId,
                    info.infoBean,
                    info.dpCode
                )
            }

            override fun delete(info: ZigbeeUnlockInfo, position: Int) {
                DialogUtils.showDelete(this@OpModeListActivity) { dialog, which ->
                    removeOpMode(
                        info.infoBean!!,
                        position
                    )
                }
            }

            override fun add(info: ZigbeeUnlockInfo, position: Int) {
                OpModeDetailActivity.startAddActivity(
                    this@OpModeListActivity,
                    memberInfo,
                    deviceId,
                    info.dpCode
                )
            }
        })
    }

    override fun onResume() {
        super.onResume()
        getListData()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun getListData() {
        zigBeeLock!!.getMemberOpmodeList(
            memberInfo!!.userId,
            object : IThingResultCallback<ArrayList<OpModeBean>?> {
                override fun onSuccess(result: ArrayList<OpModeBean>?) {
                    Log.i(
                        Constant.TAG,
                        "getProBoundUnlockOpModeList:" + JSONObject.toJSONString(result)
                    )
                    opModeBeanArrayList.clear()
                    opModeBeanArrayList.addAll(result!!)
                    initData()
                }

                override fun onError(code: String, message: String) {
                    runOnUiThread {
                        Log.i(
                            Constant.TAG,
                            "getProBoundUnlockOpModeList onError code:$code, message:$message"
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

    fun removeOpMode(infoBean: OpModeBean, position: Int) {
        val removeRequest = OpModeRemoveRequest()
        removeRequest.userId = infoBean.userId
        removeRequest.lockUserId = infoBean.lockUserId
        removeRequest.unlockId = infoBean.unlockId
        removeRequest.opModeId = infoBean.opmodeId
        removeRequest.userType = infoBean.userType
        zigBeeLock!!.removeUnlockOpmodeForMember(
            removeRequest,
            object : IThingResultCallback<Boolean?> {
                override fun onSuccess(result: Boolean?) {
                    Toast.makeText(applicationContext, "onSuccess", Toast.LENGTH_SHORT).show()
                    adapter.remove(position)
                }

                override fun onError(errorCode: String, errorMessage: String) {
                    runOnUiThread {
                        Toast.makeText(
                            applicationContext,
                            errorMessage,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
    }
}