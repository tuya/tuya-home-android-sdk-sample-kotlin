package com.tuya.appbizsdk.scenebiz.util

import kotlin.math.roundToInt

object PercentUtil {
    /**
     * 0-1000算法
     */
    fun percentToValueFromOne(percent: Int, min: Int, max: Int): Int {
        return min + (max - min) * (percent - 1) / 99
    }

    /**
     * 数值型dp点装换百分比 四舍五入
     */
    fun valueToPercent(value: Int, min: Int, max: Int): Int {
        val f = (max - min).toFloat()
        val v = (value - min) * 100f / f
        return v.roundToInt()
    }

    /**
     * 10-1000算法
     */
    fun valueToPercentFromOne(value: Int, min: Int, max: Int): Int {
        val f = (max - min).toFloat()
        val v = 1 + (value - min) * 99f / f
        return v.roundToInt()
    }

    fun getPercent(value: Int, min: Int, max: Int): String {
        return StringBuilder().append(valueToPercent(value, min, max)).append("%").toString()
    }

    fun getPercentFromOne(value: Int, min: Int, max: Int): String {
        return StringBuilder().append(valueToPercentFromOne(value, min, max)).append("%").toString()
    }

}