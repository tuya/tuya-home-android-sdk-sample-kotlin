package com.tuya.appbizsdk.scenebiz.util

import android.content.Context
import android.text.TextUtils
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.scene.api.service.IExtService
import com.tuya.appbizsdk.scenebiz.R
import java.math.BigDecimal
import kotlin.math.pow
import kotlin.math.roundToInt

object TempUtil {
    //气象条件会传来这个大°F单位
    const val TEMPER_FAHRENHEIT_SIGN1: String = "°F"
    const val TEMPER_CELSIUS_SIGN1: String = "°C"

    val extService: IExtService? = ThingHomeSdk.getSceneServiceInstance()?.extService()
    val TEMPER_FAHRENHEIT_UNIT = arrayOf(
        "°F",
        "℉",
        "fahrenheit", "˚F"
    )
    val TEMPER_CELSIUS_UNIT = arrayOf(
        "°C",
        "celsius",
        "℃", "˚C"
    )

    var TEMPER_FAHRENHEIT = "fahrenheit"
    var TEMPER_CELSIUS = "celsius"
    var TEMPER_FAHRENHEIT_SIGN = "℉"
    var TEMPER_CELSIUS_SIGN = "℃"

    /**
     * 是否是温度单位
     */
    fun isTempUnit(unit: String?): Boolean {
        return unit != null && (isFahrenheitTempUnit(unit) || isCelsiusTempUnit(unit))
    }

    fun isFahrenheitTempUnit(unit: String?): Boolean {
        return unit != null && (unit in TEMPER_FAHRENHEIT_UNIT)
    }

    fun isCelsiusTempUnit(unit: String?): Boolean {
        return unit != null && (unit in TEMPER_CELSIUS_UNIT)
    }

    /**
     * 返回温度单位的全名
     */
    fun getTempUnitBySign(sign: String): String {
        return if (sign in TEMPER_FAHRENHEIT_UNIT) TEMPER_FAHRENHEIT else TEMPER_CELSIUS
    }

    fun getShowTempUnit(context: Context, devId: String): String {
        val deviceBean = extService?.getDevice(devId)

        val schemaBeanMap = deviceBean?.getSchemaMap()
        if (schemaBeanMap.isNullOrEmpty().not()) {
            for (value in schemaBeanMap?.values ?: emptyList()) {
                if (TextUtils.equals(value.code, "temp_unit_convert")) {
                    deviceBean?.dps?.get(value.id)?.let {
                        // 2019年的温标方案dp类型是bool。规则：一般都是98%以上，true 表示 ℃ 【一个字符，不是° 和C的组合】，false 表示 ℉ 【一个字符，不是° 和F的组合】（2%可能true是℉就不考虑了）。
                        return@getShowTempUnit if (value.schemaType == "bool") {
                            if (TextUtils.equals(it.toString(), "true")) {
                                TEMPER_CELSIUS_SIGN1
                            } else {
                                TEMPER_FAHRENHEIT_SIGN1
                            }
                        } else {
                            // 之后的温标方案dp类型是enum。c 表示 摄氏度， f 表示 华氏度
                            if (TextUtils.equals(it.toString(), context.getString(R.string.thing_temp_celsius))) {
                                TEMPER_CELSIUS_SIGN1
                            } else {
                                TEMPER_FAHRENHEIT_SIGN1
                            }
                        }
                    }
                }
            }
        }

        // e.g. app temp unit use fahrenheit
        return TEMPER_FAHRENHEIT_SIGN
    }

    /**
     * 返回原始单位 和 显示单位对应的取值区间
     * return: pair(tempOriginRangeList, tempConvertRangeList )
     */
    fun generateTempValueList(
        min: Int,
        max: Int,
        step: Int,
        scale: Int,
        originTempUnit: String,
        showTempUnit: String
    ): Triple<List<Int>, List<Int>, List<String>> {
        //根据设备原始单位统计的可选值
        val tempOriginRangeList = mutableListOf<Int>()
        //根据显示单位统计的可选值
        var tempConvertRangeList = mutableListOf<Int>()
        //用于ui显示
        val tempConvertWithScaleRangeList = mutableListOf<String>()


        for (index in min..max step step) {
            tempOriginRangeList.add(index)
        }
        if (tempOriginRangeList.last() < max) {
            tempOriginRangeList.add(max)
        }

        if (isTempEqual(originTempUnit, showTempUnit)) {
            //单位相同
            tempConvertRangeList = tempOriginRangeList
        } else {
            //单位不同
            for (originValue in tempOriginRangeList) {
                tempConvertRangeList.add(transferTemp(originTempUnit, showTempUnit, originValue, scale))
            }
        }


        for (convertValue in tempConvertRangeList) {
            tempConvertWithScaleRangeList.add(transferShowTemp(convertValue, scale))
        }

        return Triple(tempOriginRangeList, tempConvertRangeList, tempConvertWithScaleRangeList)
    }

    /**
     * 摄氏度转华氏度
     */
    private fun celsiusToFahrenheit(value: Double): Float {
        val add = BigDecimal(value).multiply(BigDecimal("1.8")).add(BigDecimal(32))
        return add.toFloat()
    }

    /**
     * 华氏度转摄氏度
     */
    private fun fahrenheitToCelsius(value: Double): Float {
        val divide = BigDecimal(value).subtract(BigDecimal(32)).divide(BigDecimal("1.8"), 5, BigDecimal.ROUND_FLOOR)
        return divide.toFloat()
    }

    /**
     * 判断温度单位是否相同
     */
    fun isTempEqual(fromTempUnit: String, toTempUnit: String): Boolean {
        return (fromTempUnit in TEMPER_CELSIUS_UNIT && toTempUnit in TEMPER_CELSIUS_UNIT) ||
                (fromTempUnit in TEMPER_FAHRENHEIT_UNIT && toTempUnit in TEMPER_FAHRENHEIT_UNIT)

    }

    /**
     * 根据传入单位换算
     */
    fun transferTemp(fromTempUnit: String, toTempUnit: String, temp: Int, scale: Int): Int {
        val tranScale = if (scale != 0) 10.0.pow(scale.toDouble()) else 1.toDouble()
        val transValue = if (scale != 0) temp.toFloat() / tranScale else temp.toFloat()

        return if (isTempEqual(fromTempUnit, toTempUnit)) {
            //单位相同
            temp
        } else if (fromTempUnit in TEMPER_CELSIUS_UNIT && toTempUnit in TEMPER_FAHRENHEIT_UNIT) {
            //c->f
            val t = transferCelsiusToFahrenheit(transValue.toDouble())
            return if (scale != 0) (t * tranScale).roundToInt() else t.roundToInt()
        } else {
            //f->c
            val t = transferFahrenheitToCelsius(transValue.toDouble())
            return if (scale != 0) (t * tranScale).roundToInt() else t.roundToInt()
        }
    }

    /**
     * C -> F
     * F = C×1.8+32
     * 向上取整
     */
    fun transferCelsiusToFahrenheit(celsius: Double): Double {
        return celsiusToFahrenheit(celsius).toDouble()
    }

    /**
     * F -> C
     * C = (F-32)÷1.8
     * 向下取整
     */
    fun transferFahrenheitToCelsius(fahrenheit: Double): Double {
        return fahrenheitToCelsius(fahrenheit).toDouble()
    }

    /**
     * 按照精度转换出显示值
     */
    fun transferShowTemp(temp: Int, scale: Int): String {
        val tranScale = if (scale != 0) 10.0.pow(scale.toDouble()) else 1.toDouble()
        val transValue = if (scale != 0) temp.toFloat() / tranScale else temp.toFloat()
        return String.format("%.${scale}f", transValue)
    }


    fun showTransferToOrigin(originTempUnit: String, targetUnit: String, transferValue: String, scale: Int): Int {
        val wrapperOriginUnit = if (isFahrenheitTempUnit(originTempUnit)) TEMPER_FAHRENHEIT else TEMPER_CELSIUS

        return if (TextUtils.equals(wrapperOriginUnit, targetUnit)) {
            if (scale == 0) {
                transferValue.toInt()
            } else {
                (transferValue.toFloat() * 10.0.pow(scale)).toInt()
            }
        } else {
            var originValue = if (TextUtils.equals(
                    wrapperOriginUnit,
                    TEMPER_CELSIUS
                )
            ) {
                fahrenheitToCelsius(transferValue.toDouble())

            } else {
                celsiusToFahrenheit(transferValue.toDouble())
            }

            if (scale == 0) {
                originValue.toInt()
            } else {
                (originValue * 10.0.pow(scale)).toInt()
            }
        }
    }
}