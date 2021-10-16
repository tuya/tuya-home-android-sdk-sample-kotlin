package com.tuya.smart.android.demo.camera.bean

/**
 * @author houqing <a href="mailto:developer@tuya.com"/>
 * @since 2021/7/26 4:53 PM
 */
data class TimePieceBean(val startTime:Int,val endTime:Int,val playTime:Int,val prefix:Int) :Comparable<TimePieceBean> {
    override fun compareTo(other: TimePieceBean): Int {
        return if (endTime >= other.endTime) 1 else -1
    }

    override fun toString(): String {
        return "TimePieceBean(startTime=$startTime, endTime=$endTime, playTime=$playTime, prefix=$prefix)"
    }

}