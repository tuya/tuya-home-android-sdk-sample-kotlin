package com.thingclips.smart.devicebiz.biz.migrate.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.thingclips.smart.android.network.Business
import com.thingclips.smart.android.network.http.BusinessResponse
import com.thingclips.smart.device.DeviceBusinessDataManager
import com.thingclips.smart.device.migration.IDeviceMigrationManager
import com.thingclips.smart.device.migration.IMigratedStateListener
import com.thingclips.smart.device.migration.bean.MigrationInfo
import com.thingclips.smart.sdk.api.wifibackup.api.bean.BackupWifiBean
import com.thingclips.smart.sdk.api.wifibackup.api.bean.CurrentWifiInfoBean

class DeviceMigrateModel(application: Application) : AndroidViewModel(application) {
    private var manager: IDeviceMigrationManager? = null

    var mMigrationDeviceList: MutableLiveData<List<String>> =
        MutableLiveData<List<String>>()

    var migrationState: MutableLiveData<MigrationInfo?> =
        MutableLiveData<MigrationInfo?>()

    var isStartMigrateSuccess: MutableLiveData<Boolean> =
        MutableLiveData<Boolean>()

    var isSupportMigrate: MutableLiveData<Boolean> =
        MutableLiveData<Boolean>()


    init {
        manager = DeviceBusinessDataManager.getInstance().getDeviceMigrationManager()
    }

    /**
     * 开始替换故障网关
     *
     * @param sourcesGwId 新网关的id
     * @param targetGwId 被替换的故障网关的id或sn码
     * @param gid 当前空间id
     */
    fun startMigrateGateway(
        sourcesGwId: String,
        targetGwId: String,
        gid: Long,
    ) {
        manager?.startMigrateGateway(
            sourcesGwId,
            targetGwId,
            gid,
            object : Business.ResultListener<MigrationInfo> {

                override fun onSuccess(
                    response: BusinessResponse?,
                    data: MigrationInfo?,
                    apiName: String?
                ) {
                    isStartMigrateSuccess.value = true
                }

                override fun onFailure(
                    response: BusinessResponse?,
                    data: MigrationInfo?,
                    apiName: String?
                ) {
                    isStartMigrateSuccess.value = false
                }
            })
    }

    /**
     * 查询可迁移的故障网关列表
     *
     * @param gwId 新网关的id
     * @param gid 当前空间id
     */
    fun getEnableMigratedGatewayList(
        gwId: String,
        gid: Long,
    ) {
        manager?.getEnableMigratedGatewayList(
            gwId,
            gid,
            object : Business.ResultListener<ArrayList<String>> {

                override fun onSuccess(
                    response: BusinessResponse?,
                    data: ArrayList<String>?,
                    apiName: String?
                ) {
                    mMigrationDeviceList.value = data
                }

                override fun onFailure(
                    response: BusinessResponse?,
                    data: ArrayList<String>?,
                    apiName: String?
                ) {
                    mMigrationDeviceList.value = data
                }
            })
    }

    /**
     * 查询当前网关是否具备替换故障网关的能力
     *
     * @param gwId 新网关的id
     */
    fun isSupportedMigrationWithGwId(
        gwId: String,
    ) {
        manager?.isSupportedMigrationWithGwId(gwId, object : Business.ResultListener<Boolean> {

            override fun onSuccess(response: BusinessResponse?, data: Boolean?, apiName: String?) {
                isSupportMigrate.value = data
            }

            override fun onFailure(response: BusinessResponse?, data: Boolean?, apiName: String?) {
                isSupportMigrate.value = false
            }
        })
    }

    /**
     * 查询故障网关迁移状态
     *
     * @param gwId 新网关的id
     */
    fun getMigratedGwState(
        gwId: String,
    ) {
        manager?.getMigratedGwState(gwId, object : Business.ResultListener<MigrationInfo> {

            override fun onSuccess(
                response: BusinessResponse?,
                data: MigrationInfo?,
                apiName: String?
            ) {
                migrationState.value = data
            }

            override fun onFailure(
                response: BusinessResponse?,
                data: MigrationInfo?,
                apiName: String?
            ) {
                migrationState.value = data
            }
        })
    }

    /**
     * 注册故障网关替换状态监听器
     *
     * @param listener IMigratedStateListener
     */
    fun addMigratedStateListener(listener: IMigratedStateListener) {
        manager?.addMigratedStateListener(listener)
    }

    /**
     * 注销故障网关替换状态监听器
     *
     * @param listener IMigratedStateListener
     */
    fun removeMigratedStateListener(listener: IMigratedStateListener) {
        manager?.removeMigratedStateListener(listener)
    }

    fun onDestroy() {
        manager?.onDestroy()
    }


}