package com.thingclips.smart.devicebiz.bean

import com.thingclips.sdk.device.enums.DevUpgradeStatusEnum

data class OtaInfoItemBean(val icon:String, val name:String, val devId:String, var status: DevUpgradeStatusEnum
                           , var isEnable:Boolean)