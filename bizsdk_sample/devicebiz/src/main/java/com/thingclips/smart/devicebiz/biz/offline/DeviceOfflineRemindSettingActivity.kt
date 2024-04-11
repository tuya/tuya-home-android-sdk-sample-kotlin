package com.thingclips.smart.devicebiz.biz.offline

import android.os.Bundle
import android.view.View
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.thingclips.smart.android.network.Business.ResultListener
import com.thingclips.smart.android.network.http.BusinessResponse
import com.thingclips.smart.device.DeviceBusinessDataManager
import com.thingclips.smart.device.offline.IDeviceOfflineReminderManager
import com.thingclips.smart.device.offline.bean.IsSupportOffLineBean
import com.thingclips.smart.device.offline.bean.OffLineStatusBean
import com.thingclips.smart.devicebiz.R

open class DeviceOfflineRemindSettingActivity : AppCompatActivity() {

    private lateinit var offlineStates: TextView
    private lateinit var switchOffline: Switch
    private lateinit var manager: IDeviceOfflineReminderManager
    private var deviceId: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_offline_remind)
        initView()
        initData()
    }

    private fun initData() {
        deviceId = intent.getStringExtra("deviceId")
        manager = DeviceBusinessDataManager.getInstance().getDeviceOfflineReminderManager()
        getOfflineReminderSupportStatus(deviceId)
    }

    private fun initView() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar_main)
        toolbar.title = resources.getString(R.string.device_offline_remind)
        offlineStates = findViewById<TextView>(R.id.offline_states)
        switchOffline = findViewById<Switch>(R.id.switch_offline)
        switchOffline.setOnCheckedChangeListener { buttonView, isChecked ->
            updateOfflineReminderStatus(deviceId, isChecked)
        }
    }

    private fun getOfflineReminderSupportStatus(deviceId: String?) {
        manager.getOfflineReminderSupportStatus(deviceId,
            object : ResultListener<IsSupportOffLineBean?> {
                override fun onFailure(
                    p0: BusinessResponse?,
                    p1: IsSupportOffLineBean?,
                    p2: String?
                ) {

                }

                override fun onSuccess(
                    p0: BusinessResponse?,
                    offLineBean: IsSupportOffLineBean?,
                    p2: String?
                ) {
                    if (offLineBean?.offlineReminder == true) {
                        switchOffline.visibility = View.VISIBLE
                        getOfflineReminderStatus(deviceId)
                    } else {
                        offlineStates.text = resources.getString(R.string.device_offline_remind_tip)
                        switchOffline.visibility = View.GONE
                    }
                }
            })
    }

    fun getOfflineReminderStatus(deviceId: String?) {
        manager.getOfflineReminderStatus(deviceId,
            object : ResultListener<ArrayList<OffLineStatusBean?>?> {
                override fun onFailure(
                    p0: BusinessResponse?,
                    p1: ArrayList<OffLineStatusBean?>?,
                    p2: String?
                ) {

                }

                override fun onSuccess(
                    p0: BusinessResponse?,
                    statusBeans: ArrayList<OffLineStatusBean?>?,
                    p2: String?
                ) {
                    if (!statusBeans.isNullOrEmpty()) {
                        if (statusBeans[0]?.enabled == true) {
                            offlineStates.text =
                                resources.getString(R.string.device_offline_remind) + " Open"
                            switchOffline.isChecked = true
                        } else {
                            offlineStates.text =
                                resources.getString(R.string.device_offline_remind) + " Close"
                            switchOffline.isChecked = false
                        }
                    }
                }
            })
    }

    private fun updateOfflineReminderStatus(deviceId: String?, isOpen: Boolean) {
        manager.updateOfflineReminderStatus(deviceId, isOpen, object : ResultListener<Boolean> {
            override fun onFailure(p0: BusinessResponse?, p1: Boolean?, p2: String?) {

            }

            override fun onSuccess(p0: BusinessResponse?, p1: Boolean?, p2: String?) {
                Toast.makeText(
                    this@DeviceOfflineRemindSettingActivity,
                    resources.getString(R.string.update_success),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }


}