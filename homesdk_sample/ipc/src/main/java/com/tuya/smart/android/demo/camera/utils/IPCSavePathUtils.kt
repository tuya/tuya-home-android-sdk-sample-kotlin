package com.tuya.smart.android.demo.camera.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import com.tuya.smart.android.demo.camera.utils.IPCSavePathUtils
import java.io.File

class IPCSavePathUtils(context: Context) {
    init {
        //初始化外部存储根目录
        ROOT_PATH = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //android11及以上设备
            if (null == context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)) {
                context.filesDir.absolutePath
            } else {
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
                    .path
            }
        } else {
            //android11以下设备
            Environment.getExternalStorageDirectory().absolutePath
        }
        DOWNLOAD_PATH = "$ROOT_PATH/Camera/Thumbnail/"
        DOWNLOAD_PATH_Q = ROOT_PATH + "/Camera/" + Environment.DIRECTORY_DCIM + "/Thumbnail/"
    }

    fun recordPathSupportQ(devId: String): String {
        val videoPath: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //分区存储
            "$DOWNLOAD_PATH_Q$devId/"
        } else {
            //非分区存储
            "$DOWNLOAD_PATH$devId/"
        }
        val file = File(videoPath)
        if (!file.exists()) {
            if (!file.mkdirs()) {
                // L.e(TAG, "recordPathQ create the directory fail, videoPath is " + videoPath);
                return ""
            }
        }
        return videoPath
    }

    companion object {
        private var ROOT_PATH: String? = null
        lateinit var DOWNLOAD_PATH: String
        lateinit var DOWNLOAD_PATH_Q: String
    }
}