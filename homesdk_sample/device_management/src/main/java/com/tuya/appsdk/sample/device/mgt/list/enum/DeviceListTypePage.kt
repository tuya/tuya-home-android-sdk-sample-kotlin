package com.tuya.appsdk.sample.device.mgt.list.enum

/**
 * Device List Type Page
 *
 * @author aiwen <a href="mailto:developer@tuya.com"/>
 * @since 2/25/21 2:16 PM
 */
interface DeviceListTypePage {
    companion object {
        const val NORMAL_DEVICE_LIST = 1
        const val ZIGBEE_GATEWAY_LIST = 2
        const val ZIGBEE_SUB_DEVICE_LIST = 3
    }
}