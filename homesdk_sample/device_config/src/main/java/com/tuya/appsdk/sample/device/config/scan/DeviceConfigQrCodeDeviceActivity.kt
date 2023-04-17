package com.tuya.appsdk.sample.device.config.scan

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
import com.google.android.material.appbar.MaterialToolbar
import com.tuya.appsdk.sample.device.config.R
import com.tuya.appsdk.sample.resource.HomeModel
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.home.sdk.builder.ThingQRCodeActivatorBuilder
import com.thingclips.smart.sdk.api.IThingDataCallback
import com.thingclips.smart.sdk.api.IThingSmartActivatorListener
import com.thingclips.smart.sdk.bean.DeviceBean
import com.uuzuche.lib_zxing.activity.CaptureActivity
import com.uuzuche.lib_zxing.activity.CodeUtils
import org.json.JSONObject
import java.util.*

/**
 * Qr Code
 *
 * @author yueguang [](mailto:developer@tuya.com)
 * @since 2021/3/11 3:13 PM
 */
class DeviceConfigQrCodeDeviceActivity : AppCompatActivity(), View.OnClickListener {
    private var topAppBar: MaterialToolbar? = null
    private var bt_search: Button? = null
    private var mUuid: String? = null
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

    private fun deviceQrCode(result: String?) {
        val postData = HashMap<String, Any?>()
        postData["code"] = result
        ThingHomeSdk.getRequestInstance().requestWithApiNameWithoutSession(
            "tuya.m.qrcode.parse",
            "4.0",
            postData,
            String::class.java,
            object : IThingDataCallback<String> {
                override fun onSuccess(result: String) {
                    initQrCode(result)
                }

                override fun onError(errorCode: String, errorMessage: String) {}
            }
        )
    }

    private fun initQrCode(result: String) {
        val homeId = HomeModel.INSTANCE.getCurrentHome(this)
        val obj = JSONObject(result)
        val actionObj = obj.optJSONObject("actionData")
        if (null != actionObj) {
            mUuid = actionObj.optString("uuid")
            val builder = ThingQRCodeActivatorBuilder()
                .setUuid(mUuid)
                .setHomeId(homeId)
                .setContext(this)
                .setTimeOut(100)
                .setListener(object : IThingSmartActivatorListener {
                    override fun onError(errorCode: String, errorMsg: String) {}
                    override fun onActiveSuccess(devResp: DeviceBean) {
                        Toast.makeText(
                            this@DeviceConfigQrCodeDeviceActivity,
                            "ActiveSuccess",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    override fun onStep(step: String, data: Any) {}
                })
            val iTuyaActivator = ThingHomeSdk.getActivatorInstance().newQRCodeDevActivator(builder)
            iTuyaActivator.start()
        }
    }
    companion object {
        private const val REQUEST_CODE_SCAN = 1
    }
}