package com.thingclips.smart.devicebiz.utils

import com.thingclips.smart.android.common.utils.Base64
import com.thingclips.smart.android.common.utils.L
import java.security.MessageDigest

object SHA256Utils {

    fun getBase64Hash(str: String): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.reset()
            String(Base64.encodeBase64(digest.digest(str.toByteArray())))
        } catch (var2: Throwable) {
            L.w("", var2.toString(), var2)
            null
        }
    }
}