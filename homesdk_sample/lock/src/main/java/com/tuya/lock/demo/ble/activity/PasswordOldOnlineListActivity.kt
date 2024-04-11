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
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingBleLockV2
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.optimus.lock.bean.ble.OnlinePasswordDeleteRequest
import com.thingclips.smart.sdk.optimus.lock.bean.ble.TempPasswordBeanV3
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.adapter.PasswordOldOnlineListAdapter
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.DialogUtils.showDelete

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class PasswordOldOnlineListActivity : AppCompatActivity() {

    companion object {
        fun startActivity(context: Context?, devId: String?, passwordType: String) {
            val intent = Intent(context, PasswordOldOnlineListActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            //Constant.TYPE_SINGLE
            intent.putExtra(Constant.PASSWORD_TYPE, passwordType)
            context?.startActivity(intent)
        }
    }

    private var tuyaLockDevice: IThingBleLockV2? = null
    private val adapter: PasswordOldOnlineListAdapter by lazy {
        PasswordOldOnlineListAdapter()
    }
    private var availTimes = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_old_online_list)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        val mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        val type = intent.getStringExtra(Constant.PASSWORD_TYPE)
        if (type == Constant.TYPE_SINGLE) {
            availTimes = 1
        } else if (type == Constant.TYPE_MULTIPLE) {
            availTimes = 0
        }
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        tuyaLockDevice = tuyaLockManager.getBleLockV2(mDevId)
        val rvList = findViewById<RecyclerView>(R.id.password_list)
        rvList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        adapter.setDevId(mDevId)
        adapter.delete(object : PasswordOldOnlineListAdapter.Callback {
            override fun remove(bean: TempPasswordBeanV3, position: Int) {
                showDelete(
                    this@PasswordOldOnlineListActivity
                ) { _: DialogInterface?, _: Int ->
                    deletePasscode(
                        bean,
                        position
                    )
                }
            }
        })
        rvList.adapter = adapter
        findViewById<View>(R.id.password_add).setOnClickListener { v: View ->
            PasswordOldOnlineDetailActivity.startActivity(
                v.context,
                null,
                mDevId,
                0,
                availTimes
            )
        }
    }

    private fun deletePasscode(tempPasswordBeanV3: TempPasswordBeanV3, position: Int) {
        val deleteRequest = OnlinePasswordDeleteRequest()
        deleteRequest.passwordId = tempPasswordBeanV3.passwordId.toString()
        deleteRequest.sn = tempPasswordBeanV3.sn
        tuyaLockDevice!!.deleteOnlinePassword(deleteRequest, object : IThingResultCallback<String> {
            override fun onSuccess(result: String) {
                Log.i(
                    Constant.TAG,
                    "deleteOnlineTempPassword onSuccess: $result"
                )
                Toast.makeText(applicationContext, "delete success", Toast.LENGTH_SHORT).show()
                if (null != adapter) {
                    adapter.remove(position)
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
        tuyaLockDevice!!.getOnlinePasswordList(
            availTimes,
            object : IThingResultCallback<ArrayList<TempPasswordBeanV3>> {
                override fun onSuccess(result: ArrayList<TempPasswordBeanV3>) {
                    Log.i(
                        Constant.TAG,
                        "getOnlineTempPasswordList success: $result"
                    )
                    adapter.setData(result)
                    adapter.notifyDataSetChanged()
                }

                override fun onError(errorCode: String, errorMessage: String) {
                    Log.e(
                        Constant.TAG,
                        "getOnlineTempPasswordList failed: code = $errorCode  message = $errorMessage"
                    )
                    Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
                }
            })
    }
}