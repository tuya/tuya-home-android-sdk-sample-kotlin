package com.tuya.lock.demo.ble.activity

import android.content.Context
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
import com.thingclips.smart.optimus.lock.api.bean.OfflineTempPasswordItem
import com.thingclips.smart.optimus.lock.api.enums.OfflineTempPasswordStatus
import com.thingclips.smart.optimus.lock.api.enums.OfflineTempPasswordType
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.adapter.PasswordOldOfflineListAdapter
import com.tuya.lock.demo.common.constant.Constant

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class PasswordOldOfflineListActivity: AppCompatActivity() {

    companion object {
        fun startActivity(context: Context?, devId: String?, passwordType: String) {
            val intent = Intent(context, PasswordOldOfflineListActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            intent.putExtra(Constant.PASSWORD_TYPE, passwordType)
            context?.startActivity(intent)
        }
    }

    private var tuyaLockDevice: IThingBleLockV2? = null
    private val adapter: PasswordOldOfflineListAdapter by lazy {
        PasswordOldOfflineListAdapter()
    }
    private var selectType = OfflineTempPasswordType.MULTIPLE
    private var selectStatus = OfflineTempPasswordStatus.TO_BE_USED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_old_offline_list)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        val mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        val type = intent.getStringExtra(Constant.PASSWORD_TYPE)
        when (type) {
            Constant.TYPE_SINGLE -> selectType = OfflineTempPasswordType.SINGLE
            Constant.TYPE_MULTIPLE -> selectType = OfflineTempPasswordType.MULTIPLE
            Constant.TYPE_CLEAR_ALL -> selectType = OfflineTempPasswordType.CLEAR_ALL
        }
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        tuyaLockDevice = tuyaLockManager.getBleLockV2(mDevId)
        findViewById<View>(R.id.password_offline_add).setOnClickListener { v: View ->
            val intent = Intent(
                v.context,
                PasswordOldOfflineAddActivity::class.java
            )
            intent.putExtra(Constant.DEVICE_ID, mDevId)
            intent.putExtra(Constant.PASSWORD_TYPE, type)
            v.context.startActivity(intent)
        }
        val rvList = findViewById<RecyclerView>(R.id.password_list)
        rvList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        val password_offline_state_main = findViewById<RadioGroup>(R.id.password_offline_state_main)
        password_offline_state_main.setOnCheckedChangeListener { group: RadioGroup?, checkedId: Int ->
            if (checkedId == R.id.password_offline_state_to_be_used) {
                selectStatus = OfflineTempPasswordStatus.TO_BE_USED
            } else if (checkedId == R.id.password_offline_state_used) {
                selectStatus = OfflineTempPasswordStatus.USED
            } else if (checkedId == R.id.password_offline_state_expired) {
                selectStatus = OfflineTempPasswordStatus.EXPIRED
            }
            getOfflineTempPasswordList()
        }
        adapter.setDevId(mDevId)
        rvList.adapter = adapter
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
        tuyaLockDevice!!.getOfflinePasswordList(
            selectType,
            0,
            10,
            selectStatus,
            object : IThingResultCallback<ArrayList<OfflineTempPasswordItem>> {
                override fun onSuccess(result: ArrayList<OfflineTempPasswordItem>) {
                    Log.i(
                        Constant.TAG,
                        "getOfflineTempPasswordList success: $result"
                    )
                    adapter.setData(result)
                    adapter.notifyDataSetChanged()
                }

                override fun onError(errorCode: String, errorMessage: String) {
                    Log.e(
                        Constant.TAG,
                        "getOfflineTempPasswordList failed: code = $errorCode  message = $errorMessage"
                    )
                    Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
                }
            })
    }
}