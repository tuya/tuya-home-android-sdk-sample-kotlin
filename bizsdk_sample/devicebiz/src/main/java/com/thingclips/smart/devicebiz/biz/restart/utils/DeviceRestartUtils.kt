package com.thingclips.smart.devicebiz.biz.restart.utils

import android.content.Context


object DeviceRestartUtils {

     fun setRepeatShowTime(context: Context, mode: String):String {
       return when (mode) {
            TimerConstant.MODE_REPEAT_WEEKDAY -> {
                "Working Days"
            }

            TimerConstant.MODE_REPEAT_WEEKEND -> {
                "Weekend"
            }

            TimerConstant.MODE_REPEAT_EVERYDAY -> {
                "Every Day"
            }

            TimerConstant.MODE_REPEAT_ONCE -> {
                "Once"
            }

            else -> {
               getRepeatString(context, mode)
            }
        }
    }

    fun getRepeatString(context: Context, mode: String): String {
        val suffix = ", "
        val stringBuilder = StringBuilder()
        val res = arrayOf("sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday")

        for (i in mode.indices) {
            if (mode[i] == '1') {
                stringBuilder.append(res[i])
                stringBuilder.append(suffix)
            }
        }

        if (stringBuilder.isNotEmpty()) {
            stringBuilder.delete(stringBuilder.length - suffix.length, stringBuilder.length)
        }

        return stringBuilder.toString()
    }

}