package com.tuya.lock.demo.zigbee.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.thingclips.sdk.os.ThingOSDevice
import com.thingclips.smart.android.common.utils.L
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.lock.api.IThingZigBeeLock
import com.thingclips.smart.optimus.lock.api.zigbee.request.PasswordRemoveRequest
import com.thingclips.smart.optimus.lock.api.zigbee.request.PasswordRequest
import com.thingclips.smart.optimus.lock.api.zigbee.response.PasswordBean
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.optimus.lock.bean.ZigBeeDatePoint
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.DialogUtils
import com.tuya.lock.demo.zigbee.adapter.PasswordListAdapter

/**
 *
 * Created by HuiYao on 2024/3/1
 */
class PasswordListActivity:AppCompatActivity() {

    private var zigBeeLock: IThingZigBeeLock? = null
    private var adapter: PasswordListAdapter? = null
    private var password_list: RecyclerView? = null
    private var error_view: TextView? = null

    companion object{
        fun startActivity(context: Context, devId: String?) {
            val intent = Intent(context, PasswordListActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zigbee_password_list)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        val mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        zigBeeLock = tuyaLockManager.getZigBeeLock(mDevId)
        error_view = findViewById(R.id.error_view)
        password_list = findViewById(R.id.password_list)
        password_list?.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.VERTICAL,
            false
        )
        adapter = PasswordListAdapter()
        adapter!!.setDevId(mDevId)
        adapter!!.addCallback(object : PasswordListAdapter.Callback {
            override fun remove(bean: PasswordBean.DataBean?, position: Int) {
                DialogUtils.showDelete(
                    this@PasswordListActivity
                ) { dialog, which -> deletePasscode(bean, position) }
            }

            override fun freeze(bean: PasswordBean.DataBean?, position: Int, isFreeze: Boolean) {
                freezePasscode(bean, isFreeze)
            }

            override fun edit(bean: PasswordBean.DataBean?, position: Int) {
                PasswordDetailActivity.startEditActivity(this@PasswordListActivity, mDevId, bean)
            }
        })
        password_list?.adapter = adapter
        findViewById<View>(R.id.password_add).setOnClickListener { v: View ->
            PasswordDetailActivity.startActivity(
                v.context,
                mDevId,
                0
            )
        }
        findViewById<View>(R.id.password_add_one).setOnClickListener { v: View ->
            PasswordDetailActivity.startActivity(
                v.context,
                mDevId,
                1
            )
        }

        //是否支持一次性密码
        val deviceBean = ThingOSDevice.getDeviceBean(mDevId)
        if (deviceBean.getDpCodes().containsKey(ZigBeeDatePoint.SINGLE_USE_PASSWORD)) {
            findViewById<View>(R.id.password_add_one).visibility = View.VISIBLE
        } else {
            findViewById<View>(R.id.password_add_one).visibility = View.GONE
        }
        findViewById<View>(R.id.invalid_password_list).setOnClickListener { v: View ->
            PasswordInvalidListActivity.startActivity(
                v.context,
                mDevId
            )
        }
    }

    private fun deletePasscode(dataBean: PasswordBean.DataBean?, position: Int) {
        val removeRequest = PasswordRemoveRequest()
        removeRequest.id = dataBean!!.id
        removeRequest.oneTime = dataBean.oneTime
        removeRequest.name = dataBean.name
        removeRequest.effectiveTime = dataBean.effectiveTime
        removeRequest.invalidTime = dataBean.invalidTime
        zigBeeLock!!.removeTemporaryPassword(removeRequest, object : IThingResultCallback<String> {
            override fun onSuccess(result: String) {
                Log.i(Constant.TAG, "deleteOnlineTempPassword onSuccess: $result")
                Toast.makeText(applicationContext, "delete success", Toast.LENGTH_SHORT).show()
                if (null != adapter) {
                    adapter!!.remove(position)
                }
            }

            override fun onError(errorCode: String, errorMessage: String) {
                Log.e(
                    Constant.TAG,
                    "deleteOnlineTempPassword failed: code = $errorCode  message = $errorMessage"
                )
                Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun freezePasscode(dataBean: PasswordBean.DataBean?, isFreeze: Boolean) {
        val passwordRequest = PasswordRequest()
        passwordRequest.password = dataBean!!.password
        passwordRequest.name = dataBean.name
        passwordRequest.effectiveTime = dataBean.effectiveTime
        passwordRequest.invalidTime = dataBean.invalidTime
        passwordRequest.oneTime = dataBean.oneTime
        passwordRequest.id = dataBean.id
        if (isFreeze) {
            zigBeeLock!!.freezeTemporaryPassword(
                passwordRequest,
                object : IThingResultCallback<String> {
                    override fun onSuccess(result: String) {
                        L.i(
                            Constant.TAG,
                            "freezeTemporaryPassword onSuccess: $result"
                        )
                        Toast.makeText(applicationContext, "freeze success", Toast.LENGTH_SHORT)
                            .show()
                    }

                    override fun onError(errorCode: String, errorMessage: String) {
                        L.e(
                            Constant.TAG,
                            "freezeTemporaryPassword failed: code = $errorCode  message = $errorMessage"
                        )
                        Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                })
            return
        }
        zigBeeLock!!.unfreezeTemporaryPassword(
            passwordRequest,
            object : IThingResultCallback<String> {
                override fun onSuccess(result: String) {
                    L.i(
                        Constant.TAG,
                        "unfreezeTemporaryPassword onSuccess: $result"
                    )
                    Toast.makeText(applicationContext, "unfreeze success", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onError(errorCode: String, errorMessage: String) {
                    L.e(
                        Constant.TAG,
                        "unfreezeTemporaryPassword failed: code = $errorCode  message = $errorMessage"
                    )
                    Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onResume() {
        super.onResume()
        getOfflineTempPasswordList()
    }

    /**
     * 密码列表
     */
    private fun getOfflineTempPasswordList() {
        zigBeeLock!!.getPasswordList(0, 50, object : IThingResultCallback<PasswordBean> {
            override fun onSuccess(result: PasswordBean) {
                Log.i(Constant.TAG, "getOnlineTempPasswordList success: $result")
                adapter!!.setData(result.datas)
                adapter!!.notifyDataSetChanged()
                if (result.datas.size == 0) {
                    showError("No content")
                } else {
                    password_list!!.visibility = View.VISIBLE
                    error_view!!.visibility = View.GONE
                }
            }

            override fun onError(errorCode: String, errorMessage: String) {
                Log.e(
                    Constant.TAG,
                    "getOnlineTempPasswordList failed: code = $errorCode  message = $errorMessage"
                )
                showError(errorMessage)
            }
        })
    }

    private fun showError(msg: String) {
        password_list!!.visibility = View.GONE
        error_view!!.visibility = View.VISIBLE
        error_view!!.text = msg
    }
}