package com.tuya.lock.demo.common.utils

import android.graphics.BitmapFactory
import android.text.TextUtils
import android.widget.ImageView
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 *
 * Created by HuiYao on 2024/2/29
 */
object Utils {

    @JvmStatic
    fun getStampTime(time: String?, pattern: String?): Long {
        if (TextUtils.isEmpty(time)) {
            return 0
        }
        val simpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())
        var date: Date? = null
        try {
            date = simpleDateFormat.parse(time)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return date?.time ?: 0
    }

    @JvmStatic
    fun getDateOneDay(stamp: Long): String? {
        return getDateDay(stamp, "yyyy-MM-dd HH:mm")
    }

    @JvmStatic
    fun getStampTime(time: String?): Long {
        return getStampTime(time, "yyyy-MM-dd HH:mm:ss")
    }

    @JvmStatic
    fun getDateDay(stamp: Long): String? {
        return getDateDay(stamp, "yyyy-MM-dd HH:mm:ss")
    }

    @JvmStatic
    fun getDateDay(stamp: Long, pattern: String?): String? {
        val simpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())
        return simpleDateFormat.format(Date(stamp))
    }

    @JvmStatic
    fun showImageUrl(imageUrl: String?, imageView: ImageView) {
        try {
            val url = URL(imageUrl)
            Thread {
                try {
                    val bitmap =
                        BitmapFactory.decodeStream(url.openStream())
                    imageView.post { imageView.setImageBitmap(bitmap) }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}