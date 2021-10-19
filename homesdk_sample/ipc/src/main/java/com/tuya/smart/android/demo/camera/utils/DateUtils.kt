package com.tuya.smart.android.demo.camera.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author houqing <a href="mailto:developer@tuya.com"/>
 * @since 2021/7/26 3:37 PM
 */
class DateUtils {
    companion object{
        /**
         * Get the start of the day（00:00:00）
         *
         * @param currentTime
         * @return
         */
        fun getTodayStart(currentTime: Long): Int {
            val calendar: Calendar = GregorianCalendar()
            calendar.timeInMillis = currentTime
            calendar[Calendar.HOUR_OF_DAY] = 0
            calendar[Calendar.MINUTE] = 0
            calendar[Calendar.SECOND] = 0
            val value = calendar.timeInMillis / 1000L
            return value.toInt()
        }

        /**
         * Get the end of the day（00:00:00）
         *
         * @param currentTime
         * @return
         */
        fun getTodayEnd(currentTime: Long): Int {
            val calendar = Calendar.getInstance()
            calendar.time = Date(currentTime)
            calendar[Calendar.HOUR_OF_DAY] = 0
            calendar[Calendar.MINUTE] = 0
            calendar[Calendar.SECOND] = 0
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val value = calendar.timeInMillis / 1000L
            return value.toInt()
        }

        /**
         *
         * @param year
         * @param month
         * @param day
         * @return
         */
        fun getCurrentTime(year: Int, month: Int, day: Int): Long {
            val monthStr = if (month < 10) "0$month" else "" + month
            val dayStr = if (day < 10) "0$day" else "" + day
            val currentDate = year.toString() + monthStr + dayStr
            val simpleDateFormat = SimpleDateFormat("yyyyMMdd")
            try {
                val date = simpleDateFormat.parse(currentDate)
                return date.time
            } catch (px: ParseException) {
                px.printStackTrace()
            }
            return 0
        }
    }

}