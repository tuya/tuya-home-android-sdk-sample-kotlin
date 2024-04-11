package com.thingclips.smart.devicebiz.utils

import com.thingclips.group_usecase_api.bean.GroupDeviceDetailBean
import com.thingclips.group_usecase_api.relation.ThingGroupCoreKit
import com.thingclips.smart.group.manager.bean.GroupInfo

fun List<GroupInfo>?.toGroupDeviceDetail(): List<GroupDeviceDetailBean> {
    return this?.map { bean ->
        val mBean = GroupDeviceDetailBean()
        mBean.isChecked = bean.isChecked
        mBean.productId = bean.productId
        mBean.isOnline = bean.isOnline
        mBean.deviceBean = ThingGroupCoreKit.getDeviceBean(bean.devId)
        mBean.belongHomeName = ThingGroupCoreKit.getHomeName()
        mBean.belongRoomName = ThingGroupCoreKit.getRoomName(bean.devId)
        mBean
    }?.toList() ?: arrayListOf()
}