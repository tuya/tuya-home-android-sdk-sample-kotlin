package com.tuya.lock.demo.ble.activity

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingBleLockV2
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.optimus.lock.bean.ble.MemberInfoBean
import com.thingclips.smart.sdk.optimus.lock.bean.ble.OpModeRemoveRequest
import com.thingclips.smart.sdk.optimus.lock.utils.LockUtil
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.adapter.OpModeListAdapter
import com.tuya.lock.demo.common.bean.UnlockInfo
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.DialogUtils.showDelete
import com.tuya.lock.demo.common.utils.OpModeUtils

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class OpModeListActivity : AppCompatActivity() {

    private var tuyaLockDevice: IThingBleLockV2? = null
    private val unlockInfoList: MutableList<UnlockInfo> = ArrayList()
    private var memberInfo: MemberInfoBean? = null
    private val adapter: OpModeListAdapter by lazy {
        OpModeListAdapter()
    }
    private var deviceId: String? = null
    private var userId: String? = null
    private var lockUserId = 0

    companion object{
        fun startActivity(context: Context, devId: String?, userId: String?, lockUserId: Int) {
            val intent = Intent(context, OpModeListActivity::class.java)
            intent.putExtra(Constant.DEVICE_ID, devId)
            intent.putExtra(Constant.USER_ID, userId)
            intent.putExtra(Constant.LOCK_USER_ID, lockUserId)
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
        userId = intent.getStringExtra(Constant.USER_ID)
        lockUserId = intent.getIntExtra(Constant.LOCK_USER_ID, 0)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        tuyaLockDevice = tuyaLockManager.getBleLockV2(deviceId)
        findViewById<View>(R.id.show_code).setOnClickListener { v: View ->
            ShowCodeActivity.startActivity(
                v.context,
                JSONObject.toJSONString(memberInfo)
            )
        }
    }

    private fun initData() {
        memberInfo!!.lockUserId = lockUserId
        unlockInfoList.clear()
        for (itemDetail in memberInfo!!.unlockDetail) {
            val dpCode = LockUtil.convertId2Code(deviceId, itemDetail.dpId.toString())
            val unlockInfo = UnlockInfo()
            unlockInfo.type = 0
            unlockInfo.count = itemDetail.unlockList.size
            unlockInfo.name = OpModeUtils.getTypeName(this@OpModeListActivity, dpCode)
            unlockInfo.dpCode = dpCode
            unlockInfoList.add(unlockInfo)
            for (infoBean in itemDetail.unlockList) {
                val infoItem = UnlockInfo()
                infoItem.type = 1
                infoItem.dpCode = dpCode
                infoItem.name = infoBean.unlockName
                infoItem.infoBean = infoBean
                unlockInfoList.add(infoItem)
            }
        }
        val recyclerView = findViewById<RecyclerView>(R.id.unlock_list)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = adapter
        adapter.setData(unlockInfoList)
        adapter.addCallback(object : OpModeListAdapter.Callback {
            override fun edit(info: UnlockInfo?, position: Int) {
                OpModeDetailActivity.startActivity(
                    this@OpModeListActivity,
                    memberInfo,
                    info!!.infoBean!!.opModeId,
                    deviceId,
                    info.dpCode
                )
            }

            override fun delete(info: UnlockInfo?, position: Int) {
                showDelete(
                    this@OpModeListActivity
                ) { _: DialogInterface?, _: Int ->
                    removeOpMode(
                        info!!,
                        position
                    )
                }
            }

            override fun add(info: UnlockInfo?, position: Int) {
                OpModeDetailActivity.startActivity(
                    this@OpModeListActivity,
                    memberInfo,
                    null,
                    deviceId,
                    info!!.dpCode
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
        unlockInfoList.clear()
    }

    private fun getListData() {
        tuyaLockDevice!!.getProBoundUnlockOpModeList(
            userId,
            object : IThingResultCallback<MemberInfoBean> {
                override fun onSuccess(result: MemberInfoBean) {
                    Log.i(
                        Constant.TAG,
                        "getProBoundUnlockOpModeList:" + JSONObject.toJSONString(result)
                    )
                    memberInfo = result
                    initData()
                }

                override fun onError(code: String, message: String) {
                    Log.i(
                        Constant.TAG,
                        "getProBoundUnlockOpModeList onError code:$code, message:$message"
                    )
                    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                }
            })
    }

    fun removeOpMode(info: UnlockInfo, position: Int) {
        val removeRequest = OpModeRemoveRequest()
        removeRequest.userId = memberInfo!!.userId
        removeRequest.lockUserId = memberInfo!!.lockUserId
        removeRequest.unlockId = info.infoBean!!.unlockId
        removeRequest.opModeId = info.infoBean!!.opModeId
        removeRequest.userType = memberInfo!!.userType
        tuyaLockDevice!!.removeProUnlockOpModeForMember(
            removeRequest,
            object : IThingResultCallback<Boolean?> {
                override fun onSuccess(result: Boolean?) {
                    Toast.makeText(applicationContext, "onSuccess", Toast.LENGTH_SHORT).show()
                    adapter.remove(position)
                }

                override fun onError(errorCode: String, errorMessage: String) {
                    Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
                }
            })
    }
}