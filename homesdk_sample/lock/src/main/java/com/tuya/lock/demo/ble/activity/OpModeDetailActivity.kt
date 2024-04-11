package com.tuya.lock.demo.ble.activity

import android.content.Context
import android.content.DialogInterface
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
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.BleLockConstant
import com.thingclips.smart.optimus.lock.api.IThingBleLockV2
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.lock.api.ThingUnlockType
import com.thingclips.smart.optimus.lock.api.bean.UnlockModeResponse
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.api.IDevListener
import com.thingclips.smart.sdk.api.IResultCallback
import com.thingclips.smart.sdk.api.IThingDevice
import com.thingclips.smart.sdk.optimus.lock.bean.ble.AddOpmodeResult
import com.thingclips.smart.sdk.optimus.lock.bean.ble.MemberInfoBean
import com.thingclips.smart.sdk.optimus.lock.bean.ble.NotifyInfoBean
import com.thingclips.smart.sdk.optimus.lock.bean.ble.OpModeDetailBean
import com.thingclips.smart.sdk.optimus.lock.bean.ble.OpModeRequest
import com.thingclips.smart.sdk.optimus.lock.utils.LockUtil
import com.thingclips.smart.sdk.optimus.lock.utils.StandardDpConverter
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.DialogUtils.showDelete
import com.tuya.lock.demo.common.utils.OpModeUtils.getTypeName
import com.tuya.lock.demo.common.utils.PasscodeUtils

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class OpModeDetailActivity : AppCompatActivity() {

    private var tuyaLockDevice: IThingBleLockV2? = null
    private var mFrom = 0
    private var memberInfoBean: MemberInfoBean? = null
    private val request = OpModeRequest()
    private var add_tips_view: TextView? = null
    private var addView: Button? = null
    private var IThingDevice: IThingDevice? = null
    private var opModeId: Long = 0
    private var toolbar: Toolbar? = null
    private var add_name_view: EditText? = null
    private var hijack_switch: SwitchCompat? = null
    private var add_password: EditText? = null
    private var show_code_view: Button? = null
    private var detailBean: OpModeDetailBean? = null
    private var addString: String? = null
    private var mDevId: String? = null
    private var dpCode: String? = null
    private var isAddMode = false

    companion object {
        fun startActivity(
            context: Context, memberInfoBean: MemberInfoBean?, opModeId: Long?,
            devId: String?, dpCode: String?
        ) {
            val intent = Intent(context, OpModeDetailActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            //用户数据
            intent.putExtra(Constant.USER_DATA, JSONObject.toJSONString(memberInfoBean))
            //云端锁id
            intent.putExtra(Constant.OP_MODE_ID, opModeId)
            intent.putExtra(Constant.DP_CODE, dpCode)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unlock_mode_add)
        toolbar = findViewById(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar!!.setNavigationOnClickListener { onBackPressed() }
        add_tips_view = findViewById(R.id.add_tips)
        add_tips_view!!.visibility = View.GONE
        val userData = intent.getStringExtra(Constant.USER_DATA)
        mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        opModeId = intent.getLongExtra(Constant.OP_MODE_ID, -1)
        dpCode = intent.getStringExtra(Constant.DP_CODE)
        memberInfoBean = JSONObject.parseObject(
            userData,
            MemberInfoBean::class.java
        )
        if (opModeId > 0) {
            mFrom = 1
        }
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        tuyaLockDevice = tuyaLockManager.getBleLockV2(mDevId)
        IThingDevice = ThingHomeSdk.newDeviceInstance(mDevId)
        IThingDevice!!.registerDevListener(object : IDevListener {
            override fun onDpUpdate(devId: String, dpStr: String) {
                val dpCode = StandardDpConverter.convertIdToCodeMap(
                    dpStr,
                    StandardDpConverter.getSchemaMap(mDevId)
                )
                Log.i(
                    Constant.TAG,
                    "onDpUpdate dpCode = $dpCode"
                )
                dealAddUnlockMode(dpCode)
            }

            override fun onRemoved(devId: String) {}
            override fun onStatusChanged(devId: String, online: Boolean) {}
            override fun onNetworkStatusChanged(devId: String, status: Boolean) {}
            override fun onDevInfoUpdate(devId: String) {}
        })
        request.userType = memberInfoBean!!.userType
        request.userId = memberInfoBean!!.userId
        request.lockUserId = memberInfoBean!!.lockUserId
        request.unlockType = dpCode
        initData()
        initUi()
    }

    fun initData() {
        add_name_view = findViewById<EditText>(R.id.add_name)
        hijack_switch = findViewById<SwitchCompat>(R.id.hijack_switch)
        addView = findViewById<Button>(R.id.unlock_mode_add)
        add_password = findViewById<EditText>(R.id.add_password)
        show_code_view = findViewById<Button>(R.id.show_code_view)
    }

    fun initUi() {
        toolbar!!.title = getTypeName(this, dpCode!!)
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
            findViewById<View>(R.id.password_wrap).visibility = View.VISIBLE
            findViewById<View>(R.id.random_password).setOnClickListener { v: View? ->
                add_password!!.setText(
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
            if (!tuyaLockDevice!!.isProDevice && TextUtils.isEmpty(add_name_view!!.text)) {
                showTips(resources.getString(R.string.enter_unlock_mode_name), false)
                return@setOnClickListener
            }
            val loadingStr = "$addString..."
            addView!!.text = loadingStr
            addView!!.isEnabled = false
            validateOpModePassword()
        }
        show_code_view!!.setOnClickListener { v: View ->
            ShowCodeActivity.startActivity(
                v.context,
                JSONObject.toJSONString(detailBean)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (opModeId > 0) {
            tuyaLockDevice!!.getProUnlockOpModeDetail(
                opModeId,
                object : IThingResultCallback<OpModeDetailBean> {
                    override fun onSuccess(result: OpModeDetailBean) {
                        Log.i(Constant.TAG, JSONObject.toJSONString(result))
                        detailBean = result
                        request.unlockId = result.unlockId
                        request.opModeId = result.opModeId
                        request.unlockAttr = result.unlockAttr
                        request.unlockName = result.unlockName
                        request.notifyInfo = result.notifyInfo
                        request.userType = result.userType
                        request.userId = result.userId
                        request.lockUserId = result.lockUserId
                        show_code_view!!.visibility = View.VISIBLE
                        initUi()
                    }

                    override fun onError(errorCode: String, errorMessage: String) {
                        showTips(errorMessage, false)
                    }
                })
        }
    }

    private fun validateOpModePassword() {
        if (TextUtils.equals(dpCode, ThingUnlockType.PASSWORD)) {
            tuyaLockDevice!!.validateOpModePassword(
                request.password,
                object : IThingResultCallback<String?> {
                    override fun onSuccess(result: String?) {
                        val responseJSON = JSONObject.parseObject(result)
                        if (responseJSON.getBooleanValue("valid")) {
                            if (mFrom == 0) {
                                addUnlockMode()
                            } else {
                                upDataUnlockMode()
                            }
                        } else {
                            val errorCode = responseJSON.getString("errorCode")
                            Log.e(
                                Constant.TAG,
                                "validatePassword onSuccess is:false, errorCode:$errorCode"
                            )
                            showTips(errorCode, false)
                            addView!!.text = addString
                            addView!!.isEnabled = true
                            isAddMode = false
                        }
                    }

                    override fun onError(errorCode: String, errorMessage: String) {
                        Log.e(
                            Constant.TAG,
                            "validatePassword onError errorCode:$errorCode, errorMessage:$errorMessage"
                        )
                        showTips(errorCode, false)
                        addView!!.text = addString
                        addView!!.isEnabled = true
                        isAddMode = false
                    }
                })
        } else {
            if (mFrom == 0) {
                addUnlockMode()
            } else {
                upDataUnlockMode()
            }
        }
    }

    private fun addUnlockMode() {
        isAddMode = true
        tuyaLockDevice!!.addProUnlockOpModeForMember(
            request,
            object : IThingResultCallback<AddOpmodeResult?> {
                override fun onSuccess(result: AddOpmodeResult?) {
                    val tips = "add onSuccess"
                    showTips(tips, true)
                    Log.i(Constant.TAG, tips + JSONObject.toJSONString(result))
                    isAddMode = false

                    //同步解锁方式
                    val typeDpId = LockUtil.convertCode2Id(mDevId, dpCode)
                    val dpIds = ArrayList<String>()
                    dpIds.add(typeDpId)
                    tuyaLockDevice!!.syncData(dpIds, null)

                    //退出页面
                    finish()
                }

                override fun onError(errorCode: String, errorMessage: String) {
                    Log.e(
                        Constant.TAG,
                        "addProUnlockOpModeForMember:$errorMessage"
                    )
                    showTips(errorMessage, false)
                    addView!!.text = addString
                    addView!!.isEnabled = true
                    isAddMode = false
                }
            })
    }

    private fun upDataUnlockMode() {
        val notifyInfoBean = NotifyInfoBean()
        notifyInfoBean.isAppSend = true
        request.unlockId = detailBean!!.unlockId
        request.notifyInfo = notifyInfoBean
        tuyaLockDevice!!.modifyProUnlockOpModeForMember(
            request,
            object : IThingResultCallback<Boolean?> {
                override fun onSuccess(result: Boolean?) {
                    val tips = "update onSuccess"
                    showTips(tips, true)
                    Log.i(Constant.TAG, tips + JSONObject.toJSONString(result))
                    finish()
                }

                override fun onError(errorCode: String, errorMessage: String) {
                    showTips(errorMessage, false)
                    addView!!.text = addString
                    addView!!.isEnabled = true
                    Log.i(
                        Constant.TAG,
                        "upDataUnlockMode$errorMessage"
                    )
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
        val times = lockResponse.substring(10, 12).toInt(16)
        if (stage == BleLockConstant.STAGE_ENTERING) {
            if (times >= 0) {
                Log.i(Constant.TAG, "times:$times")
                val tipsAdd = "times: $times/5"
                showTips(tipsAdd, true)
            }
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
        IThingDevice!!.unRegisterDevListener()
        IThingDevice!!.onDestroy()
        memberInfoBean = null
    }

    private fun cancelUnlock() {
        tuyaLockDevice!!.cancelUnlockOpModeForFinger(
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
        if (dpCode == ThingUnlockType.FINGERPRINT && isAddMode) {
            showDelete(
                this, resources.getString(R.string.whether_to_cancel)
            ) { _: DialogInterface?, _: Int ->
                cancelUnlock()
                super@OpModeDetailActivity.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }
}