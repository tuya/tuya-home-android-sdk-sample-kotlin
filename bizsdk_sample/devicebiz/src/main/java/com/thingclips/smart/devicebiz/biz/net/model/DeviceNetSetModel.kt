package com.thingclips.smart.devicebiz.biz.net.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.thingclips.sdk.core.PluginManager
import com.thingclips.smart.device.DeviceBusinessDataManager
import com.thingclips.smart.device.net.IDeviceNetSetManager
import com.thingclips.smart.interior.api.IThingDevicePlugin
import com.thingclips.smart.sdk.api.IThingDataCallback
import com.thingclips.smart.sdk.api.wifibackup.api.bean.BackupWifiBean
import com.thingclips.smart.sdk.api.wifibackup.api.bean.BackupWifiListInfo
import com.thingclips.smart.sdk.api.wifibackup.api.bean.BackupWifiResultBean
import com.thingclips.smart.sdk.api.wifibackup.api.bean.CurrentWifiInfoBean
import com.thingclips.smart.sdk.api.wifibackup.api.bean.SwitchWifiResultBean
import com.thingclips.smart.sdk.bean.DeviceBean

class DeviceNetSetModel(application: Application) : AndroidViewModel(application) {

    private var deviceId: String? = null
    var deviceBean: DeviceBean? = null
    private var manager: IDeviceNetSetManager? = null

    var mDeviceBackupWiFiList: MutableLiveData<List<BackupWifiBean>> =
        MutableLiveData<List<BackupWifiBean>>()

    var mDeviceCurrentWifi: MutableLiveData<CurrentWifiInfoBean?> =
        MutableLiveData<CurrentWifiInfoBean?>()

    var canSwitchDeviceWiFi: MutableLiveData<Boolean> =
        MutableLiveData<Boolean>()

    var canUpdateDeviceBackupWiFiList: MutableLiveData<Boolean> =
        MutableLiveData<Boolean>()

    var updateDeviceBackupWiFiListResult: MutableLiveData<Boolean> =
        MutableLiveData<Boolean>()

    var switchToBackupWifiResult: MutableLiveData<Boolean> =
        MutableLiveData<Boolean>()

    var switchToNewWifiResult: MutableLiveData<Boolean> =
        MutableLiveData<Boolean>()

    var isSupportBackupNetwork: MutableLiveData<Boolean> =
        MutableLiveData<Boolean>()

    var maxBackupWifiNum: MutableLiveData<String> =
        MutableLiveData<String>()

    fun init(deviceId: String) {
        this.deviceId = deviceId
        // 获取 DeviceBean
        val plugin = PluginManager.service(IThingDevicePlugin::class.java)
        deviceBean = plugin.devListCacheManager.getDev(deviceId)

        manager = DeviceBusinessDataManager.getInstance().getDeviceNetSetManager(deviceId)
    }

    fun getDeviceBackupWiFiList() {
        manager?.getDeviceBackupWiFiList(object : IThingDataCallback<BackupWifiListInfo?> {
            override fun onSuccess(result: BackupWifiListInfo?) {
                maxBackupWifiNum.value = result?.maxNum
                mDeviceBackupWiFiList.value = result?.backupList ?: ArrayList()
            }

            override fun onError(errorCode: String?, errorMessage: String?) {
                maxBackupWifiNum.value = "0"
                mDeviceBackupWiFiList.value = ArrayList()
            }

        })
    }

    fun getDeviceCurrentWifi() {
        manager?.getDeviceCurrentNetInfo(object : IThingDataCallback<CurrentWifiInfoBean?> {
            override fun onSuccess(result: CurrentWifiInfoBean?) {
                mDeviceCurrentWifi.value = result
            }

            override fun onError(errorCode: String?, errorMessage: String?) {
                mDeviceCurrentWifi.value = null
            }

        })
    }

    fun canSwitchDeviceWiFi(bean: CurrentWifiInfoBean) {
        canSwitchDeviceWiFi.value = manager?.canSwitchDeviceWiFi(bean)
    }


    fun canUpdateDeviceBackupWiFiList(bean: CurrentWifiInfoBean) {
        canUpdateDeviceBackupWiFiList.value = manager?.canUpdateDeviceBackupWiFiList(bean)
    }

    fun updateDeviceBackupWiFiList(backupWifiList: List<BackupWifiBean>?) {
        manager?.updateDeviceBackupWiFiList(backupWifiList,
            object : IThingDataCallback<BackupWifiResultBean?> {
                override fun onSuccess(result: BackupWifiResultBean?) {
                    updateDeviceBackupWiFiListResult.value = true
                }

                override fun onError(errorCode: String?, errorMessage: String?) {
                    updateDeviceBackupWiFiListResult.value = false
                }

            })
    }

    fun switchToBackupWifi(hash: String?) {
        manager?.switchToBackupWifi(hash, object : IThingDataCallback<SwitchWifiResultBean?> {
            override fun onSuccess(result: SwitchWifiResultBean?) {
                switchToBackupWifiResult.value = true
            }

            override fun onError(errorCode: String?, errorMessage: String?) {
                switchToBackupWifiResult.value = false
            }

        })
    }

    fun switchToNewWifi(ssid: String?, pwd: String?) {
        manager?.switchToNewWifi(ssid, pwd, object : IThingDataCallback<SwitchWifiResultBean?> {
            override fun onSuccess(result: SwitchWifiResultBean?) {
                switchToNewWifiResult.value = true
            }

            override fun onError(errorCode: String?, errorMessage: String?) {
                switchToNewWifiResult.value = false
            }

        })
    }

    fun isSupportBackupNetwork() {
        isSupportBackupNetwork.value = manager?.isSupportBackupNetwork()
    }
}