package com.tuya.lock.demo

import android.content.Context
import com.thingclips.sdk.os.ThingOSDevice
import com.thingclips.smart.android.common.utils.L
import com.tuya.lock.demo.ble.activity.BleLockDetailActivity
import com.tuya.lock.demo.video.activity.VideoDeviceDetail
import com.tuya.lock.demo.wifi.activity.WifiDeviceDetail
import com.tuya.lock.demo.zigbee.activity.DeviceDetail

/**
 *
 * Created by HuiYao on 2024/2/28
 */
object LockDeviceUtils {
    fun check(context: Context?, deviceId: String): Boolean {
        val mDeviceBean = ThingOSDevice.getDeviceBean(deviceId)
        val categoryCode = mDeviceBean.categoryCode
        L.i(
            "LockDeviceUtils",
            "device:$deviceId, categoryCode:$categoryCode"
        )
        if (categoryCode.contains("jtmspro_2b_2") || categoryCode.contains("ble_ms")) {
            BleLockDetailActivity.startActivity(context, deviceId)
            return true
        } else if (categoryCode.contains("jtmspro_4z_1") || categoryCode.contains("zig_ms")) {
            DeviceDetail.startActivity(context, deviceId)
            return true
        } else if (categoryCode.contains("wf_jtms") || categoryCode.contains("wf_ms")) {
            WifiDeviceDetail.startActivity(context, deviceId)
            return true
        } else if (categoryCode.contains("videolock_1w_1")) {
            VideoDeviceDetail.startActivity(context, deviceId)
            return true
        }
        return false
    }
}