package com.tuya.lock.demo.zigbee.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.thingclips.sdk.os.ThingOSDevice
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.optimus.lock.api.IThingLockManager
import com.thingclips.smart.optimus.lock.api.IThingZigBeeLock
import com.thingclips.smart.optimus.lock.api.zigbee.request.RemotePermissionEnum
import com.thingclips.smart.optimus.lock.api.zigbee.response.MemberInfoBean
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.thingclips.smart.sdk.api.IDevListener
import com.thingclips.smart.sdk.api.IResultCallback
import com.thingclips.smart.sdk.api.IThingDataCallback
import com.thingclips.smart.sdk.api.IThingDevice
import com.thingclips.smart.sdk.optimus.lock.bean.ZigBeeDatePoint
import com.thingclips.smart.sdk.optimus.lock.utils.StandardDpConverter
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.activity.DeleteDeviceActivity
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.DialogUtils
import com.tuya.lock.demo.common.view.LockButtonProgressView
import java.util.Locale

/**
 *
 * Created by HuiYao on 2024/3/1
 */
class DeviceDetail: AppCompatActivity() {

    private var guard_view: TextView? = null
    private var unlock_btn: LockButtonProgressView? = null
    private var closed_door_view: TextView? = null
    private var anti_lock_view: TextView? = null
    private var child_lock_view: TextView? = null
    private var power_view: TextView? = null
    private var alarm_record_view: TextView? = null
    private var door_record_view: TextView? = null
    private var temporary_password_view: TextView? = null
    private var member_list_view: TextView? = null
    private var setting_view: TextView? = null
    private var dynamic_password_view: TextView? = null
    private var mDevId: String? = null
    private var zigBeeLock: IThingZigBeeLock? = null
    private var isRemoteOpen = 0 //0 免密正常、-1 无此功能、-2 无权限、1 密钥开门 -3 网络异常

    private var ITuyaDevice: IThingDevice? = null
    private var isLoadNum = false

    private var isOpen = false

    companion object{
        fun startActivity(context: Context?, devId: String?) {
            val intent = Intent(context, DeviceDetail::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            context?.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zigbee_device_detail)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        mDevId = intent.getStringExtra(Constant.DEVICE_ID)
        val tuyaLockManager = ThingOptimusSdk.getManager(
            IThingLockManager::class.java
        )
        zigBeeLock = tuyaLockManager.getZigBeeLock(mDevId)
        ITuyaDevice = ThingHomeSdk.newDeviceInstance(mDevId)
        ITuyaDevice?.registerDevListener(object : IDevListener {
            override fun onDpUpdate(devId: String, dpStr: String) {
                val dpData = StandardDpConverter.convertIdToCodeMap(
                    dpStr,
                    StandardDpConverter.getSchemaMap(mDevId)
                )
                dealAddUnlockMode(dpData)
            }

            override fun onRemoved(devId: String) {}
            override fun onStatusChanged(devId: String, online: Boolean) {
                deviceOnline()
            }

            override fun onNetworkStatusChanged(devId: String, status: Boolean) {}
            override fun onDevInfoUpdate(devId: String) {}
        })
        initView()
        deviceOnline()
    }


    private fun dealAddUnlockMode(dpData: Map<String, Any>) {
        for (key in dpData.keys) {
            val lockResponse = dpData[key].toString()
            when (key) {
                ZigBeeDatePoint.REMOTE_RESULT -> doorOpenSuccess()
                ZigBeeDatePoint.CLOSED_OPENED -> checkDoorOpen(lockResponse)
                ZigBeeDatePoint.REVERSE_LOCK -> checkReverseOpen(
                    java.lang.Boolean.parseBoolean(
                        lockResponse
                    )
                )

                ZigBeeDatePoint.CHILD_LOCK -> checkChildOpen(
                    java.lang.Boolean.parseBoolean(
                        lockResponse
                    )
                )

                ZigBeeDatePoint.RESIDUAL_ELECTRICITY -> checkResidual(lockResponse.toInt())
                ZigBeeDatePoint.HI_JACK, ZigBeeDatePoint.ALARM_LOCK, ZigBeeDatePoint.DOORBELL -> {
                    //有告警记录上报，查询未读数
                    if (isLoadNum) {
                        return
                    }
                    isLoadNum = true
                    getUnRead()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        initData()
    }

    override fun onDestroy() {
        super.onDestroy()
        zigBeeLock!!.onDestroy()
        ITuyaDevice!!.onDestroy()
    }

    private fun initView() {
        guard_view = findViewById(R.id.guard_view)
        unlock_btn = findViewById(R.id.unlock_btn)
        closed_door_view = findViewById(R.id.closed_door_view)
        anti_lock_view = findViewById(R.id.anti_lock_view)
        child_lock_view = findViewById(R.id.child_lock_view)
        power_view = findViewById(R.id.power_view)
        alarm_record_view = findViewById(R.id.alarm_record_view)
        door_record_view = findViewById(R.id.door_record_view)
        temporary_password_view = findViewById(R.id.temporary_password_view)
        setting_view = findViewById(R.id.setting_view)
        member_list_view = findViewById(R.id.member_list_view)
        dynamic_password_view = findViewById(R.id.dynamic_password_view)
        val numStr = String.format(Locale.CHINA, getString(R.string.alarm_records), "")
        alarm_record_view?.text = numStr
    }

    private fun initData() {
        val deviceBean = ThingOSDevice.getDeviceBean(mDevId)
        //未读消息
        getUnRead()
        zigBeeLock!!.getSecurityGuardDays(object : IThingResultCallback<String?> {
            override fun onSuccess(result: String?) {
                val guard = String.format(
                    Locale.CHINA,
                    getString(R.string.zigbee_security_guard_days),
                    result
                )
                guard_view!!.post { guard_view!!.text = guard }
            }

            override fun onError(errorCode: String, errorMessage: String) {}
        })
        unlock_btn?.addClickCallback(object : LockButtonProgressView.ClickCallback{
            override fun doLongPress() {
                if (isRemoteOpen == 0) {
                    remoteUnlock()
                } else if (isRemoteOpen == 1) {
                    runOnUiThread {
                        DialogUtils.showInputEdit(this@DeviceDetail, object : DialogUtils.InputCallback {
                            override fun input(password: String) {
                                remotePasswordUnlock(password)
                            }

                            override fun close() {
                                unlock_btn?.setTitle(getString(R.string.zigbee_unlock_open))
                                unlock_btn?.setProgress(0)
                                unlock_btn?.isEnabled = true
                            }
                        })
                    }
                }
            }

        })
        zigBeeLock!!.fetchRemoteUnlockType(object : IThingResultCallback<Boolean?> {
            override fun onSuccess(result: Boolean?) {
                //远程开门关闭
                if (!result!!) {
                    isRemoteOpen = -1
                    deviceOnline()
                    return
                }
                //无远程含密开门权限
                if (TextUtils.isEmpty(zigBeeLock!!.convertCode2Id(ZigBeeDatePoint.REMOTE_UNLOCK))) {
                    isRemoteOpen = 0
                    deviceOnline()
                    return
                }
                zigBeeLock!!.getRemoteUnlockPermissionValue(object :
                    IThingResultCallback<RemotePermissionEnum?> {
                    override fun onSuccess(result: RemotePermissionEnum?) {
                        when (result) {
                            RemotePermissionEnum.REMOTE_NOT_DP_KEY_ADMIN -> zigBeeLock!!.getMemberInfo(
                                object : IThingDataCallback<MemberInfoBean> {
                                    override fun onSuccess(result: MemberInfoBean) {
                                        isRemoteOpen =
                                            if (result.userType == 10 || result.userType == 50) {
                                                //只有管理员能远程免密开门
                                                0
                                            } else {
                                                //无法使用
                                                -2
                                            }
                                        deviceOnline()
                                    }

                                    override fun onError(errorCode: String, errorMessage: String) {
                                        isRemoteOpen = -3
                                        deviceOnline()
                                    }
                                })

                            RemotePermissionEnum.REMOTE_NOT_DP_KEY_ALL -> {
                                //所有人可以免密开门
                                isRemoteOpen = 0
                                deviceOnline()
                            }

                            RemotePermissionEnum.REMOTE_UNLOCK_ADMIN -> zigBeeLock!!.getMemberInfo(
                                object : IThingDataCallback<MemberInfoBean> {
                                    override fun onSuccess(result: MemberInfoBean) {
                                        //只有管理员可以含密开门
                                        isRemoteOpen =
                                            if (result.userType == 10 || result.userType == 50) {
                                                1
                                            } else {
                                                //其他人无法操作
                                                -2
                                            }
                                        deviceOnline()
                                    }

                                    override fun onError(errorCode: String, errorMessage: String) {
                                        isRemoteOpen = -3
                                        deviceOnline()
                                    }
                                })

                            RemotePermissionEnum.REMOTE_UNLOCK_ALL -> {
                                //所有人含密开门
                                isRemoteOpen = 1
                                deviceOnline()
                            }

                            else -> {}
                        }
                    }

                    override fun onError(errorCode: String, errorMessage: String) {
                        isRemoteOpen = 0
                        deviceOnline()
                    }
                })
            }

            override fun onError(errorCode: String, errorMessage: String) {
                unlock_btn?.isEnabled = false
                unlock_btn?.setTitle(errorMessage)
            }
        })

        //门是否关闭
        if (null != deviceBean && deviceBean.getDpCodes()
                .containsKey(ZigBeeDatePoint.CLOSED_OPENED)
        ) {
            val closedCode = deviceBean.getDpCodes()[ZigBeeDatePoint.CLOSED_OPENED] as String?
            checkDoorOpen(closedCode)
            closed_door_view!!.visibility = View.VISIBLE
        } else {
            closed_door_view!!.visibility = View.GONE
        }

        //是否反锁
        if (null != deviceBean && deviceBean.getDpCodes()
                .containsKey(ZigBeeDatePoint.REVERSE_LOCK)
        ) {
            val reverseCode = deviceBean.getDpCodes()[ZigBeeDatePoint.REVERSE_LOCK] as Boolean?
            checkReverseOpen(reverseCode)
            anti_lock_view!!.visibility = View.VISIBLE
        } else {
            anti_lock_view!!.visibility = View.GONE
        }

        //是否童锁
        if (null != deviceBean && deviceBean.getDpCodes().containsKey(ZigBeeDatePoint.CHILD_LOCK)) {
            val childCode = deviceBean.getDpCodes()[ZigBeeDatePoint.CHILD_LOCK] as Boolean?
            checkChildOpen(childCode)
            child_lock_view!!.visibility = View.VISIBLE
        } else {
            child_lock_view!!.visibility = View.GONE
        }

        //电量
        if (null != deviceBean && deviceBean.getDpCodes()
                .containsKey(ZigBeeDatePoint.RESIDUAL_ELECTRICITY)
        ) {
            val residualCode = deviceBean.getDpCodes()[ZigBeeDatePoint.RESIDUAL_ELECTRICITY] as Int?
            checkResidual(residualCode)
        } else {
            power_view!!.text = getString(R.string.zigbee_battery_not_support)
        }
        /**
         * 告警记录
         */
        alarm_record_view!!.setOnClickListener { v: View? ->
            isLoadNum = false
            AlarmRecordListActivity.startActivity(
                this@DeviceDetail,
                mDevId
            )
        }
        /**
         * 开门记录
         */
        door_record_view!!.setOnClickListener { v: View? ->
            DoorRecordListActivity.startActivity(
                this@DeviceDetail,
                mDevId
            )
        }
        /**
         * 临时密码
         */
        temporary_password_view!!.setOnClickListener { v: View? ->
            PasswordListActivity.startActivity(
                this@DeviceDetail,
                mDevId
            )
        }
        /**
         * 动态密码
         */
        dynamic_password_view!!.setOnClickListener { v: View? ->
            PasswordDynamicActivity.startActivity(
                this@DeviceDetail,
                mDevId
            )
        }
        /**
         * 家庭成员入口
         */
        member_list_view!!.setOnClickListener { v: View? ->
            MemberListActivity.startActivity(
                this@DeviceDetail,
                mDevId
            )
        }
        /**
         * 设置
         */
        setting_view!!.setOnClickListener { v: View? ->
            SettingActivity.startActivity(
                this@DeviceDetail,
                mDevId
            )
        }
        /**
         * 解绑
         */
        findViewById<View>(R.id.device_delete).setOnClickListener { v: View ->
            DeleteDeviceActivity.startActivity(
                v.context,
                mDevId
            )
        }
    }

    private fun deviceOnline() {
        val deviceBean = ThingOSDevice.getDeviceBean(mDevId)
        //是否开启远程开门的功能
        if (isRemoteOpen == 0 || isRemoteOpen == 1) {
            unlock_btn?.isEnabled = deviceBean.isOnline
            //设备是否在线
            if (deviceBean.isOnline) {
                unlock_btn?.setTitle(getString(R.string.zigbee_unlock_open))
            } else {
                unlock_btn?.setTitle(getString(R.string.zigbee_device_offline))
            }
        } else if (isRemoteOpen == -2) {
            unlock_btn?.isEnabled = false
            unlock_btn?.setTitle(getString(R.string.zigbee_insufficient_permissions))
        } else if (isRemoteOpen == -3) {
            unlock_btn?.isEnabled = false
            unlock_btn?.setTitle(getString(R.string.zigbee_network_error))
        } else {
            unlock_btn?.isEnabled = false
            unlock_btn?.setTitle(getString(R.string.zigbee_not_enabled))
        }
    }

    private fun checkDoorOpen(closedOpened: String?) {
        //门是否关闭
        if (TextUtils.equals(closedOpened, "open")) {
            closed_door_view!!.text = getString(R.string.zigbee_door_not_closed)
        } else {
            closed_door_view!!.text = getString(R.string.zigbee_door_is_closed)
        }
    }

    private fun checkReverseOpen(reverseCode: Boolean?) {
        if (reverseCode!!) {
            anti_lock_view!!.text = getString(R.string.zigbee_door_locked)
        } else {
            anti_lock_view!!.text = getString(R.string.zigbee_door_not_locked)
        }
    }

    private fun checkChildOpen(childCode: Boolean?) {
        if (childCode!!) {
            child_lock_view!!.text = getString(R.string.zigbee_child_lock_open)
        } else {
            child_lock_view!!.text = getString(R.string.zigbee_child_lock_off)
        }
    }

    private fun checkResidual(residualCode: Int?) {
        val residual =
            String.format(Locale.CHINA, getString(R.string.zigbee_power), residualCode.toString())
        power_view!!.text = residual
    }

    private fun remoteUnlock() {
        unlock_btn?.setTitle("Loading")
        unlock_btn?.isEnabled = false
        zigBeeLock!!.remoteUnlock(object : IResultCallback {
            override fun onError(code: String, error: String) {
                showToast(error)
                unlock_btn?.setTitle(getString(R.string.zigbee_unlock_open))
                unlock_btn?.setProgress(0)
                unlock_btn?.isEnabled = true
            }

            override fun onSuccess() {
                isOpen = true
            }
        })
    }

    private fun getUnRead() {
        zigBeeLock!!.getUnreadAlarmNumber(object : IThingResultCallback<String?> {
            override fun onSuccess(result: String?) {
                if (!TextUtils.equals(result, "0")) {
                    val numStr =
                        String.format(Locale.CHINA, getString(R.string.alarm_records), "(new)")
                    alarm_record_view!!.post { alarm_record_view!!.text = numStr }
                }
            }

            override fun onError(errorCode: String, errorMessage: String) {}
        })
    }

    private fun showToast(msg: String) {
        Toast.makeText(this@DeviceDetail, msg, Toast.LENGTH_SHORT).show()
    }

    private fun remotePasswordUnlock(password: String) {
        unlock_btn?.setTitle("Loading")
        unlock_btn?.isEnabled = false
        zigBeeLock?.remoteUnlock(password, object : IResultCallback {
            override fun onError(code: String, error: String) {
                showToast(error)
                unlock_btn?.setTitle(getString(R.string.zigbee_unlock_open))
                unlock_btn?.setProgress(0)
                unlock_btn?.isEnabled = true
            }

            override fun onSuccess() {
                isOpen = true
            }
        })
    }

    /**
     * 开门成功提示
     */
    private fun doorOpenSuccess() {
        if (isOpen) {
            showToast(getString(R.string.zigbee_operation_suc))
            unlock_btn?.setProgress(0)
            unlock_btn?.setTitle(getString(R.string.zigbee_unlock_open))
            unlock_btn?.isEnabled = true
            isOpen = false
        }
    }
}