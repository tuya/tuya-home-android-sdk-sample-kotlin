package com.tuya.appbizsdk.scenebiz.detail

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.thingclips.scene.core.bean.ConditionBase
import com.thingclips.scene.core.enumerate.ConditionEntityType
import com.thingclips.scene.core.protocol.b.usualimpl.DeviceConditionBuilder
import com.thingclips.scene.core.protocol.b.usualimpl.GeofenceConditionBuilder
import com.thingclips.scene.core.protocol.b.usualimpl.TimingConditionBuilder
import com.thingclips.scene.core.tool.mapToDeviceConditionData
import com.thingclips.smart.family.FamilyManagerCoreKit
import com.thingclips.smart.family.base.BizResponseData
import com.thingclips.smart.family.bean.FamilyBean
import com.thingclips.smart.family.callback.IFamilyDataCallback
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.scene.api.IResultCallback
import com.thingclips.smart.scene.api.service.IDeviceService
import com.thingclips.smart.scene.api.service.IExtService
import com.thingclips.smart.scene.lib.util.DeviceUtil
import com.thingclips.smart.scene.model.condition.ConditionItemDetail
import com.thingclips.smart.scene.model.condition.SceneCondition
import com.thingclips.smart.scene.model.constant.CONDITION_TYPE_DEVICE
import com.thingclips.smart.scene.model.constant.CONDITION_TYPE_GEO_FENCING
import com.thingclips.smart.scene.model.constant.CONDITION_TYPE_LOCK
import com.thingclips.smart.scene.model.constant.CONDITION_TYPE_MANUAL
import com.thingclips.smart.scene.model.constant.CONDITION_TYPE_PIR
import com.thingclips.smart.scene.model.constant.CONDITION_TYPE_TIMER
import com.thingclips.smart.scene.model.constant.CONDITION_TYPE_WEATHER
import com.thingclips.smart.scene.model.constant.CONDITION_TYPE_WITH_TIME
import com.thingclips.smart.scene.model.constant.COND_TYPE_DURATION
import com.thingclips.smart.scene.model.constant.DatapointType
import com.thingclips.smart.scene.model.constant.GeofencingType
import com.thingclips.smart.scene.model.device.DeviceConditionData
import com.thingclips.smart.scene.model.device.OtherTypeData
import com.thingclips.smart.scene.model.device.SchemaExt
import com.thingclips.smart.sdk.bean.DeviceBean
import com.tuya.appbizsdk.scenebiz.R
import com.tuya.appbizsdk.scenebiz.cond.WeatherListActivity
import com.tuya.appbizsdk.scenebiz.databinding.ConditionDialogFragmentBinding
import com.tuya.appbizsdk.scenebiz.extensions.defaultConditionMap
import com.tuya.appbizsdk.scenebiz.extensions.getDefaultTimer
import com.tuya.appbizsdk.scenebiz.extensions.valueStatusMap
import com.tuya.appbizsdk.scenebiz.util.PercentUtil.getPercent
import com.tuya.appbizsdk.scenebiz.util.PercentUtil.getPercentFromOne
import com.tuya.appbizsdk.scenebiz.util.TempUtil
import com.tuya.appbizsdk.scenebiz.util.TempUtil.TEMPER_FAHRENHEIT_SIGN
import com.tuya.appbizsdk.scenebiz.util.TimeUtil
import com.tuya.appbizsdk.scenebiz.util.TimeUtil.secondToShowText
import kotlin.math.pow

class ConditionDialogFragment : DialogFragment() {

    private lateinit var binding: ConditionDialogFragmentBinding
    private lateinit var detailConditionTypeAdapter: DetailConditionTypeAdapter
    private val deviceService: IDeviceService? = ThingHomeSdk.getSceneServiceInstance()?.deviceService()
    private val extService: IExtService? = ThingHomeSdk.getSceneServiceInstance()?.extService()

    // 记录设备温度Dp的显示单位
    var showTempUnit: String? = null

    // 设备DP原始单位统计的可选值
    var tempOriginRangeList: List<Int>? = null

    // 设备DP（温标转换）显示单位统计的可选值
    var tempConvertRangeList: List<Int>? = null

    // 显示值的可取范围，在 tempConvertRangeList 基础上加上scale
    var tempConvertWithScaleRangeList: List<String>? = null
    var dpScale: Int? = null


    lateinit var chooseWeatherResult: ActivityResultLauncher<Intent>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        chooseWeatherResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                // requireActivity().setResult(AppCompatActivity.RESULT_OK, result.data)

                result.data?.getStringExtra(WeatherListActivity.KEY_RESULT_DATA)?.let {
                    sendDataToActivity(it)
                }
            }
        }
        binding = ConditionDialogFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setOnClickListener { dismiss() }
        detailConditionTypeAdapter = DetailConditionTypeAdapter { type ->
            when (type) {
                CONDITION_TYPE_MANUAL -> {
                    val manualCondition = SceneCondition().apply {
                        entityType = CONDITION_TYPE_MANUAL
                    }
                    sendDataToActivity(JSON.toJSONString(manualCondition))
                }

                CONDITION_TYPE_GEO_FENCING -> {
                    val radius = 100
                    val lat: Double = 30.30288959184809
                    val lon: Double = 120.0640840491766
                    val address = "XX 影视"
                    val geofenceType = GeofencingType.GEOFENCING_TYPE_ENTER.type
                    val geofenceConditionBuilder = GeofenceConditionBuilder(radius, lat, lon, address, geofenceType)
                    val conditionBase = geofenceConditionBuilder.build() as ConditionBase
                    val geoCondition = SceneCondition(conditionBase).apply {
                        this.entityName = address
                    }
                    sendDataToActivity(JSON.toJSONString(geoCondition))
                }

                CONDITION_TYPE_TIMER -> {
                    val timerExpr = getDefaultTimer(requireContext())
                    val conditionBase: ConditionBase = TimingConditionBuilder(
                        timerExpr.timeZoneId,
                        timerExpr.loops,
                        timerExpr.time,
                        timerExpr.date
                    ).build() as ConditionBase
                    val timerCondition = SceneCondition(conditionBase).apply {
                        entityName = getString(R.string.timer)
                        exprDisplay = timerExpr.time
                    }
                    sendDataToActivity(JSON.toJSONString(timerCondition))
                }

                CONDITION_TYPE_WEATHER -> {
                    val intent = Intent(context, WeatherListActivity::class.java)
                    chooseWeatherResult.launch(intent)
                }

                CONDITION_TYPE_DEVICE -> {
                    chooseDevice {
                        sendDataToActivity(JSON.toJSONString(it))
                    }
                }

                CONDITION_TYPE_LOCK -> {
                    chooseLockDevice { device ->
                        val display = "${getString(R.string.scene_family_member_go_home)}:${device.name ?: ""}"
                        // e.g. mock data
                        val membersString = "小明,小李"
                        val memberIds = "1223421,1273723"
                        val conditionBase = DeviceConditionBuilder(
                            deviceId = device.devId,
                            dpId = /*devConds.value?.find { it.entityType == ConditionEntityType.LOCK_MEMBER_GO_HOME.type }?.entitySubId ?: */"",
                            entityType = ConditionEntityType.LOCK_MEMBER_GO_HOME.type,
                            deviceConditionData = null,
                            chooseValue = memberIds
                        ).setMembers(membersString)
                            .build() as ConditionBase
                        val lockCondition = SceneCondition(conditionBase).apply {
                            entityName = membersString
                            exprDisplay = display
                        }
                        sendDataToActivity(JSON.toJSONString(lockCondition))
                    }

                }
            }
        }

        binding.rvDetailCondition.adapter = detailConditionTypeAdapter
    }

    override fun onResume() {
        super.onResume()
        defaultConditionMap.values.map { ConditionTypeItemData(it.type, it.conditionIcon, getString(it.conditionName)) }.run {
            Log.i(TAG, "condition type list size: ${this.size}")
            detailConditionTypeAdapter.submitList(this)
        }
    }

    override fun getTheme(): Int {
        return R.style.FullDialog
    }

    private fun sendDataToActivity(data: String) {
        val result = Bundle()
        result.putString(KEY_DATA_KEY, data)
        arguments?.getString(KEY_REQ_KEY)?.let { parentFragmentManager.setFragmentResult(it, result) }
        dismiss()
    }

    private fun chooseLockDevice(cb: (DeviceBean) -> Unit) {
        FamilyManagerCoreKit.getFamilyUseCase().getCurrentDefaultFamilyDetail(object : IFamilyDataCallback<BizResponseData<FamilyBean>> {
            override fun onSuccess(result: BizResponseData<FamilyBean>?) {
                val msg = "getCurrentDefaultFamilyDetail onSuccess, result: $result"
                Log.i(TAG, msg)
                //successful return result。
                val gid = result?.data?.homeId
                if(gid == null){
                    val msg1 = "gid is null"
                    Log.e(TAG, msg1)
                    Toast.makeText(requireActivity(), msg1, Toast.LENGTH_LONG).show()
                    return
                }
                gid.let {
                    deviceService?.getLockDeviceIdAll(it, object : IResultCallback<List<String>?> {
                        override fun onError(errorCode: String?, errorMessage: String?) {
                            val msg1 = "getLockDeviceIdAll, errCode: $errorCode, errMsg: $errorMessage"
                            Log.e(TAG, msg1)
                            Toast.makeText(requireActivity(), msg1, Toast.LENGTH_LONG).show()
                        }

                        override fun onSuccess(result: List<String>?) {
                            val msg1 = "getLockDeviceIdAll onSuccess, lockDevices.size: ${result?.size}"
                            Log.i(TAG, msg1)
                            val lockDevSize = result?.size ?: 0
                            // e.g. choose last device
                            val chooseDevice: DeviceBean? = if (lockDevSize > 0) {
                                extService?.getDevice(result!![lockDevSize - 1])
                            } else {
                                null
                            }

                            if (chooseDevice == null) {
                                val msg2 = "no lock device chosen!"
                                Log.e(TAG, msg2)
                                Toast.makeText(requireActivity(), msg2, Toast.LENGTH_LONG).show()
                                return
                            }
                            cb(chooseDevice)
                        }
                    })
                }
            }

            override fun onError(errcode: String?, errMsg: String?) {
                val msg = "getCurrentDefaultFamilyDetail, errCode: $errcode, errMsg: $errMsg"
                Log.e(TAG, msg)
                Toast.makeText(requireActivity(), msg, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun chooseDevice(cb: (SceneCondition) -> Unit) {
        FamilyManagerCoreKit.getFamilyUseCase().getCurrentDefaultFamilyDetail(object : IFamilyDataCallback<BizResponseData<FamilyBean>> {
            override fun onSuccess(result: BizResponseData<FamilyBean>?) {
                val msg = "getCurrentDefaultFamilyDetail onSuccess, result: $result"
                Log.i(TAG, msg)
                //successful return result。
                val gid = result?.data?.homeId
                gid?.let {
                    deviceService?.getConditionDeviceAll(it, object : IResultCallback<List<DeviceBean?>?> {
                        override fun onError(errorCode: String?, errorMessage: String?) {
                            val msg1 = "getConditionDeviceAll, errCode: $errorCode, errMsg: $errorMessage"
                            Log.e(TAG, msg1)
                            Toast.makeText(requireActivity(), msg1, Toast.LENGTH_LONG).show()
                        }

                        override fun onSuccess(result: List<DeviceBean?>?) {
                            val msg1 = "getConditionDeviceAll onSuccess, devices.size: ${result?.size}"
                            Log.i(TAG, msg1)
                            val devSize = result?.size ?: 0
                            // e.g. choose last device
                            val chooseDevice: DeviceBean? = if (devSize > 0) {
                                result?.get(devSize - 1)
                            } else {
                                null
                            }

                            if (chooseDevice == null) {
                                val msg2 = "no device chosen!"
                                Log.e(TAG, msg2)
                                Toast.makeText(requireActivity(), msg2, Toast.LENGTH_LONG).show()
                                return
                            }
                            chooseDeviceDp(chooseDevice.devId, cb)
                        }
                    })
                } ?: {
                    val msg1 = "gid is null"
                    Log.e(TAG, msg1)
                    Toast.makeText(requireActivity(), msg1, Toast.LENGTH_LONG).show()
                }
            }

            override fun onError(errcode: String?, errMsg: String?) {
                val msg = "getCurrentDefaultFamilyDetail, errCode: $errcode, errMsg: $errMsg"
                Log.e(TAG, msg)
                Toast.makeText(requireActivity(), msg, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun chooseDeviceDp(deviceId: String, cb: (SceneCondition) -> Unit) {
        deviceService?.getConditionDeviceDpAll(deviceId, object : IResultCallback<List<ConditionItemDetail>?> {
            override fun onError(errorCode: String?, errorMessage: String?) {
                val msg1 = "getConditionDeviceDpAll, errCode: $errorCode, errMsg: $errorMessage"
                Log.e(TAG, msg1)
                Toast.makeText(requireActivity(), msg1, Toast.LENGTH_LONG).show()
            }

            override fun onSuccess(result: List<ConditionItemDetail>?) {
                val msg1 = "getConditionDeviceDpAll onSuccess, result.size: ${result?.size}"
                Log.i(TAG, msg1)
                val deviceConditionDetailList = mutableListOf<DeviceConditionData>()
                result?.forEachIndexed { _, function ->
                    function.mapToDeviceConditionData(deviceId).run {
                        deviceConditionDetailList.add(this)
                    }
                }
                // e.g. choose last dp of device
                val chooseDeviceActionDetail = if (deviceConditionDetailList.size > 0) {
                    deviceConditionDetailList[deviceConditionDetailList.size - 1]
                } else {
                    null
                }
                chooseDeviceActionDetail?.let {
                    // e.g. common device dp
                    mockChooseDp(it, cb)
                } ?: {
                    val msg2 = "no device dp chosen!"
                    Log.e(TAG, msg2)
                    Toast.makeText(requireActivity(), msg2, Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun mockChooseDp(oriDeviceConditionData: DeviceConditionData, cb: (SceneCondition) -> Unit) {
        if (oriDeviceConditionData.datapointType == DatapointType.DATAPOINT_TYPE_VALUE && TempUtil.isTempUnit(
                oriDeviceConditionData.valueTypeData?.unit ?: ""
            )
        ) {
            // 温度类型需要做单位转换
            oriDeviceConditionData.valueTypeData?.let { valueData ->
                // 初始化设备温标显示单位
                showTempUnit = TempUtil.getShowTempUnit(requireContext(), oriDeviceConditionData.deviceId)
                // 初始化原始值区间/显示值区间
                val tripple = TempUtil.generateTempValueList(
                    valueData.min,
                    valueData.max,
                    valueData.step,
                    valueData.scale,
                    valueData.unit,
                    showTempUnit!!
                )
                tempOriginRangeList = tripple.first
                tempConvertRangeList = tripple.second
                tempConvertWithScaleRangeList = tripple.third
                dpScale = valueData.scale
                // 初始化当前值：温度的当前值用索引来表示
                valueData.value = tempOriginRangeList!!.indexOf(valueData.value)
            }
        }

        // e.g. checked last enum option if list exist and not empty
        val otherList: MutableList<OtherTypeData>? = oriDeviceConditionData.otherTypeData?.toMutableList()
        val size: Int? = otherList?.size
        oriDeviceConditionData.otherTypeData?.forEachIndexed { index, otherTypeData ->
            if (index == size!! - 1) {
                var durationTime: String? = null
                // e.g. pir calculate seconds: 62
                if (otherTypeData.virtualItem == true && COND_TYPE_DURATION == oriDeviceConditionData.extraInfo?.calType) {
                    oriDeviceConditionData.extraInfo?.timeWindow = 62
                    durationTime = TimeUtil.secondToShowText(requireContext(), 62)
                }
                OtherTypeData(
                    datapointOption = otherTypeData.datapointOption,
                    datapointKey = otherTypeData.datapointKey,
                    checked = true,
                    virtualItem = otherTypeData.virtualItem,
                    durationTime = durationTime ?: otherTypeData.durationTime
                ).run {
                    otherList.set(index, this)
                }
            } else {
                otherList[index] = otherTypeData
            }
        }
        val chosenDeviceConditionData = if (otherList?.isNotEmpty() == true) {
            DeviceConditionData(
                deviceId = oriDeviceConditionData.deviceId,
                datapointType = oriDeviceConditionData.datapointType,
                datapointId = oriDeviceConditionData.datapointId,
                datapointName = oriDeviceConditionData.datapointName,
                deviceIcon = oriDeviceConditionData.deviceIcon,
                valueTypeData = oriDeviceConditionData.valueTypeData,
                otherTypeData = otherList,
                extraInfo = oriDeviceConditionData.extraInfo,
                entityType = oriDeviceConditionData.entityType,
                mcGroups = oriDeviceConditionData.mcGroups
            )
        } else {
            oriDeviceConditionData
        }

        DeviceUtil.getDevice(chosenDeviceConditionData.deviceId)?.let { deviceBean ->
            val otherTypeData = chosenDeviceConditionData.otherTypeData?.find { it.checked }
            val chooseValue: Int? =
                if (TempUtil.isTempUnit(chosenDeviceConditionData.valueTypeData?.unit ?: "")) {
                    //温度类型: conditionData.valueTypeData?.value 塞的是index
                    tempOriginRangeList?.get(chosenDeviceConditionData.valueTypeData?.value ?: 0)
                } else {
                    chosenDeviceConditionData.valueTypeData?.value
                }
            // entityType 修正
            val bIsDeviceDuration = COND_TYPE_DURATION == chosenDeviceConditionData.extraInfo?.calType && otherTypeData?.virtualItem == true
            if (bIsDeviceDuration) {
                chosenDeviceConditionData.entityType = CONDITION_TYPE_WITH_TIME
            }
            val builder = DeviceConditionBuilder(
                deviceId = chosenDeviceConditionData.deviceId,
                dpId = chosenDeviceConditionData.datapointId.toString(),
                entityType = chosenDeviceConditionData.entityType ?: CONDITION_TYPE_DEVICE,
                deviceConditionData = chosenDeviceConditionData,
                chooseValue = chooseValue
            )

            // extraInfo 相关字段处理
            if (TempUtil.isTempUnit(chosenDeviceConditionData.valueTypeData?.unit)) {
                // e.g. app set temp unit to TEMPER_FAHRENHEIT_SIGN
                val tempUnit = TempUtil.getTempUnitBySign(showTempUnit ?: TEMPER_FAHRENHEIT_SIGN)
                val originTempUnit = TempUtil.getTempUnitBySign(chosenDeviceConditionData.valueTypeData?.unit ?: "")
                val dpScale = chosenDeviceConditionData.valueTypeData?.scale ?: 0
                val tempMap = mutableMapOf<String, Int>()
                tempMap[TempUtil.getTempUnitBySign(showTempUnit ?: TEMPER_FAHRENHEIT_SIGN)] =
                    tempConvertRangeList?.get(chosenDeviceConditionData.valueTypeData?.value ?: 0) ?: 0
                tempMap[TempUtil.getTempUnitBySign(chosenDeviceConditionData.valueTypeData?.unit ?: TEMPER_FAHRENHEIT_SIGN)] =
                    tempOriginRangeList?.get(chosenDeviceConditionData.valueTypeData?.value ?: 0) ?: 0
                builder.setConvertTemp(tempMap)
                    .setTempUnit(tempUnit)
                    .setOriginTempUnit(originTempUnit)
                    .setDpScale(dpScale)
            }
            if (bIsDeviceDuration) {
                builder.setCalType(COND_TYPE_DURATION)
                builder.setTimeWindow(chosenDeviceConditionData.extraInfo?.timeWindow ?: 0)
            }
            if (chosenDeviceConditionData.entityType == CONDITION_TYPE_PIR) {
                builder.setDelayTime(otherTypeData?.datapointKey ?: "")
            }

            val displayString = if (chosenDeviceConditionData.datapointType == DatapointType.DATAPOINT_TYPE_VALUE) {
                getDisplay(chosenDeviceConditionData) ?: ""
            } else if (chosenDeviceConditionData.datapointType == DatapointType.DATAPOINT_TYPE_RAW ||
                chosenDeviceConditionData.datapointType == DatapointType.DATAPOINT_TYPE_STRING
            ) {
                chosenDeviceConditionData.datapointName
            } else if (bIsDeviceDuration) {
                "${chosenDeviceConditionData.datapointName}\"${getString(R.string.thing_scene_dp_duration)}: " +
                        secondToShowText(requireContext(), chosenDeviceConditionData.extraInfo?.timeWindow?.toInt() ?: 0)
            } else "${chosenDeviceConditionData.datapointName}:${otherTypeData?.datapointOption}"

            val conditionBase = builder.build() as ConditionBase
            val deviceCondition = SceneCondition(conditionBase).apply {
                entityName = deviceBean.name
                entitySubIds = chosenDeviceConditionData.datapointId.toString()
                iconUrl = deviceBean.getIconUrl()
                exprDisplay = displayString
            }

            cb.invoke(deviceCondition)
        }
    }

    private fun getDisplay(data: DeviceConditionData): String? {
        val valueData = data.valueTypeData
        val exprDisplay = valueData?.let { typeData ->
            val operator = typeData.operators.find { it.second }?.first

            val schemaMap = getSchemaMap(data.deviceId)
            val schema = schemaMap[data.datapointId.toInt()]
            val currentValue = valueData.value
            val showValue = when (schema?.inputType) {
                VALUE_TYPE_COUNTDOWN, VALUE_TYPE_COUNTDOWN_SECOND -> {
                    val stringBuilder = StringBuilder()
                    stringBuilder.append(secondToShowText(requireContext(), currentValue))
                    stringBuilder.toString()
                }

                VALUE_TYPE_PERCENT -> {
                    getPercent(
                        typeData.value,
                        typeData.min, typeData.max
                    )
                }

                VALUE_TYPE_PERCENT_SECOND -> {
                    getPercentFromOne(
                        typeData.value,
                        typeData.min, typeData.max
                    )
                }

                else -> {
                    if (TempUtil.isTempUnit(typeData.unit)) {
                        //温度
                        "${tempConvertWithScaleRangeList?.get(typeData.value) ?: ""}${showTempUnit}"
                    } else {
                        val scale = 10.0.pow(typeData.scale)
                        if (scale == 1.0) {
                            "${typeData.value}${typeData.unit}"
                        } else "${String.format("%.${typeData.scale}f", typeData.value / scale)}${typeData.unit}"
                    }

                }
            }
            "${data.datapointName} : ${valueStatusMap[operator]?.let { getString(it) }}$showValue"
        }
        return exprDisplay
    }

    /**
     * 获取deviceBean 的 Schema ， 用于判断设备dp类型
     * 新的数据结构中会下发valueType
     */
    fun getSchemaMap(devId: String): Map<Int, SchemaExt> {
        val mSchemaExtMap = hashMapOf<Int, SchemaExt>()
        val dev = extService?.getDevice(devId)
        if (dev != null && !TextUtils.isEmpty(dev.schemaExt)) {
            val schemaExtList = JSONArray.parseArray(dev.schemaExt, SchemaExt::class.java)
            schemaExtList?.let {
                schemaExtList.map {
                    mSchemaExtMap.put(it.id, it)
                }
            }
        }
        return mSchemaExtMap
    }

    companion object {
        const val TAG = "ConditionDialogFragment"
        const val KEY_REQ_KEY = "requestKey"
        const val KEY_DATA_KEY = "condition_data_key"

        const val VALUE_TYPE_COUNTDOWN = "countdown"
        const val VALUE_TYPE_COUNTDOWN_SECOND = "countdown1"
        const val VALUE_TYPE_PERCENT = "percent"
        const val VALUE_TYPE_PERCENT_SECOND = "percent1"

        @JvmStatic
        fun newInstance(requestKey: String) = ConditionDialogFragment().apply {
            arguments = Bundle().apply {
                putString(KEY_REQ_KEY, requestKey)
            }
        }
    }
}