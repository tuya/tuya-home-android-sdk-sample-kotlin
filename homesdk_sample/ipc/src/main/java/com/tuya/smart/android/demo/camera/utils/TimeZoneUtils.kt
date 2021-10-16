package com.tuya.smart.android.demo.camera.utils

import android.icu.util.TimeZone
import android.os.Build
import java.util.*

/**

 * TODO feature

 *

 * @author houqing <a href="mailto:developer@tuya.com"/>

 * @since 2021/7/26 3:38 PM

 */
class TimeZoneUtils {
    companion object{
        fun getTimezoneGCMById(timezoneId: String?): String? {
            val timeZoneByRawOffset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val timeZone = TimeZone.getTimeZone(timezoneId)
                timeZone.rawOffset + timeZone.dstSavings
            } else {
                val timeZone = SimpleTimeZone.getTimeZone(timezoneId)
                timeZone.rawOffset + timeZone.dstSavings
            }
            return getTimeZoneByRawOffset(timeZoneByRawOffset)
        }


        private fun getTimeZoneByRawOffset(rawOffset: Int): String {
            var timeDisplay = if (rawOffset >= 0) "+" else ""
            val hour = rawOffset / 1000 / 3600
            val minute = (rawOffset - hour * 1000 * 3600) / 1000 / 60
            timeDisplay += String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            return timeDisplay
        }
    }
}