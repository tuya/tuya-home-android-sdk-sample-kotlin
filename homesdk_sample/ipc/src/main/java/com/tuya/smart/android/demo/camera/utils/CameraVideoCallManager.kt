package com.tuya.smart.android.demo.camera.utils

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.thingclips.smart.android.camera.sdk.ThingIPCSdk
import com.thingclips.smart.call.module.api.FailureCallback
import com.thingclips.smart.call.module.api.IThingCallModule
import com.thingclips.smart.call.module.api.SuccessCallback
import com.thingclips.smart.call.module.api.ThingCallModuleConstants
import com.thingclips.smart.call.module.api.bean.ThingCall
import com.thingclips.smart.call.module.api.bean.ThingCallChannel
import com.thingclips.smart.call.module.api.bean.ThingCallError
import com.thingclips.smart.call.module.api.ui.ICallInterfaceProvider
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.sdk.ThingSdk
import com.tuya.smart.android.demo.camera.CameraVideoCallActivity

object CameraVideoCallManager {

    private const val TAG = "CameraVideoCallManager"
    private const val IPC_CATEGORY = "sp_dpsxj"

    private val callModuleService: IThingCallModule? by lazy {
        ThingIPCSdk.getVideoCall()
    }

    private var isRegister = false;

    /**
     * 一般在登入后注册
     */
    fun registerCallModule() {
        if (!isRegister) {
            // 向通话中心注册 Provider
            callModuleService?.registerCallModuleProvider(
                IPC_CATEGORY,
                VideoCallProvider()
            )
            callModuleService?.registerMessageHandler()
            isRegister = true
        }
    }

    /**
     * 退出登录注销
     */
    fun unRegisterCallModule() {
        callModuleService?.unRegisterMessageHandler()
    }

    fun callIn(call: ThingCall, success: SuccessCallback, failure: FailureCallback) {
        callModuleService?.receiveCallMessage(call, success, failure)
    }

    fun callOut(devId: String) {
        Handler(Looper.getMainLooper()).run {
            registerCallModule()
        }
        val extra = mutableMapOf<String, Any>()
        extra[ThingCallModuleConstants.EXTRA_KEY_CATEGORY] = IPC_CATEGORY
        extra[ThingCallModuleConstants.EXTRA_KEY_BIZ_TYPE] = "screen_ipc"
        extra[ThingCallModuleConstants.EXTRA_KEY_CHANNEL_TYPE] = ThingCallChannel.ALL.value

        callModuleService?.launchCall(
            devId,
            30L,
            extra,
            object : SuccessCallback {
                override fun invoke() {
                }
            }, object : FailureCallback {
                override fun invoke(error: ThingCallError) {
                }
            }
        )
    }

    /**
     * 是否支持双向视频对讲
     */
    fun fetchSupportVideoCall(devId: String, callback: IThingResultCallback<Boolean>?) {
        ThingIPCSdk.getVideoCall()?.fetchSupportVideoCall(devId, callback)
    }
}

class VideoCallProvider : ICallInterfaceProvider {
    override fun launchUI(call: ThingCall) {
        val bundle = Bundle()
        bundle.putParcelable("thing_call", call)
        bundle.putString(Constants.INTENT_DEV_ID, call.targetId)
        val intent = Intent(
            ThingSdk.getApplication().applicationContext,
            CameraVideoCallActivity::class.java
        )
        intent.putExtras(bundle)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        ThingSdk.getApplication().applicationContext.startActivity(intent)
    }
}