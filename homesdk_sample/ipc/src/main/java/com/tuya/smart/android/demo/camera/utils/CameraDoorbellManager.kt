package com.tuya.smart.android.demo.camera.utils

import android.app.Application
import android.content.Intent
import com.thingclips.smart.android.camera.sdk.ThingIPCSdk
import com.thingclips.smart.android.camera.sdk.api.IThingIPCDoorBellManager
import com.thingclips.smart.android.camera.sdk.bean.ThingDoorBellCallModel
import com.thingclips.smart.android.camera.sdk.callback.ThingSmartDoorBellObserver
import com.thingclips.smart.android.common.utils.L
import com.tuya.smart.android.demo.camera.CameraDoorBellActivity
import com.thingclips.smart.sdk.bean.DeviceBean

/**
 * @author houqing <a href="mailto:developer@tuya.com"/>
 * @since 2021/7/26 3:36 PM
 */
class CameraDoorbellManager {
    companion object {
        private const val TAG = "CameraDoorbellManager"
        const val EXTRA_AC_DOORBELL = "ac_doorbell"
        private val INSTANCE: CameraDoorbellManager by lazy { CameraDoorbellManager() }
        fun getInstance(): CameraDoorbellManager {
            return INSTANCE
        }
    }

    private val doorBellInstance: IThingIPCDoorBellManager by lazy { ThingIPCSdk.getDoorbell().ipcDoorBellManagerInstance }

    fun init(application: Application) {
        doorBellInstance.addObserver(object : ThingSmartDoorBellObserver() {
            override fun doorBellCallDidReceivedFromDevice(
                callModel: ThingDoorBellCallModel?,
                deviceModel: DeviceBean?
            ) {
                L.d(TAG, "Receiving a doorbell call")
                callModel?.let {
                    val type = it.type
                    val messageId = it.messageId
                    if (EXTRA_AC_DOORBELL == type) {
                        val intent = Intent(
                            application.applicationContext,
                            CameraDoorBellActivity::class.java
                        )
                        intent.putExtra(Constants.INTENT_MSGID, messageId)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        application.applicationContext.startActivity(intent)
                    }
                }
            }
        })
    }

    fun deInit() {
        doorBellInstance.removeAllObservers()
    }

}