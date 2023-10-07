package com.tuya.appbizsdk.activator.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.alibaba.fastjson.JSONException
import com.alibaba.fastjson.JSONObject
import com.google.android.material.appbar.MaterialToolbar
import com.thingclips.smart.activator.core.kit.ThingActivatorCoreKit
import com.thingclips.smart.activator.core.kit.active.inter.IThingActiveManager
import com.thingclips.smart.activator.core.kit.bean.ScanActionBean
import com.thingclips.smart.activator.core.kit.bean.ThingDeviceActiveErrorBean
import com.thingclips.smart.activator.core.kit.bean.ThingDeviceActiveLimitBean
import com.thingclips.smart.activator.core.kit.builder.ThingDeviceActiveBuilder
import com.thingclips.smart.activator.core.kit.constant.ThingDeviceActiveModeEnum
import com.thingclips.smart.activator.core.kit.listener.IThingDeviceActiveListener
import com.thingclips.smart.android.network.Business.ResultListener
import com.thingclips.smart.android.network.http.BusinessResponse
import com.thingclips.smart.sdk.bean.DeviceBean
import com.tuya.appbizsdk.activator.R
import com.tuya.appsdk.sample.resource.HomeModel
import com.uuzuche.lib_zxing.activity.CaptureActivity
import com.uuzuche.lib_zxing.activity.CodeUtils

/**
 * Qr Code
 *
 * @author yueguang [](mailto:developer@tuya.com)
 * @since 2021/3/11 3:13 PM
 */
class DeviceConfigQrCodeDeviceActivity : AppCompatActivity(), View.OnClickListener {
    private var topAppBar: MaterialToolbar? = null
    private var bt_search: Button? = null

    private var activeManager: IThingActiveManager? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_config_info_hint_activity)
        initView()
    }

    private fun initView() {
        topAppBar = findViewById<View>(R.id.topAppBar) as MaterialToolbar
        topAppBar!!.setNavigationOnClickListener { finish() }
        topAppBar!!.title = getString(R.string.device_qr_code_service_title)
        bt_search = findViewById<View>(R.id.bt_search) as Button
        bt_search!!.setOnClickListener(this)
        bt_search!!.setText(R.string.device_qr_code_service_title)
    }

    override fun onClick(v: View) {
        if (v.id == R.id.bt_search) {
            startQrCode()
        }
    }

    private fun startQrCode() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CODE_SCAN
            )
            return
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE_SCAN
            )
            return
        }
        val intent = Intent(this, CaptureActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE_SCAN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCAN) {
            if (null != data) {
                val bundle = data.extras ?: return
                if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
                    val result = bundle.getString(CodeUtils.RESULT_STRING)
                    Toast.makeText(this, "result:$result", Toast.LENGTH_LONG).show()
                    deviceQrCode(result)
                } else if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED) {
                    Toast.makeText(this, "解析二维码失败", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activeManager?.stopActive()
    }

    private fun deviceQrCode(result: String?) {
        ThingActivatorCoreKit.getCommonBizOpt().parseQrCode(
            result!!, object : ResultListener<ScanActionBean> {
                override fun onFailure(
                    bizResponse: BusinessResponse?,
                    bizResult: ScanActionBean?,
                    apiName: String?
                ) {
                }

                override fun onSuccess(
                    bizResponse: BusinessResponse?,
                    bizResult: ScanActionBean?,
                    apiName: String?
                ) {
                    initQrCode(bizResult)
                }

            })
    }

    private fun initQrCode(result: ScanActionBean?) {
        if (result?.actionData == null) return

        try {
            val obj = JSONObject.parseObject(JSONObject.toJSONString(result.actionData))
            val mUuid = obj.getString("uuid")
            val homeId = HomeModel.INSTANCE.getCurrentHome(this)
            activeManager = ThingActivatorCoreKit.getActiveManager().newThingActiveManager()
            activeManager!!.startActive(ThingDeviceActiveBuilder().apply {
                activeModel = ThingDeviceActiveModeEnum.QR
                this.uuid = mUuid
                context = this@DeviceConfigQrCodeDeviceActivity
                timeOut = 100
                relationId = homeId
                listener = object : IThingDeviceActiveListener {
                    override fun onActiveError(errorBean: ThingDeviceActiveErrorBean) {
                    }

                    override fun onActiveLimited(limitBean: ThingDeviceActiveLimitBean) {
                    }

                    override fun onActiveSuccess(deviceBean: DeviceBean) {
                        Toast.makeText(
                            this@DeviceConfigQrCodeDeviceActivity,
                            "ActiveSuccess",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    override fun onBind(devId: String) {
                    }

                    override fun onFind(devId: String) {
                    }

                }
            })
        } catch (e: JSONException) {

        }
    }

    companion object {
        private const val REQUEST_CODE_SCAN = 1
    }
}