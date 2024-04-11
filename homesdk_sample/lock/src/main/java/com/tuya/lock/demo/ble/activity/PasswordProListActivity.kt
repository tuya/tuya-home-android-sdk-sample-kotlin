package com.tuya.lock.demo.ble.activity

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingBleLockV2
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.lock.api.bean.ProTempPasswordItem
import com.thingclips.smart.optimus.lock.api.enums.ProPasswordListTypeEnum
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.optimus.lock.bean.ble.OnlinePasswordDeleteRequest
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.adapter.PasswordProListAdapter
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.DialogUtils.showDelete

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class PasswordProListActivity: AppCompatActivity() {

    companion object {
        fun startActivity(context: Context?, devId: String?) {
            val intent = Intent(context, PasswordProListActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            context?.startActivity(intent)
        }
    }

    private var tuyaLockDevice: IThingBleLockV2? = null
    private val adapter: PasswordProListAdapter by lazy {
        PasswordProListAdapter()
    }
    private val authTypes: MutableList<ProPasswordListTypeEnum> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_pro_offline_list)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        val mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        tuyaLockDevice = tuyaLockManager.getBleLockV2(mDevId)
        val rvList = findViewById<RecyclerView>(R.id.password_list)
        rvList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        authTypes.add(ProPasswordListTypeEnum.LOCK_BLUE_PASSWORD)
        authTypes.add(ProPasswordListTypeEnum.LOCK_OFFLINE_TEMP_PWD)
        authTypes.add(ProPasswordListTypeEnum.LOCK_TEMP_PWD)
        val list_type_main = findViewById<RadioGroup>(R.id.list_type_main)
        list_type_main.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            when (checkedId) {
                R.id.list_type_all -> {
                    authTypes.clear()
                    authTypes.add(ProPasswordListTypeEnum.LOCK_BLUE_PASSWORD)
                    authTypes.add(ProPasswordListTypeEnum.LOCK_OFFLINE_TEMP_PWD)
                    authTypes.add(ProPasswordListTypeEnum.LOCK_TEMP_PWD)
                }
                R.id.list_type_online -> {
                    authTypes.clear()
                    authTypes.add(ProPasswordListTypeEnum.LOCK_TEMP_PWD)
                }
                R.id.list_type_offline -> {
                    authTypes.clear()
                    authTypes.add(ProPasswordListTypeEnum.LOCK_OFFLINE_TEMP_PWD)
                }
            }
            getOfflineTempPasswordList()
        }
        adapter.setDevId(mDevId)
        adapter.delete(object : PasswordProListAdapter.Callback {
            override fun remove(passwordItem: ProTempPasswordItem, position: Int) {
                showDelete(
                    this@PasswordProListActivity
                ) { _: DialogInterface?, _: Int ->
                    deletePasscode(
                        passwordItem,
                        position
                    )
                }
            }
        })
        rvList.adapter = adapter
    }

    private fun deletePasscode(passwordItem: ProTempPasswordItem, position: Int) {
        val deleteRequest = OnlinePasswordDeleteRequest()
        deleteRequest.sn = passwordItem.sn
        deleteRequest.passwordId = passwordItem.unlockBindingId
        tuyaLockDevice!!.deleteProOnlinePassword(
            deleteRequest,
            object : IThingResultCallback<String> {
                override fun onSuccess(result: String) {
                    Log.i(
                        Constant.TAG,
                        "deleteProOnlineTempPassword success: $result"
                    )
                    Toast.makeText(applicationContext, "delete success", Toast.LENGTH_SHORT).show()
                    adapter.remove(position)
                }

                override fun onError(errorCode: String, errorMessage: String) {
                    Log.e(
                        Constant.TAG,
                        "deleteProOnlineTempPassword failed: code = $errorCode  message = $errorMessage"
                    )
                    Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onResume() {
        super.onResume()
        getOfflineTempPasswordList()
    }

    override fun onDestroy() {
        super.onDestroy()
        tuyaLockDevice!!.onDestroy()
    }

    /**
     * 获取门锁成员
     */
    private fun getOfflineTempPasswordList() {
        tuyaLockDevice!!.getProPasswordList(
            authTypes,
            object : IThingResultCallback<ArrayList<ProTempPasswordItem>> {
                override fun onSuccess(result: ArrayList<ProTempPasswordItem>) {
                    adapter.setData(result)
                    adapter.notifyDataSetChanged()
                }

                override fun onError(errorCode: String, errorMessage: String) {
                    Log.e(
                        Constant.TAG,
                        "getProTempPasswordList failed: code = $errorCode  message = $errorMessage"
                    )
                    Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
                }
            })
    }
}