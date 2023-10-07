package com.tuya.appbizsdk.activator.util.qrcode

import android.content.Context
import android.view.WindowManager

/**
 * TODO feature
 *
 * @author hou qing <a href="mailto:developer@tuya.com"/>
 * @since 2021/7/28 6:19 下午
 */
class DensityUtil {
    companion object{
        fun dip2px(context: Context, dpValue: Float): Int {
            val scale = context.resources.displayMetrics.density
            return (dpValue * scale + 0.5f).toInt()
        }

        fun dip2pxF(context: Context, dpValue: Float): Float {
            val scale = context.resources.displayMetrics.density
            return dpValue * scale + 0.5f
        }

        /**
         * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
         */
        fun px2dip(context: Context, pxValue: Float): Int {
            val scale = context.resources.displayMetrics.density
            return (pxValue / scale + 0.5f).toInt()
        }

        /**
         * 将px值转换为sp值，保证文字大小不变
         *
         * @param pxValue
         * @param context
         * （DisplayMetrics类中属性scaledDensity）
         * @return
         */
        fun px2sp(context: Context, pxValue: Float): Int {
            val fontScale = context.resources.displayMetrics.scaledDensity
            return (pxValue / fontScale + 0.5f).toInt()
        }

        /**
         * 将sp值转换为px值，保证文字大小不变
         *
         * @param spValue
         * @param context
         * （DisplayMetrics类中属性scaledDensity）
         * @return
         */
        fun sp2px(context: Context, spValue: Float): Int {
            val fontScale = context.resources.displayMetrics.scaledDensity
            return (spValue * fontScale + 0.5f).toInt()
        }

        fun getScreenDispalyWidth(context: Context): Int {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val width = wm.defaultDisplay.width //手机屏幕的宽度
            val height = wm.defaultDisplay.height //手机屏幕的高度
            return if (width > height) height else width
        }
    }
}