package com.tuya.lock.demo.zigbee.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.home.sdk.anntation.MemberRole
import com.thingclips.smart.home.sdk.bean.MemberBean
import com.thingclips.smart.home.sdk.bean.MemberWrapperBean
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.lock.api.IThingZigBeeLock
import com.thingclips.smart.optimus.lock.api.zigbee.response.MemberInfoBean
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.api.IResultCallback
import com.thingclips.smart.sdk.api.IThingDataCallback
import com.tuya.appsdk.sample.resource.HomeModel
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant

/**
 *
 * Created by HuiYao on 2024/3/1
 */
class MemberDetailActivity:AppCompatActivity() {

    private var userBean: MemberInfoBean? = null
    private var mFrom = 0

    private var nameView: EditText? = null
    private var account_View: EditText? = null
    private var role_View: EditText? = null
    private var countryCode_view: EditText? = null

    private var zigBeeLock: IThingZigBeeLock? = null

    private var memberWrapperBean: MemberWrapperBean.Builder? = null

    companion object{
        fun startActivity(
            context: Context,
            memberInfoBean: MemberInfoBean?,
            devId: String?,
            from: Int
        ) {
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
        setContentView(R.layout.activity_member_add)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        val userData = intent.getStringExtra(Constant.USER_DATA)
        val mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        mFrom = intent.getIntExtra(Constant.FROM, 0)
        try {
            userBean = JSONObject.parseObject(
                userData,
                MemberInfoBean::class.java
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
        }
        if (null == userBean) {
            userBean = MemberInfoBean()
        }
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        zigBeeLock = tuyaLockManager.getZigBeeLock(mDevId)
        memberWrapperBean = MemberWrapperBean.Builder()
        if (mFrom == 1) {
            toolbar.title = resources.getString(R.string.submit_edit)
        } else {
            toolbar.title = resources.getString(R.string.user_add)
        }
        /**
         * 用户昵称
         */
        nameView = findViewById(R.id.nameView)
        nameView?.setText(userBean!!.nickName)
        /**
         * 受邀账号
         */
        account_View = findViewById(R.id.account_View)
        if (mFrom == 1) {
            account_View?.isEnabled = false
            findViewById<View>(R.id.account_wrap).visibility = View.GONE
            findViewById<View>(R.id.account_line).visibility = View.GONE
            findViewById<View>(R.id.countryCode_wrap).visibility = View.GONE
            findViewById<View>(R.id.countryCode_line).visibility = View.GONE
        } else {
            findViewById<View>(R.id.account_wrap).visibility = View.VISIBLE
            findViewById<View>(R.id.account_line).visibility = View.VISIBLE
            findViewById<View>(R.id.countryCode_wrap).visibility = View.VISIBLE
            findViewById<View>(R.id.countryCode_line).visibility = View.VISIBLE
            account_View?.isEnabled = true
        }
        /**
         * 账号ID
         * 10. 管理员 20.普通成员. 30. 没有名字的成员. 50. 家庭拥有者
         */
        role_View = findViewById(R.id.role_View)
        var userType = -999
        when (userBean!!.userType) {
            50 -> {
                userType = MemberRole.ROLE_OWNER
            }
            10 -> {
                userType = MemberRole.ROLE_ADMIN
            }
            20 -> {
                userType = MemberRole.ROLE_MEMBER
            }
            30 -> {
                userType = MemberRole.ROLE_CUSTOM
            }
        }
        if (userBean!!.userType != 0) {
            role_View?.setText(userType.toString())
            memberWrapperBean!!.setRole(userType)
        }
        if (!TextUtils.isEmpty(userBean!!.userId)) {
            try {
                memberWrapperBean!!.setMemberId(userBean!!.userId.toLong())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        /**
         * 是否需要受邀请者同意接受加入家庭邀请
         */
        memberWrapperBean!!.setAutoAccept(true)
        val autoAccept_wrap = findViewById<RadioGroup>(R.id.autoAccept_wrap)
        autoAccept_wrap.setOnCheckedChangeListener { group: RadioGroup?, checkedId: Int ->
            if (checkedId == R.id.autoAccept_yes) {
                memberWrapperBean!!.setAutoAccept(true)
            } else {
                memberWrapperBean!!.setAutoAccept(false)
            }
        }
        if (mFrom == 1) {
            findViewById<View>(R.id.autoAccept_main).visibility = View.GONE
        } else {
            findViewById<View>(R.id.autoAccept_main).visibility = View.VISIBLE
        }
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
        countryCode_view = findViewById(R.id.countryCode_view)
    }


    override fun onDestroy() {
        super.onDestroy()
    }

    /**
     * 用户更新信息
     */
    private fun addLockUser() {
        if (!TextUtils.isEmpty(nameView!!.text)) {
            memberWrapperBean!!.setNickName(nameView!!.text.toString().trim { it <= ' ' })
        }
        if (!TextUtils.isEmpty(account_View!!.text) && !TextUtils.isEmpty(
                countryCode_view!!.text
            )
        ) {
            memberWrapperBean!!.setCountryCode(
                countryCode_view!!.text.toString().trim { it <= ' ' })
            memberWrapperBean!!.setAccount(account_View!!.text.toString().trim { it <= ' ' })
        }
        if (!TextUtils.isEmpty(role_View!!.text)) {
            memberWrapperBean!!.setRole(role_View!!.text.toString().trim { it <= ' ' }.toInt())
        }
        memberWrapperBean!!.setHomeId(HomeModel.INSTANCE.getCurrentHome(this))
        zigBeeLock!!.addMember(
            memberWrapperBean!!.build(),
            object : IThingDataCallback<MemberBean?> {
                override fun onSuccess(result: MemberBean?) {
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
        memberWrapperBean!!.setNickName(nameView!!.text.toString().trim { it <= ' ' })
        memberWrapperBean!!.setRole(role_View!!.text.toString().trim { it <= ' ' }.toInt())
        zigBeeLock!!.updateMember(memberWrapperBean!!.build(), object : IResultCallback {
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

            override fun onSuccess() {
                Log.i(Constant.TAG, "update lock user success")
                Toast.makeText(applicationContext, "update lock user success", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        })
    }
}