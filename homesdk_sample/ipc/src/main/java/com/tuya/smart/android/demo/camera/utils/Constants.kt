package com.tuya.smart.android.demo.camera.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.lang.Exception

/**
 * @author houqing <a href="mailto:developer@tuya.com"/>
 * @since 2021/7/26 3:36 PM
 */
class Constants {
    companion object {
        const val INTENT_MSGID = "msgid"
        const val INTENT_DEV_ID = "intent_devId"
        const val INTENT_P2P_TYPE = "intent_p2p_type"
        const val EXTERNAL_STORAGE_REQ_CODE = 10
        const val EXTERNAL_AUDIO_REQ_CODE = 11

        const val ARG1_OPERATE_SUCCESS = 0
        const val ARG1_OPERATE_FAIL = 1

        const val MSG_CONNECT = 2033
        const val MSG_CREATE_DEVICE = 2099
        const val MSG_SET_CLARITY = 2054

        const val MSG_TALK_BACK_FAIL = 2021
        const val MSG_TALK_BACK_BEGIN = 2022
        const val MSG_TALK_BACK_OVER = 2023
        const val MSG_DATA_DATE = 2035

        const val MSG_MUTE = 2024
        const val MSG_SCREENSHOT = 2017

        const val MSG_VIDEO_RECORD_FAIL = 2018
        const val MSG_VIDEO_RECORD_BEGIN = 2019
        const val MSG_VIDEO_RECORD_OVER = 2020


        const val MSG_DATA_DATE_BY_DAY_SUCC = 2045
        const val MSG_DATA_DATE_BY_DAY_FAIL = 2046

        const val ALARM_DETECTION_DATE_MONTH_FAILED = 2047
        const val ALARM_DETECTION_DATE_MONTH_SUCCESS = 2048
        const val MSG_GET_ALARM_DETECTION = 2049
        const val MOTION_CLASSIFY_FAILED = 2050
        const val MOTION_CLASSIFY_SUCCESS = 2051
        const val MSG_DELETE_ALARM_DETECTION = 2052
        const val MSG_GET_VIDEO_CLARITY = 2053

        fun requestPermission(
            context: Context,
            permission: String,
            requestCode: Int,
            tip: String
        ): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                return true
            }
            return if (ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        (context as Activity),
                        permission
                    )
                ) {
                    ToastUtil.shortToast(context, tip)
                } else {
                    ActivityCompat.requestPermissions(
                        context,
                        arrayOf(permission),
                        requestCode
                    )
                }
                false
            } else {
                true
            }
        }

        @SuppressLint("all")
        fun hasRecordPermission(): Boolean {
            val minBufferSize = AudioRecord.getMinBufferSize(
                8000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSizeInBytes = 640
            val audioData = ByteArray(bufferSizeInBytes)
            var readSize = 0
            var audioRecord: AudioRecord? = null
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.DEFAULT, 8000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, minBufferSize
                )
                // start recording
                audioRecord.startRecording()
            } catch (e: Exception) {
                audioRecord?.release()
                return false
            }
            return if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
                audioRecord?.release()
                false
            } else {
                readSize = audioRecord.read(audioData, 0, bufferSizeInBytes)
                // Check whether the recording result can be obtained
                audioRecord?.stop()
                audioRecord?.release()
               return readSize>0
            }
        }
    }

}