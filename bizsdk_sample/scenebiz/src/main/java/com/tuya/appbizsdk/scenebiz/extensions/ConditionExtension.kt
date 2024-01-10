package com.tuya.appbizsdk.scenebiz.extensions

import android.content.Context
import com.thingclips.scene.core.protocol.expr.usualimpl.SunSetRiseRule
import com.thingclips.smart.scene.model.condition.ConditionItem
import com.thingclips.smart.scene.model.condition.SceneCondition
import com.thingclips.smart.scene.model.constant.*
import com.tuya.appbizsdk.scenebiz.R
import java.text.SimpleDateFormat
import java.util.*

val defaultConditionMap = mapOf(
    Pair(
        CONDITION_TYPE_MANUAL,
        ConditionItem(
            conditionIcon = R.drawable.scene_ic_manual,
            conditionName = R.string.scene_ui_one_click_excute,
            conditionHint = R.string.thing_scene_create_tip_click_excute,
            type = CONDITION_TYPE_MANUAL
        )
    ),
    Pair(
        CONDITION_TYPE_WEATHER,
        ConditionItem(
            conditionIcon = R.drawable.scene_ic_weather_change,
            conditionName = R.string.scene_weather_change,
            conditionHint = R.string.thing_scene_create_tip_weather_change,
            type = CONDITION_TYPE_WEATHER
        )
    ),
    Pair(
        CONDITION_TYPE_GEO_FENCING,
        ConditionItem(
            conditionIcon = R.drawable.scene_ic_geofence,
            conditionName = R.string.scene_position_change,
            conditionHint = R.string.thing_scene_create_tip_position_change,
            type = CONDITION_TYPE_GEO_FENCING
        )
    ),

    Pair(
        CONDITION_TYPE_TIMER,
        ConditionItem(
            conditionIcon = R.drawable.scene_ic_timer,
            conditionName = R.string.timer,
            conditionHint = R.string.thing_scene_create_tip_timer,
            type = CONDITION_TYPE_TIMER
        )
    ),
    Pair(
        CONDITION_TYPE_DEVICE,
        ConditionItem(
            conditionIcon = R.drawable.scene_ic_device,
            conditionName = R.string.scene_device_status_change,
            conditionHint = R.string.thing_scene_create_tip_device_status_change,
            type = CONDITION_TYPE_DEVICE
        )
    ),
    Pair(
        CONDITION_TYPE_LOCK,
        ConditionItem(
            conditionIcon = R.drawable.scene_ic_go_home,
            conditionName = R.string.thing_scene_member_back_home,
            conditionHint = R.string.thing_scene_create_tip_member_go_home,
            type = CONDITION_TYPE_LOCK
        )
    )
)

val valueStatusMap = mapOf(
    Pair("==", R.string.thing_smart_scene_edit_equal),
    Pair("<", R.string.thing_smart_scene_edit_lessthan),
    Pair(">", R.string.thing_smart_scene_edit_morethan)
)

val timerTypeMap = mapOf(
    TimerType.MODE_REPEAT_WEEKDAY.type to R.string.weekday,
    TimerType.MODE_REPEAT_WEEKEND.type to R.string.weekend,
    TimerType.MODE_REPEAT_EVERYDAY.type to R.string.everyday,
    TimerType.MODE_REPEAT_ONCE.type to R.string.clock_timer_once
)

val sunSetMap = mapOf(
    "sunset" to SunSetRiseRule.SunType.SUNSET,
    "sunrise" to SunSetRiseRule.SunType.SUNRISE,
)

/**
 * 构造默认定时表达式
 */
fun getDefaultTimer(context: Context): TimerExpression {
    val calendar = Calendar.getInstance()
    return TimerExpression(
        loops = TimerType.MODE_REPEAT_ONCE.type,
        time = "${
            String.format(Locale.US, "%02d", calendar.get(Calendar.HOUR_OF_DAY))
        }:${
            String.format(Locale.US, "%02d", calendar.get(Calendar.MINUTE))
        }",
        timeZoneId = calendar.timeZone.id,
        date = try {
            String.format(Locale.US, "%04d%02d%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
        } catch (e: Exception) {
            SimpleDateFormat(context.getString(R.string.thing_scene_date), Locale.US).format(calendar.time)
        }
    )
}

/**
 * 是否包含地理围栏
 */
fun isContainGeofence(conditions: List<SceneCondition>?): Boolean =
    conditions?.any { it.entityType == CONDITION_TYPE_GEO_FENCING } ?: false

/**
 * 是否是一键执行
 */
fun isManual(conditionList: List<SceneCondition>?): Boolean {
    return conditionList.isNullOrEmpty() || (conditionList.size == 1 && conditionList[0].entityType == CONDITION_TYPE_MANUAL)
}