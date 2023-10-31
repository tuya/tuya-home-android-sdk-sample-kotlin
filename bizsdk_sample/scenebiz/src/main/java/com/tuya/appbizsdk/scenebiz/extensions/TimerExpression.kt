package com.tuya.appbizsdk.scenebiz.extensions

import java.util.*

data class TimerExpression(
    var date: String = "",
    val timeZoneId: String = TimeZone.getDefault().id,
    var loops: String = "",
    var time: String = ""
)
