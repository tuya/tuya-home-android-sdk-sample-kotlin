package com.tuya.smart.android.demo.camera.utils

import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

object BitmapUtils {
    private const val TAG = "BitmapUtils"
    @JvmOverloads
    fun savePhotoToSDCard(
        photoBitmap: Bitmap?,
        path: String,
        name: String? = System.currentTimeMillis().toString() + ".png"
    ): Boolean {
        var isSave = false
        val dir = File(path)
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e(TAG, "savePhotoToSDCard create file fail, path: $path")
            }
        }
        val photoFile = File(path, name)
        var fileOutputStream: FileOutputStream? = null
        try {
            fileOutputStream = FileOutputStream(photoFile)
            if (photoBitmap != null) {
                if (photoBitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)) {
                    fileOutputStream.flush()
                }
                isSave = true
            }
        } catch (e: FileNotFoundException) {
            if (!photoFile.delete()) {
                Log.e(TAG, "savePhotoToSDCard delete photoFile fail, path: $path")
            }
            e.printStackTrace()
            isSave = false
        } catch (e: IOException) {
            if (!photoFile.delete()) {
                Log.e(TAG, "savePhotoToSDCard try catch delete file fail, path: $path")
            }
            e.printStackTrace()
            isSave = false
        } finally {
            try {
                fileOutputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return isSave
    }
}