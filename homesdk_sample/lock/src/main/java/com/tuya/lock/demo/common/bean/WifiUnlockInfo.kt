package com.tuya.lock.demo.common.bean

import com.thingclips.smart.optimus.lock.api.zigbee.response.OpModeBean


/**
 *
 * Created by HuiYao on 2024/2/29
 */
data class WifiUnlockInfo(
    var type: Int = 0,
    var name: String? = null,
    var dpCode: String? = null,
    var count: Int = 0,
    var infoBean: OpModeBean? = null
)
