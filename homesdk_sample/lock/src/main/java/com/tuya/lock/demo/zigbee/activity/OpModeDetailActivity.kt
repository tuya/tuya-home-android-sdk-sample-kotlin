package com.tuya.lock.demo.zigbee.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.android.common.utils.L
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.lock.api.IThingZigBeeLock
import com.thingclips.smart.optimus.lock.api.ThingUnlockType
import com.thingclips.smart.optimus.lock.api.bean.UnlockModeResponse
import com.thingclips.smart.optimus.lock.api.zigbee.request.OpModeAddRequest
import com.thingclips.smart.optimus.lock.api.zigbee.response.MemberInfoBean
import com.thingclips.smart.optimus.lock.api.zigbee.response.OpModeAddBean
import com.thingclips.smart.optimus.lock.api.zigbee.response.OpModeBean
import com.thingclips.smart.optimus.lock.api.zigbee.status.ZigbeeOpModeStage
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.api.IDevListener
import com.thingclips.smart.sdk.api.IResultCallback
import com.thingclips.smart.sdk.api.IThingDevice
import com.thingclips.smart.sdk.optimus.lock.utils.StandardDpConverter
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.activity.ShowCodeActivity
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.DialogUtils
import com.tuya.lock.demo.common.utils.OpModeUtils
import com.tuya.lock.demo.common.utils.PasscodeUtils

/**
 *
 * Created by HuiYao on 2024/3/1
 */
class OpModeDetailActivity : AppCompatActivity() {
    private var zigBeeLock: IThingZigBeeLock? = null
    private var mFrom = 0
    private var memberInfoBean: MemberInfoBean? = null
    private val request = OpModeAddRequest()
    private var add_tips_view: TextView? = null
    private var addView: Button? = null
    private var ITuyaDevice: IThingDevice? = null
    private var toolbar: Toolbar? = null
    private var add_name_view: EditText? = null
    private var hijack_switch: SwitchCompat? = null
    private var add_password: EditText? = null
    private var show_code_view: Button? = null
    private var addString: String? = null
    private var mDevId: String? = null
    private var isAddMode = false
    private var opModeBean: OpModeBean? = null
    private var total = 0
    private var dpCode: String? = null
    private var tyabitmqxx = false

    companion object{
        fun startEditActivity(
            context: Context,
            memberInfoBean: MemberInfoBean?,
            devId: String?,
            opModeBean: OpModeBean?,
            dpCode: String?
        ) {
            val intent = Intent(context, OpModeDetailActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            //用户数据
            intent.putExtra(Constant.USER_DATA, JSONObject.toJSONString(memberInfoBean))
            //解锁方式详情
            intent.putExtra(Constant.UNLOCK_INFO, JSONObject.toJSONString(opModeBean))
            intent.putExtra(Constant.DP_CODE, dpCode)
            context.startActivity(intent)
        }

        fun startAddActivity(
            context: Context,
            memberInfoBean: MemberInfoBean?,
            devId: String?,
            dpCode: String?
        ) {
            val intent = Intent(context, OpModeDetailActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            //用户数据
            intent.putExtra(Constant.USER_DATA, JSONObject.toJSONString(memberInfoBean))
            //锁类型
            intent.putExtra(Constant.DP_CODE, dpCode)
            context.startActivity(intent)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unlock_mode_add)
        toolbar = findViewById(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar?.setNavigationOnClickListener { onBackPressed() }
        add_tips_view = findViewById(R.id.add_tips)
        add_tips_view?.visibility = View.GONE
        val userData = intent.getStringExtra(Constant.USER_DATA)
        val unlockData = intent.getStringExtra(Constant.UNLOCK_INFO)
        mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        dpCode = intent.getStringExtra(Constant.DP_CODE)
        memberInfoBean = JSONObject.parseObject(
            userData,
            MemberInfoBean::class.java
        )
        if (!TextUtils.isEmpty(unlockData)) {
            mFrom = 1
        }
        opModeBean = JSONObject.parseObject(unlockData, OpModeBean::class.java)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        zigBeeLock = tuyaLockManager.getZigBeeLock(mDevId)
        ITuyaDevice = ThingHomeSdk.newDeviceInstance(mDevId)
        ITuyaDevice?.registerDevListener(object : IDevListener {
            override fun onDpUpdate(devId: String, dpStr: String) {
                val dpCode = StandardDpConverter.convertIdToCodeMap(
                    dpStr,
                    StandardDpConverter.getSchemaMap(mDevId)
                )
                Log.i(Constant.TAG, "onDpUpdate dpCode = $dpCode")
                dealAddUnlockMode(dpCode)
            }

            override fun onRemoved(devId: String) {}
            override fun onStatusChanged(devId: String, online: Boolean) {}
            override fun onNetworkStatusChanged(devId: String, status: Boolean) {}
            override fun onDevInfoUpdate(devId: String) {}
        })
        memberInfoBean?.apply {
            request.userType = userType
            request.userId = userId
            request.lockUserId = lockUserId
        }
        request.unlockType = dpCode
        if (mFrom == 1) {
            opModeBean?.apply {
                request.unlockName = unlockName
                request.unlockAttr = unlockAttr
            }
        }
        initView()
        //面板云能力
        zigBeeLock?.getLockDeviceConfig(object : IThingResultCallback<JSONObject> {
            override fun onSuccess(result: JSONObject) {
                val powerCode = result.getJSONObject("powerCode")
                if (powerCode != null) {
                    tyabitmqxx = if (powerCode.containsKey("tyabitmqxx")) {
                        powerCode.getBooleanValue("tyabitmqxx")
                    } else {
                        true
                    }
                }
                initData()
            }

            override fun onError(errorCode: String, errorMessage: String) {}
        })
    }

    fun initView() {
        add_name_view = findViewById<EditText>(R.id.add_name)
        hijack_switch = findViewById<SwitchCompat>(R.id.hijack_switch)
        addView = findViewById<Button>(R.id.unlock_mode_add)
        add_password = findViewById<EditText>(R.id.add_password)
        show_code_view = findViewById<Button>(R.id.show_code_view)
    }

    override fun onResume() {
        super.onResume()
    }

    fun initData() {
        dpCode?.apply {
            toolbar?.title = OpModeUtils.getTypeName(this@OpModeDetailActivity, this)
        }
        add_name_view!!.setText(request.unlockName)
        add_name_view!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (!TextUtils.isEmpty(s)) {
                    request.unlockName = s.toString()
                }
            }

            override fun afterTextChanged(s: Editable) {}
        })
        if (mFrom == 1) {
            hijack_switch!!.isChecked = request.unlockAttr == 1
        } else {
            hijack_switch!!.isChecked = false
            request.unlockAttr = 0
        }
        hijack_switch!!.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            request.unlockAttr = if (isChecked) 1 else 0
        }
        if (dpCode == ThingUnlockType.PASSWORD) {
            if (!tyabitmqxx) {
                findViewById<View>(R.id.password_wrap).visibility = View.VISIBLE
            } else {
                findViewById<View>(R.id.password_wrap).visibility = View.GONE
            }
            findViewById<View>(R.id.random_password).setOnClickListener { v: View? ->
                add_password?.setText(
                    PasscodeUtils.getRandom(6)
                )
            }
            add_password!!.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    if (!TextUtils.isEmpty(s)) {
                        request.password = s.toString()
                    }
                }

                override fun afterTextChanged(s: Editable) {}
            })
        } else {
            findViewById<View>(R.id.password_wrap).visibility = View.GONE
        }
        addString = if (mFrom == 1) {
            resources.getString(R.string.submit_edit)
        } else {
            resources.getString(R.string.submit_add)
        }
        addView!!.text = addString
        addView!!.setOnClickListener { v: View? ->
            //老版本校验解锁方式名称
            if (TextUtils.isEmpty(add_name_view!!.text)) {
                showTips(resources.getString(R.string.enter_unlock_mode_name), false)
                return@setOnClickListener
            }
            val loadingStr = "$addString..."
            addView!!.text = loadingStr
            addView!!.isEnabled = false
            if (mFrom == 0) {
                addUnlockMode()
            } else {
                upDataUnlockMode()
            }
        }
        show_code_view?.setOnClickListener { v: View ->
            ShowCodeActivity.startActivity(
                v.context,
                JSONObject.toJSONString(opModeBean)
            )
        }
    }

    private fun addUnlockMode() {
        isAddMode = true
        if (!tyabitmqxx && dpCode == ThingUnlockType.PASSWORD) {
            zigBeeLock!!.addPasswordOpmodeForMember(
                request,
                object : IThingResultCallback<OpModeAddBean?> {
                    override fun onSuccess(result: OpModeAddBean?) {
                        val tips = "add onSuccess"
                        showTips(tips, true)
                        L.i(Constant.TAG, tips + JSONObject.toJSONString(result))
                        isAddMode = false

                        //退出页面
                        finish()
                    }

                    override fun onError(errorCode: String, errorMessage: String) {
                        Log.e(Constant.TAG, "addProUnlockOpModeForMember:$errorMessage")
                        showTips(errorMessage, false)
                        addView!!.text = addString
                        addView!!.isEnabled = true
                        isAddMode = false
                    }
                })
        } else {
            zigBeeLock!!.addUnlockOpmodeForMember(
                request,
                object : IThingResultCallback<OpModeAddBean?> {
                    override fun onSuccess(result: OpModeAddBean?) {
                        val tips = "add onSuccess"
                        showTips(tips, true)
                        L.i(Constant.TAG, tips + JSONObject.toJSONString(result))
                        isAddMode = false

                        //退出页面
                        finish()
                    }

                    override fun onError(errorCode: String, errorMessage: String) {
                        Log.e(Constant.TAG, "addProUnlockOpModeForMember:$errorMessage")
                        showTips(errorMessage, false)
                        addView!!.text = addString
                        addView!!.isEnabled = true
                        isAddMode = false
                    }
                })
        }
    }

    private fun upDataUnlockMode() {
        zigBeeLock!!.modifyUnlockOpmodeForMember(
            request.unlockName,
            opModeBean!!.opmodeId,
            request.unlockAttr,
            opModeBean!!.unlockId,
            object : IThingResultCallback<Boolean?> {
                override fun onSuccess(result: Boolean?) {
                    val tips = "update onSuccess"
                    showTips(tips, true)
                    L.i(Constant.TAG, tips + JSONObject.toJSONString(result))
                    finish()
                }

                override fun onError(errorCode: String, errorMessage: String) {
                    showTips(errorMessage, false)
                    addView!!.text = addString
                    addView!!.isEnabled = true
                    Log.i(Constant.TAG, "upDataUnlockMode$errorMessage")
                }
            })
    }

    private fun dealAddUnlockMode(dpCode: Map<String, Any>) {
        for (key in dpCode.keys) {
            val o = dpCode[key]
            if (TextUtils.equals(key, UnlockModeResponse.UNLOCK_METHOD_CREATE)) {
                if (o is String) {
                    dealLockResponse(o)
                }
            }
        }
    }

    private fun dealLockResponse(lockResponse: String) {
        val stage = lockResponse.substring(2, 4).toInt(16)
        val count = lockResponse.substring(14, 16).toInt(16)
        if (stage == ZigbeeOpModeStage.STAGE_START) {
            Log.i(Constant.TAG, "count:$count")
            val tipsAdd = "count: 0/$count"
            showTips(tipsAdd, true)
            total = count
        } else if (stage == ZigbeeOpModeStage.STAGE_ENTERING) {
            Log.i(Constant.TAG, "count:$count")
            val tipsAdd = "count: $count/$total"
            showTips(tipsAdd, true)
        }
    }

    private fun showTips(tips: String, isSteps: Boolean) {
        if (isSteps) {
            add_tips_view!!.setBackgroundColor(resources.getColor(R.color.green))
        } else {
            add_tips_view!!.setBackgroundColor(resources.getColor(R.color.red))
        }
        add_tips_view!!.visibility = View.VISIBLE
        add_tips_view!!.post { add_tips_view!!.text = tips }
    }

    override fun onDestroy() {
        super.onDestroy()
        ITuyaDevice!!.unRegisterDevListener()
        ITuyaDevice!!.onDestroy()
        memberInfoBean = null
    }

    private fun cancelUnlock() {
        zigBeeLock!!.cancelUnlockOpMode(
            request.unlockType,
            request.lockUserId,
            request.userType,
            object : IResultCallback {
                override fun onError(code: String, error: String) {
                    showTips(error, false)
                }

                override fun onSuccess() {}
            })
    }


    override fun onBackPressed() {
        //展示指纹录入的提示
        if (isAddMode) {
            DialogUtils.showDelete(
                this,
                resources.getString(R.string.whether_to_cancel)
            ) { _, _ -> cancelUnlock() }
            super@OpModeDetailActivity.onBackPressed()
        } else {
            super.onBackPressed()
        }
    }
}