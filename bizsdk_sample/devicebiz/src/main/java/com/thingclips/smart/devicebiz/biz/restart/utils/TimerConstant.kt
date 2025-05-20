package com.thingclips.smart.devicebiz.biz.restart.utils

object TimerConstant {

    const val MODE_REPEAT_WEEKEND = "1000001"
    const val MODE_REPEAT_WEEKDAY = "0111110"
    const val MODE_REPEAT_ONCE = "0000000"
    const val MODE_REPEAT_EVERYDAY = "1111111"


    const val DEVICE_TIME_DATA = "device_timer_data"
    const val DEVICE_TIME_LOOP = "device_timer_loop"
    const val DEVICE_RESTART_DEV_ID = "device_restart_devId"


    // 编辑
    const val RC_TIMER_EDIT = 0x0001
    // 新增
    const val RC_TIMER_ADD = 0x0002
    // 选择重复日期
    const val RC_TIMER_LOOPER = 0x0003
    // 数据
    const val RC_TIMER_DATA = 0x0004


    const val DEVICE_IS_OFFLINE = "1001"
    const val QUERY_TIME_OUT = "1002"



    const val TAG = "DeviceRestart"


    const val REQ_TYPE = "dev_reboot"
    const val REBOOT_TYPE_IMMEDIATE = "immediate"
    const val REBOOT_TYPE_QUERY = "query"
    const val REBOOT_TYPE_ADD = "add"
    const val REBOOT_TYPE_DELETE = "delete"
    const val REBOOT_TYPE_UPDATE = "update"


    const val MQTT_22 = 22
    const val MQTT_23 = 23
    const val LAN_40 = 0x40

}