package com.tuya.smart.android.demo.camera

import android.app.Application
import android.content.Context
import android.content.Intent
import com.thingclips.smart.android.camera.sdk.ThingIPCSdk
import com.tuya.smart.android.demo.camera.utils.CameraDoorbellManager
import com.tuya.smart.android.demo.camera.utils.Constants
import com.tuya.smart.android.demo.camera.utils.FrescoManager

/**
 * @author houqing <a href="mailto:developer@tuya.com"/>
 * @since 2021/7/26 3:33 PM
 */
class CameraUtils {
    companion object {
        fun init(application: Application) {
            FrescoManager.initFresco(application)
            CameraDoorbellManager.getInstance().init(application)
        }

        fun ipcProcess(context: Context, devId: String?): Boolean {
            val cameraInstance = ThingIPCSdk.getCameraInstance()
            if (cameraInstance?.isIPCDevice(devId) == true) {
                val intent = Intent(context, CameraPanelActivity::class.java)
                intent.putExtra(Constants.INTENT_DEV_ID, devId)
                context.startActivity(intent)
                return true
            }
            return false
        }
    }
}