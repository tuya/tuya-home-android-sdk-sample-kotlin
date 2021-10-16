package com.tuya.smart.android.demo.camera.utils

/**
 * @author houqing <a href="mailto:developer@tuya.com"/>
 * @since 2021/7/26 3:37 PM
 */
class DPConstants {
    companion object{
        //Data type: enum
        const val PTZ_CONTROL = "119"

        //Data type: boolean
        const val PTZ_STOP = "116"

        //Data type: boolean
        const val WATERMARK = "104"

        //Data type: boolean
        const val SD_CARD_RECORD_SWITCH = "150"

        const val SD_STATUS = "110"
        const val SD_STORAGE = "109"
        const val SD_FORMAT = "111"
        const val SD_FORMAT_STATUS = "117"

        //DP Data type
        const val SCHEMA_TYPE_RAW = "raw"
        const val SCHEMA_TYPE_BOOL = "bool"
        const val SCHEMA_TYPE_ENUM = "enum"
        const val SCHEMA_TYPE_VALUE = "value"
        const val SCHEMA_TYPE_STRING = "string"

        const val PTZ_UP = "0"
        const val PTZ_LEFT = "2"
        const val PTZ_DOWN = "4"
        const val PTZ_RIGHT = "6"
    }
}