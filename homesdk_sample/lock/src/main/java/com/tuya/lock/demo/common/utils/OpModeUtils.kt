package com.tuya.lock.demo.common.utils

import android.content.Context
import com.thingclips.smart.optimus.lock.api.ThingUnlockType
import com.tuya.lock.demo.R

/**
 *
 * Created by HuiYao on 2024/2/29
 */
object OpModeUtils {

    @JvmStatic
    fun getTypeName(context: Context, dpCode: String): String {
        var name = dpCode
        when (dpCode) {
            ThingUnlockType.FINGERPRINT -> {
                name = context.getString(R.string.mode_fingerprint)
            }
            ThingUnlockType.CARD -> {
                name = context.getString(R.string.mode_card)
            }
            ThingUnlockType.PASSWORD -> {
                name = context.getString(R.string.mode_password)
            }
            ThingUnlockType.VOICE_REMOTE -> {
                name = context.getString(R.string.mode_voice_password)
            }
        }
        return name
    }
}