package com.tuya.smart.android.demo.camera

import android.app.Application
import android.content.Context
import android.content.Intent
import com.thingclips.sdk.core.PluginManager
import com.thingclips.smart.android.camera.sdk.ThingIPCSdk
import com.thingclips.smart.android.config.api.IBaseConfig
import com.thingclips.smart.android.user.api.IBaseUser
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.interior.api.IThingUserListenerPlugin
import com.thingclips.smart.ipc.yuv.monitor.utils.log.L
import com.tuya.smart.android.demo.camera.utils.CameraDoorbellManager
import com.tuya.smart.android.demo.camera.utils.CameraVideoCallManager
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
            //监听登录/登出事件，注册视频通话模块
            val userListenerPlugin: IThingUserListenerPlugin? =
                PluginManager.service(IThingUserListenerPlugin::class.java)
            userListenerPlugin?.registerLoginSuccessListener {
                L.i("UserListener", "LoginSuccess")
                CameraVideoCallManager.registerCallModule()
            }
            userListenerPlugin?.registerLogoutListener {
                L.i("UserListener", "LoginOut")
                CameraVideoCallManager.unRegisterCallModule()
            }
            // 已登录但应用重新启动，需要注册下
            if (PluginManager.service(IBaseUser::class.java)?.isLogin == true) {
                L.i("UserListener", "Already Login")
                CameraVideoCallManager.registerCallModule()
            }
        }

        fun ipcProcess(context: Context, devId: String?): Boolean {
            val cameraInstance = ThingIPCSdk.getCameraInstance()
            if (cameraInstance?.isIPCDevice(devId) == true) {
                //视频分割
                if (cameraInstance.getCameraConfig(devId)?.isSupportVideoSegmentation == true) {
                    val intent = Intent(context, CameraMultiCamActivity::class.java)
                    intent.putExtra(Constants.INTENT_DEV_ID, devId)
                    context.startActivity(intent)
                    return true
                }
                //视频通话
                devId?.let {
                    CameraVideoCallManager.fetchSupportVideoCall(
                        it,
                        object : IThingResultCallback<Boolean> {
                            override fun onSuccess(result: Boolean?) {
                                if (result == true) {
                                    CameraVideoCallManager.callOut(it)
                                } else {
                                    startIPCPanel(context, devId)
                                }
                            }

                            override fun onError(errorCode: String?, errorMessage: String?) {
                                startIPCPanel(context, devId)
                            }
                        })
                }
                return true
            }
            return false
        }

        fun startIPCPanel(context: Context, devId: String?) {
            val intent = Intent(context, CameraPanelActivity::class.java)
            intent.putExtra(Constants.INTENT_DEV_ID, devId)
            context.startActivity(intent)
        }
    }
}