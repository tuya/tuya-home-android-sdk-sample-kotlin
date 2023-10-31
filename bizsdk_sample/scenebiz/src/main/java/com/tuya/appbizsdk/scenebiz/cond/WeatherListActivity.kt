package com.tuya.appbizsdk.scenebiz.cond

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.thingclips.scene.core.bean.ConditionBase
import com.thingclips.scene.core.protocol.b.usualimpl.SunRiseSetConditionBuilder
import com.thingclips.scene.core.protocol.b.usualimpl.WeatherConditionBuilder
import com.thingclips.smart.family.FamilyManagerCoreKit
import com.thingclips.smart.family.base.BizResponseData
import com.thingclips.smart.family.bean.FamilyBean
import com.thingclips.smart.family.callback.IFamilyDataCallback
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.scene.api.IResultCallback
import com.thingclips.smart.scene.api.service.IConditionService
import com.thingclips.smart.scene.model.condition.ConditionItemList
import com.thingclips.smart.scene.model.condition.SceneCondition
import com.thingclips.smart.scene.model.condition.WeatherData
import com.thingclips.smart.scene.model.condition.WeatherEnumData
import com.thingclips.smart.scene.model.condition.WeatherValueData
import com.thingclips.smart.scene.model.constant.CONDITION_TYPE_WEATHER_SUN
import com.thingclips.smart.scene.model.constant.DatapointType
import com.thingclips.smart.scene.model.constant.WeatherType
import com.tuya.appbizsdk.scenebiz.R
import com.tuya.appbizsdk.scenebiz.databinding.WeatherListActivityBinding
import com.tuya.appbizsdk.scenebiz.extensions.sunSetMap
import com.tuya.appbizsdk.scenebiz.extensions.valueStatusMap
import com.tuya.appbizsdk.scenebiz.util.TempUtil

class WeatherListActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "WeatherListActivity"
        const val KEY_RESULT_DATA = "weather_condition"
    }

    private lateinit var binding: WeatherListActivityBinding
    private val conditionService: IConditionService? = ThingHomeSdk.getSceneServiceInstance()?.conditionService()

    private lateinit var weatherListAdapter: WeatherListAdapter
    private var weatherListData: List<WeatherData> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = WeatherListActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.run {
            setNavigationOnClickListener {
                finish()
            }
        }

        weatherListAdapter = WeatherListAdapter { entityType, weatherType ->
            chooseWeatherType(entityType, weatherType)
        }
        binding.rvWeatherList.adapter = weatherListAdapter

        FamilyManagerCoreKit.getFamilyUseCase().getCurrentDefaultFamilyDetail(object : IFamilyDataCallback<BizResponseData<FamilyBean>> {
            override fun onSuccess(result: BizResponseData<FamilyBean>?) {
                //successful return result。
                val gid = result?.data?.homeId
                if (gid == null) {
                    val msg = "gid is null"
                    Log.e(TAG, msg)
                    Toast.makeText(this@WeatherListActivity, msg, Toast.LENGTH_LONG).show()
                    return
                }
                gid.let {
                    conditionService?.getConditionAll(it, false, "", object : IResultCallback<ConditionItemList?> {
                        override fun onError(errorCode: String?, errorMessage: String?) {
                            val msg = "getConditionAll, errCode: $errorCode, errMsg: $errorMessage"
                            Log.e(TAG, msg)
                            Toast.makeText(this@WeatherListActivity, msg, Toast.LENGTH_LONG).show()
                        }

                        override fun onSuccess(result: ConditionItemList?) {
                            getWeatherList(result).also { weatherList ->
                                weatherListData = weatherList
                            }.map { weatherData ->
                                WeatherItemData(weatherData.entityType, weatherData.entitySubId, weatherData.entityName)
                            }.run {
                                weatherListAdapter.submitList(this)
                            }
                        }
                    })
                }
            }

            override fun onError(errcode: String?, errMsg: String?) {
                val msg = "getCurrentDefaultFamilyDetail, errCode: $errcode, errMsg: $errMsg"
                Log.e(TAG, msg)
                Toast.makeText(this@WeatherListActivity, msg, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun getWeatherList(list: ConditionItemList?): List<WeatherData> {
        return list?.envConds?.map { condition ->
            val enumDataList = condition.valueRangeJson?.mapIndexed { index, data ->
                WeatherEnumData(
                    weatherSubType = data[0].toString(),
                    weatherName = data[1].toString(),
                    checked = index == 0
                )

            } ?: emptyList()

            val valueData = WeatherValueData(
                value = condition.property?.property?.min ?: 0,
                operators = JSONObject.parseArray(condition.operators, String::class.java)
                    ?.mapIndexed { index, s -> Pair(s, index == 0) } ?: emptyList(),
                unit = condition.property?.property?.unit ?: "",
                min = condition.property?.property?.min ?: 0,
                max = condition.property?.property?.max ?: 0,
                step = condition.property?.property?.step ?: 0
            )

            WeatherData(
                icon = condition.newIcon,
                entitySubId = condition.entitySubId,
                entityName = condition.entityName,
                entityType = condition.entityType,
                valueData = valueData,
                enumData = enumDataList,
                datapointType = DatapointType.getByValue(condition.property?.property?.type) ?: DatapointType.DATAPOINT_TYPE_ENUM,
                sceneCondition = null
            )
        } ?: emptyList()
    }

    private fun chooseWeatherType(entityTypeParam: Int, type: String) {
        val cityId = "1001803662567473213"
        val cityName = "杭州"
        val operator: String
        val chooseValue: Any
        var exprDis = ""
        var entityType = entityTypeParam
        // e.g. to use sunsetriseTimer, we can set value not equal to 16
        val sunTimer = 19
        val wData: WeatherData? = weatherListData.find { it.entitySubId == type }
        val weatherType = WeatherType.getByType(type) ?: WeatherType.WEATHER_TYPE_TEMP
        var finalWeatherType = weatherType
        wData?.let {
            // 条件基类构造
            val conditionBase: ConditionBase? = when (weatherType) {
                WeatherType.WEATHER_TYPE_WIND -> {
                    val valueData = wData.valueData
                    valueData?.let { data ->
                        operator = data.operators.find { pair -> pair.second }?.first ?: ""
                        exprDis = "${wData.entityName} : " +
                                "${valueStatusMap[operator]?.let { stringId -> getString(stringId) }}" +
                                "${data.value}${data.unit}"
                        chooseValue = data.value
                        WeatherConditionBuilder(
                            cityId = cityId,
                            cityName = cityName,
                            entityType = entityType,
                            weatherType = finalWeatherType,
                            operator = operator,
                            chooseValue = chooseValue,
                        )
                            .setWindSpeedUnit(data.unit)
                            .build() as ConditionBase
                    }
                }

                WeatherType.WEATHER_TYPE_TEMP -> {
                    val valueData = wData.valueData
                    valueData?.let { data ->
                        data.value = tempValueConvert2Fahrenheit(wData)
                        operator = data.operators.find { pair -> pair.second }?.first ?: ""
                        // e.g. app set temp unit "℉"
                        exprDis = "${wData.entityName} : " +
                                "${valueStatusMap[operator]?.let { stringId -> getString(stringId) }}" +
                                "${data.value}℉"
                        chooseValue = data.value
                        WeatherConditionBuilder(
                            cityId = cityId,
                            cityName = cityName,
                            entityType = entityType,
                            weatherType = finalWeatherType,
                            operator = operator,
                            chooseValue = chooseValue,
                        ).build() as ConditionBase
                    }
                }

                else -> {
                    val enumData = wData.enumData.find { item -> item.checked }
                    // e.g. 创建日出日落前后条件 15分钟
                    if (wData.entitySubId == WeatherType.WEATHER_TYPE_SUN.type && sunTimer != 16) {
                        enumData?.let { data ->
                            entityType = CONDITION_TYPE_WEATHER_SUN
                            finalWeatherType = WeatherType.WEATHER_TYPE_SUN_TIMER
                            exprDis = "${getString(R.string.scene_sunsetrise)}:${data.sunTimer}"
                            SunRiseSetConditionBuilder(
                                cityId,
                                sunSetMap[enumData.weatherSubType]!!,
                                valueToMinute(sunTimer)
                            ).build() as ConditionBase
                        }
                    } else {
                        enumData?.let { data ->
                            // 特殊处理日出日落
                            if (wData.entitySubId == WeatherType.WEATHER_TYPE_SUN.type ||
                                wData.entitySubId == WeatherType.WEATHER_TYPE_SUN_TIMER.type
                            ) {
                                finalWeatherType = WeatherType.WEATHER_TYPE_SUN
                            }
                            val (weatherSubType, weatherName, _) = data
                            exprDis = "${wData.entityName}:$weatherName"
                            chooseValue = weatherSubType
                            WeatherConditionBuilder(
                                cityId = cityId,
                                cityName = cityName,
                                entityType = entityType,
                                weatherType = finalWeatherType,
                                operator = null,
                                chooseValue = chooseValue,
                            ).build() as ConditionBase
                        }
                    }
                }
            }

            val weatherCondition = SceneCondition(conditionBase).apply {
                entityName = cityName
                iconUrl = wData.icon
                exprDisplay = exprDis
            }
            weatherCondition.run {
                // modify conditionBase#extraInfo
                val extra = condition.extraInfo ?: mutableMapOf()
                extra.apply {
                    if (WeatherType.WEATHER_TYPE_TEMP == finalWeatherType) {
                        wData.valueData?.value?.let { value ->
                            // e.g. app set temp unit fahrenheit
                            val tempUnit = "fahrenheit"
                            this["tempUnit"] = tempUnit

                            val tempMap = mutableMapOf<String, Int>()
                            tempMap[tempUnit] = value
                            this["convertTemp"] = tempMap
                        }
                    }

                    condition.extraInfo = this
                }

                val intent = Intent().apply {
                    putExtra(KEY_RESULT_DATA, JSON.toJSONString(weatherCondition))
                }
                setResult(RESULT_OK, intent)
                finish()
            }
        }
    }

    private fun tempValueConvert2Fahrenheit(wData: WeatherData): Int {
        // e.g. app set temp unit fahrenheit
        val appTempUnit = "fahrenheit"
        val originTempUnitSign = wData.valueData?.unit ?: "℃"
        return wData.valueData?.value?.let { value ->
            TempUtil.transferTemp(originTempUnitSign, appTempUnit, value, 0)
        } ?: 0
    }

    private fun minuteToValue(minutes: Int?): Int? {
        if (minutes == null) return null
        return if (minutes > -60 && minutes < 60) {
            minutes / 5 + 16
        } else if (minutes >= 60) {
            minutes / 60 + 11 + 16
        } else {
            minutes / 60 - 11 + 16
        }
    }

    private fun valueToMinute(sunTimer: Int): Int {
        val time = sunTimer - 16
        return when {
            time == 0 -> {
                time
            }

            time in 1..11 -> {
                time * 5
            }

            time > 11 -> {
                (time - 11) * 60
            }

            time >= -11 -> {
                time * 5
            }

            else -> {
                (time + 11) * 60
            }
        }
    }
}