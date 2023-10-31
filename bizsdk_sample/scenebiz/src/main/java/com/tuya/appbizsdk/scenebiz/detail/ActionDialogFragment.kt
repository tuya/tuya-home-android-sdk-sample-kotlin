package com.tuya.appbizsdk.scenebiz.detail

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.alibaba.fastjson.JSON
import com.thingclips.scene.core.bean.ActionBase
import com.thingclips.scene.core.protocol.b.usualimpl.DelayActionBuilder
import com.thingclips.scene.core.protocol.b.usualimpl.DeviceActionBuilder
import com.thingclips.scene.core.protocol.b.usualimpl.NotifyActionBuilder
import com.thingclips.scene.core.tool.mapToDeviceActionData
import com.thingclips.smart.family.FamilyManagerCoreKit
import com.thingclips.smart.family.base.BizResponseData
import com.thingclips.smart.family.bean.FamilyBean
import com.thingclips.smart.family.callback.IFamilyDataCallback
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.scene.api.IResultCallback
import com.thingclips.smart.scene.api.service.IDeviceService
import com.thingclips.smart.scene.api.service.IExtService
import com.thingclips.smart.scene.model.action.SceneAction
import com.thingclips.smart.scene.model.constant.ACTION_TYPE_DELAY
import com.thingclips.smart.scene.model.constant.ACTION_TYPE_DEVICE
import com.thingclips.smart.scene.model.constant.ACTION_TYPE_DEVICE_GROUP
import com.thingclips.smart.scene.model.constant.ACTION_TYPE_MESSAGE
import com.thingclips.smart.scene.model.constant.ACTION_TYPE_PHONE
import com.thingclips.smart.scene.model.constant.ACTION_TYPE_TRIGGER
import com.thingclips.smart.scene.model.constant.DatapointType
import com.thingclips.smart.scene.model.constant.DeviceDpValueType
import com.thingclips.smart.scene.model.constant.DeviceType
import com.thingclips.smart.scene.model.device.ActionDeviceDataPointList
import com.thingclips.smart.scene.model.device.ActionDeviceGroup
import com.thingclips.smart.scene.model.device.DeviceActionDetailBean
import com.thingclips.smart.scene.model.device.DpCodeType
import com.thingclips.smart.sdk.bean.DeviceBean
import com.tuya.appbizsdk.scenebiz.R
import com.tuya.appbizsdk.scenebiz.act.LinkageChooseListActivity
import com.tuya.appbizsdk.scenebiz.databinding.ActionDialogFragmentBinding
import com.tuya.appbizsdk.scenebiz.extensions.generateConstructActionList
import com.tuya.appbizsdk.scenebiz.util.PercentUtil
import com.tuya.appbizsdk.scenebiz.util.TimeUtil
import kotlin.math.pow

class ActionDialogFragment : DialogFragment() {
    companion object {
        const val TAG = "ActionDialogFragment"
        const val KEY_REQ_KEY = "requestKey"
        const val KEY_DATA_KEY = "actions_data_key"

        @JvmStatic
        fun newInstance(requestKey: String) = ActionDialogFragment().apply {
            arguments = Bundle().apply {
                putString(KEY_REQ_KEY, requestKey)
            }
        }
    }

    private lateinit var binding: ActionDialogFragmentBinding
    private lateinit var detailActionTypeAdapter: DetailActionTypeAdapter
    private val deviceService: IDeviceService? = ThingHomeSdk.getSceneServiceInstance()?.deviceService()
    private val extService: IExtService? = ThingHomeSdk.getSceneServiceInstance()?.extService()


    lateinit var chooseForLinkageResult: ActivityResultLauncher<Intent>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        chooseForLinkageResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                // requireActivity().setResult(AppCompatActivity.RESULT_OK, result.data)

                result.data?.getStringExtra(LinkageChooseListActivity.KEY_RESULT_DATA)?.let {
                    sendDataToActivity(it)
                }
            }
        }
        binding = ActionDialogFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.setOnClickListener { dismiss() }
        detailActionTypeAdapter = DetailActionTypeAdapter { actionType ->
            when (actionType) {
                ACTION_TYPE_DELAY -> {
                    val actionBase: ActionBase = DelayActionBuilder(62, 20).build() as ActionBase
                    val delayAction = SceneAction(actionBase).apply {
                        entityName = getString(R.string.scene_delay)
                    }

                    sendDataToActivity(JSON.toJSONString(listOf(delayAction)))
                }

                ACTION_TYPE_MESSAGE -> {
                    // val unableTip = getString(R.string.phone_notify_expired)
                    // val actionBase: ActionBase = NotifyActionBuilder(ACTION_TYPE_PHONE).build() as ActionBase
                    // val notifyAction = SceneAction(actionBase).apply {
                    //     entityName = getString(R.string.scene_phone_notice)
                    //     if (unableTip.isEmpty().not()) {
                    //         actionDisplayNew = mutableMapOf(
                    //             "voice_package_has_expired" to listOf(unableTip),
                    //             "package_has_expired" to listOf(unableTip)
                    //         )
                    //     }
                    // }
                    val actionBase: ActionBase = NotifyActionBuilder(ACTION_TYPE_MESSAGE).build() as ActionBase
                    val notifyAction = SceneAction(actionBase).apply {
                        entityName = getString(R.string.scene_push_message_phone)
                    }

                    sendDataToActivity(JSON.toJSONString(listOf(notifyAction)))
                }

                ACTION_TYPE_TRIGGER -> {
                    val intent = Intent(context, LinkageChooseListActivity::class.java)
                    chooseForLinkageResult.launch(intent)
                }

                ACTION_TYPE_DEVICE -> {
                    chooseDevice {
                        sendDataToActivity(JSON.toJSONString(listOf(it)))
                    }
                }
            }
        }

        binding.rvDetailAction.adapter = detailActionTypeAdapter
    }

    override fun onResume() {
        super.onResume()
        generateConstructActionList(false, null).map { ActionTypeItemData(it.actionType, it.actionIcon, getString(it.actionName)) }.run {
            Log.i(TAG, "action type list size: ${this.size}")
            detailActionTypeAdapter.submitList(this)
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

    private fun chooseDevice(cb: (SceneAction) -> Unit) {
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
                    deviceService?.getActionDeviceAll(it, object : IResultCallback<ActionDeviceGroup?> {
                        override fun onError(errorCode: String?, errorMessage: String?) {
                            val msg1 = "getActionDeviceAll, errCode: $errorCode, errMsg: $errorMessage"
                            Log.e(TAG, msg1)
                            Toast.makeText(requireActivity(), msg1, Toast.LENGTH_LONG).show()
                        }

                        override fun onSuccess(result: ActionDeviceGroup?) {
                            val msg1 = "getActionDeviceAll onSuccess, devices.size: ${result?.devices?.size}, groups.size: ${result?.groups?.size}"
                            Log.i(TAG, msg1)
                            val devSize = result?.devices?.size ?: 0
                            // e.g. choose last device
                            val chooseDevice: DeviceBean? = if (devSize > 0) {
                                result?.devices?.get(devSize - 1)
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
                }
            }

            override fun onError(errcode: String?, errMsg: String?) {
                val msg = "getCurrentDefaultFamilyDetail, errCode: $errcode, errMsg: $errMsg"
                Log.e(TAG, msg)
                Toast.makeText(requireActivity(), msg, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun chooseDeviceDp(deviceId: String, cb: (SceneAction) -> Unit) {
        deviceService?.getActionDeviceDpAll(deviceId, object : IResultCallback<List<ActionDeviceDataPointList>?> {
            override fun onError(errorCode: String?, errorMessage: String?) {
                val msg1 = "getActionDeviceDpAll, errCode: $errorCode, errMsg: $errorMessage"
                Log.e(TAG, msg1)
                Toast.makeText(requireActivity(), msg1, Toast.LENGTH_LONG).show()
            }

            override fun onSuccess(result: List<ActionDeviceDataPointList>?) {
                val msg1 = "getActionDeviceDpAll onSuccess, result.size: ${result?.size}"
                Log.i(TAG, msg1)
                val deviceActionDetailList = mutableListOf<DeviceActionDetailBean>()
                result?.forEachIndexed { _, function ->
                    function.mapToDeviceActionData(deviceId, deviceId, DeviceType.COMMON_DEVICE)?.let { deviceActionDetailBean ->
                        deviceActionDetailList.add(deviceActionDetailBean)
                    }
                }
                // e.g. choose last dp of device
                val chooseDeviceActionDetail = if (deviceActionDetailList.size > 0) {
                    deviceActionDetailList[deviceActionDetailList.size - 1]
                } else {
                    null
                }
                chooseDeviceActionDetail?.let {
                    // e.g. common device dp
                    if (it.functionType == DpCodeType.FUNCTION_TYPE_COMMON.type) {
                        mockChooseDp(it, cb)
                    } else {
                        val msg2 = "Demo not support FUNCTION_TYPE_LIGHT device dp!"
                        Log.e(TAG, msg2)
                        Toast.makeText(requireActivity(), msg2, Toast.LENGTH_LONG).show()
                    }
                } ?: {
                    val msg2 = "no device dp chosen!"
                    Log.e(TAG, msg2)
                    Toast.makeText(requireActivity(), msg2, Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private val sceneActionMap: MutableMap<Long, SceneAction> = mutableMapOf()
    private fun mockChooseDp(deviceActionDetailBean: DeviceActionDetailBean, cb: (SceneAction) -> Unit) {
        val actionDisplayMap: MutableMap<String, List<String>> = mutableMapOf()
        val subActionList: MutableList<String> = mutableListOf()
        subActionList.add(deviceActionDetailBean.functionName)
        // 必要参数
        var selDpValue: Any? = null
        var selDpDisplayValue: Any? = null
        var key: Long = -1
        deviceActionDetailBean.deviceActionDataList[0].apply {
            when (datapointType) {
                // value
                DatapointType.DATAPOINT_TYPE_VALUE -> {
                    when (dpValueType) {
                        // 百分比
                        DeviceDpValueType.DP_VALUE_TYPE_PERCENT, DeviceDpValueType.DP_VALUE_TYPE_PERCENT_1 -> {
                            // e.g. use min
                            selDpValue = dpValueTypeData.min
                            selDpDisplayValue = when (dpValueType) {
                                DeviceDpValueType.DP_VALUE_TYPE_PERCENT -> {
                                    PercentUtil.getPercent(selDpValue as Int, dpValueTypeData.min, dpValueTypeData.max)
                                }

                                DeviceDpValueType.DP_VALUE_TYPE_PERCENT_1 -> {
                                    PercentUtil.getPercentFromOne(selDpValue as Int, dpValueTypeData.min, dpValueTypeData.max)
                                }

                                else -> {
                                    ""
                                }
                            }
                            subActionList.add(selDpDisplayValue.toString())
                        }
                        // 倒计时
                        DeviceDpValueType.DP_VALUE_TYPE_COUNTDOWN, DeviceDpValueType.DP_VALUE_TYPE_COUNTDOWN_1 -> {
                            // e.g. seconds: 62
                            selDpValue = 62
                            selDpDisplayValue = TimeUtil.secondToShowText(requireContext(), selDpValue as Int)
                            subActionList.add(selDpDisplayValue.toString())
                        }

                        else -> {
                            selDpValue = dpValueTypeData.min
                            val tmpSelDisplayValue = StringBuilder()
                            tmpSelDisplayValue.append(
                                if (dpValueTypeData.scale > 0) {
                                    String.format(
                                        "%.${dpValueTypeData.scale}f",
                                        selDpValue as Int / 10.toDouble().pow(dpValueTypeData.scale)
                                    )
                                } else {
                                    selDpValue
                                }
                            )

                            selDpDisplayValue = tmpSelDisplayValue.toString()
                            subActionList.add(StringBuilder().append(tmpSelDisplayValue).append(dpValueTypeData.unit).toString())
                        }
                    }
                }
                // bool, enum
                else -> {
                    selDpValue = 0
                    selDpDisplayValue = dpEnumTypeData.value[selDpValue as Int]
                    subActionList.add(selDpDisplayValue.toString())
                }
            }
            actionDisplayMap[dpId.toString()] = subActionList
            key = dpId.toLong()
        }

        val builder = DeviceActionBuilder(deviceActionDetailBean.deviceId, deviceActionDetailBean, selDpValue, selDpDisplayValue ?: "")
        val actionBase: ActionBase = builder.build() as ActionBase
        SceneAction(actionBase).apply {
            actionDisplayNew = actionDisplayMap
            deviceActionDetailBean.deviceType?.let { deviceType ->
                when (deviceType) {
                    DeviceType.COMMON_DEVICE -> {
                        extService?.getDevice(deviceActionDetailBean.deviceId)?.let {
                            this.devIcon = it.iconUrl
                            this.isDevOnline = it.isOnline
                            this.entityName = it.name
                        }
                    }

                    DeviceType.GROUP_DEVICE -> {
                        this.actionExecutor = ACTION_TYPE_DEVICE_GROUP
                        extService?.getGroupDevice(deviceActionDetailBean.deviceId.toLong())?.let {
                            this.devIcon = it.iconUrl
                            this.isDevOnline = it.isOnline
                            this.entityName = it.name
                        }
                    }
                }
            }
        }.run {
            sceneActionMap[key] = this
            cb.invoke(this)
        }
    }
}