package com.tuya.appbizsdk.scenebiz.util

import android.content.Context
import com.tuya.appbizsdk.scenebiz.R


object TimeUtil {
    fun secondToShowText(context: Context, second: Int): String {
        return hmsToS(context, sToHms(second))
    }

    private fun hmsToS(context: Context, hms: Array<Int>): String {
        return if (hms[0] == 0 && hms[1] == 0 && hms[2] == 0) {
            StringBuilder().append(0)
                .append(context.getString(R.string.thing_countdown_second)).toString()
        } else {
            StringBuilder().append(
                if (hms[0] != 0) StringBuilder().append(hms[0])
                    .append(context.getString(R.string.thing_countdown_hour)) else ""
            )
                .append(
                    if (hms[1] != 0) StringBuilder().append(hms[1])
                        .append(context.getString(R.string.thing_countdown_minute)) else ""
                )
                .append(
                    if (hms[2] != 0) StringBuilder().append(hms[2])
                        .append(context.getString(R.string.thing_countdown_second)) else ""
                )
                .toString()
        }
    }

    private fun sToHms(second: Int): Array<Int> {
        return arrayOf(second / 3600, second / 60 % 60, second % 60)
    }
}
