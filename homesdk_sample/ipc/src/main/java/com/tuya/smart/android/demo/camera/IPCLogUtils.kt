package com.tuya.smart.android.demo.camera

import android.util.Log
import com.thingclips.smart.android.common.utils.L
import com.thingclips.smart.android.common.utils.log.ILogInterception
import com.tuya.smart.android.demo.camera.IPCLogUtils

object IPCLogUtils {
    fun init() {
        L.setLogInterception(2) { _, tag, msg -> customLog(tag, msg) }
    }

    private fun customLog(var1: String, var2: String) {
        //use your log system to print/save log
        Log.i(var1, var2)
    }
}