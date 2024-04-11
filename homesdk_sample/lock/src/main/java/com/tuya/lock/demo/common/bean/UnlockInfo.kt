package com.tuya.lock.demo.common.bean

import com.thingclips.smart.sdk.optimus.lock.bean.ble.UnlockInfoBean

/**
 *
 * Created by HuiYao on 2024/2/29
 */
data class UnlockInfo(
    var type: Int = 0,

    var name: String? = null,

    var dpCode: String? = null,

    var count: Int = 0,

    var infoBean: UnlockInfoBean? = null
)
