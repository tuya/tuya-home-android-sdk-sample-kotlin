package com.tuya.appbizsdk.scenebiz.extensions

import com.thingclips.smart.scene.lib.util.DeviceUtil
import com.thingclips.smart.scene.model.NormalScene
import com.thingclips.smart.scene.model.action.ActionItem
import com.thingclips.smart.scene.model.action.SceneAction
import com.thingclips.smart.scene.model.constant.*
import com.thingclips.smart.scene.model.device.StandardSceneInfo
import com.tuya.appbizsdk.scenebiz.R

/**
 * app 默认动作列表
 */
fun generateConstructActionList(isManual: Boolean, sceneDetail: NormalScene? = null) = listOf(
    ActionItem(
        R.drawable.scene_ic_action_device,
        R.string.scene_control_single_device,
        ACTION_TYPE_DEVICE,
        isManual
    ),

    ActionItem(
        R.drawable.scene_ic_action_automation,
        R.string.scene_execute_smart,
        ACTION_TYPE_TRIGGER,
        isManual
    ),
    ActionItem(
        R.drawable.scene_ic_push,
        R.string.scene_send_message,
        ACTION_TYPE_MESSAGE,
        isManual,
    ),
    ActionItem(
        R.drawable.scene_ic_delay,
        R.string.scene_delay,
        ACTION_TYPE_DELAY,
        isManual
    )
)

/**
 * 动作与推送类型映射
 */
val pushMap = mapOf(
    ACTION_TYPE_MESSAGE to PushType.PUSH_TYPE_MESSAGE,
    ACTION_TYPE_SMS to PushType.PUSH_TYPE_SMS,
    ACTION_TYPE_PHONE to PushType.PUSH_TYPE_MOBILE
)

/**
 * 动作与推送类型反映射
 */
val reversePushMap = mapOf(
    PushType.PUSH_TYPE_MESSAGE to ACTION_TYPE_MESSAGE,
    PushType.PUSH_TYPE_SMS to ACTION_TYPE_SMS,
    PushType.PUSH_TYPE_MOBILE to ACTION_TYPE_PHONE
)

fun createDpTask(devId: String?, tasks: MutableMap<String, Any?>?): SceneAction? {
    if (tasks.isNullOrEmpty() || devId.isNullOrEmpty()) return null
    val sceneAction = SceneAction()
    sceneAction.actionExecutor = ACTION_TYPE_DEVICE
    sceneAction.entityId = devId
    DeviceUtil.getDevice(devId)?.let {
        sceneAction.devIcon = it.iconUrl
        sceneAction.isDevOnline = it.isOnline
        sceneAction.entityName = it.name
    }
    sceneAction.executorProperty = tasks
    return sceneAction
}

/**
 * 判断是否Zigbee标准本地动作
 */
fun SceneAction.isLocalAction(): Boolean {
    val standardIds: StandardSceneInfo? = if (!extraProperty.isNullOrEmpty()) {
        extraProperty.let {
            StandardSceneInfo(
                it["sid"] as? String ?: "",
                it["gid"] as? String ?: "",
                it["gwId"] as? String ?: ""
            )
        }
    } else null

    return if (null != standardIds) {
        standardIds.gwId.isNotEmpty() && standardIds.gid.isNotEmpty() && standardIds.sid.isNotEmpty()
    } else false
}