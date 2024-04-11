package com.tuya.lock.demo.wifi.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.lock.api.IThingWifiLock
import com.thingclips.smart.optimus.lock.api.bean.UnlockRelation
import com.thingclips.smart.optimus.lock.api.bean.WifiLockUser
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.bean.WifiUnlockInfo
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.DialogUtils
import com.tuya.lock.demo.common.utils.Utils
import com.tuya.lock.demo.wifi.adapter.OpModeListAdapter

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class MemberDetailActivity: AppCompatActivity() {

    private var userBean: WifiLockUser? = null
    private var mFrom = 0

    private var isUnlockEdit = false

    private var wifiLock: IThingWifiLock? = null

    private val unlockRelations: MutableList<UnlockRelation> = ArrayList()

    private val adapter: OpModeListAdapter by lazy {
        OpModeListAdapter()
    }

    companion object{
        fun startActivity(context: Context, memberInfoBean: WifiLockUser?, devId: String?, from: Int) {
            val intent = Intent(context, MemberDetailActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            //0创建、1编辑
            intent.putExtra(Constant.FROM, from)
            //用户信息
            intent.putExtra(Constant.USER_DATA, JSONObject.toJSONString(memberInfoBean))
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_member_detail)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        val userData = intent.getStringExtra(Constant.USER_DATA)
        val mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        mFrom = intent.getIntExtra(Constant.FROM, 0)
        try {
            userBean = JSONObject.parseObject(userData, WifiLockUser::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
        }
        if (null == userBean) {
            userBean = WifiLockUser()
        }
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        wifiLock = tuyaLockManager.getWifiLock(mDevId)
        if (mFrom == 1) {
            toolbar.title = resources.getString(R.string.submit_edit)
        } else {
            toolbar.title = resources.getString(R.string.user_add)
        }
        if (userBean!!.unlockRelations != null) {
            unlockRelations.addAll(userBean!!.unlockRelations)
        }
        /**
         * 用户昵称
         */
        val nameView = findViewById<EditText>(R.id.user_name)
        nameView.setText(userBean!!.userName)
        nameView.isEnabled = userBean!!.userType != 1
        nameView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!TextUtils.isEmpty(s)) {
                    userBean!!.userName = s.toString()
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        /**
         * 头像
         */
        val user_face = findViewById<ImageView>(R.id.user_face)
        if (!TextUtils.isEmpty(userBean!!.avatarUrl)) {
            Utils.showImageUrl(userBean!!.avatarUrl, user_face)
        }
        /**
         * 解锁方式列表
         */
        val recyclerView = findViewById<RecyclerView>(R.id.unlock_list)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        adapter.setDeviceId(mDevId)
        adapter.setData(unlockRelations)
        recyclerView.adapter = adapter
        /**
         * 提交创建或更新
         */
        val submitBtn = findViewById<Button>(R.id.edit_user_submit)
        submitBtn.setOnClickListener { v: View? ->
            if (mFrom == 0) {
                addLockUser()
            } else {
                upDateLockUser()
            }
        }
        if (mFrom == 1) {
            submitBtn.text = resources.getString(R.string.submit_edit)
        } else {
            submitBtn.text = resources.getString(R.string.submit_add)
        }
        adapter.addCallback(object : OpModeListAdapter.Callback {
            override fun edit(info: WifiUnlockInfo?, position: Int) {
                OpModeDetailActivity.startActivity(
                    this@MemberDetailActivity,
                    info!!.name!!.toInt(),
                    info.dpCode
                )
            }

            override fun delete(info: WifiUnlockInfo?, position: Int) {
                DialogUtils.showDelete(
                    this@MemberDetailActivity
                ) { _, _ -> deleteLockUser(info!!.name!!.toInt()) }
            }

            override fun add(info: WifiUnlockInfo?, position: Int) {
                OpModeDetailActivity.startActivity(this@MemberDetailActivity, 0, info?.dpCode)
            }
        })
    }

    private fun deleteLockUser(sn: Int) {
        var index = -1
        for (i in unlockRelations.indices) {
            val item = unlockRelations[i]
            if (item.passwordNumber == sn) {
                index = i
            }
        }
        unlockRelations.removeAt(index)
        adapter.setData(unlockRelations)
        adapter.notifyDataSetChanged()
        isUnlockEdit = true
    }


    override fun onDestroy() {
        super.onDestroy()
    }

    /**
     * 用户更新信息
     */
    private fun addLockUser() {
        wifiLock!!.addLockUser(
            userBean!!.userName,
            null,
            unlockRelations,
            object : IThingResultCallback<String?> {
                override fun onSuccess(result: String?) {
                    Log.i(Constant.TAG, "add lock user success")
                    Toast.makeText(applicationContext, "add lock user success", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                }

                override fun onError(code: String, message: String) {
                    runOnUiThread {
                        Log.e(
                            Constant.TAG,
                            "add lock user failed: code = $code  message = $message"
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

    private fun upDateLockUser() {
        if (isUnlockEdit) {
            wifiLock!!.updateFamilyUserUnlockMode(
                userBean!!.userId,
                unlockRelations,
                object : IThingResultCallback<Boolean?> {
                    override fun onSuccess(result: Boolean?) {
                        Log.i(Constant.TAG, "update lock user success")
                        Toast.makeText(
                            applicationContext,
                            "update lock user success",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }

                    override fun onError(code: String, error: String) {
                        runOnUiThread {
                            Log.e(
                                Constant.TAG,
                                "add lock user failed: code = $code  message = $error"
                            )
                            Toast.makeText(
                                applicationContext,
                                error,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                })
            return
        }
        wifiLock!!.updateLockUser(
            userBean!!.userId,
            userBean!!.userName,
            null,
            unlockRelations,
            object : IThingResultCallback<Boolean?> {
                override fun onSuccess(result: Boolean?) {
                    Log.i(Constant.TAG, "update lock user success")
                    Toast.makeText(
                        applicationContext,
                        "update lock user success",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }

                override fun onError(code: String, error: String) {
                    runOnUiThread {
                        Log.e(
                            Constant.TAG,
                            "add lock user failed: code = $code  message = $error"
                        )
                        Toast.makeText(
                            applicationContext,
                            error,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OpModeDetailActivity.REQUEST_CODE) {
            if (null != data) {
                val unlockData = data.getStringExtra(Constant.UNLOCK_INFO)
                Log.i(Constant.TAG, "onActivityResult====>$unlockData")
                if (!TextUtils.isEmpty(unlockData)) {
                    val unlockInfo = JSONObject.parseObject(
                        unlockData,
                        UnlockRelation::class.java
                    )
                    if (unlockInfo.passwordNumber == 0) {
                        return
                    }
                    unlockRelations.add(unlockInfo)
                    adapter.setData(unlockRelations)
                    adapter.notifyDataSetChanged()
                    isUnlockEdit = true
                }
            }
        }
    }
}