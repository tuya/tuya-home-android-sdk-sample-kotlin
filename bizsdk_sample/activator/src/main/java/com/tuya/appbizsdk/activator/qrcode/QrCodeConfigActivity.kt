package com.tuya.appbizsdk.activator.qrcode

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.thingclips.smart.activator.core.kit.ThingActivatorDeviceCoreKit
import com.thingclips.smart.home.sdk.builder.ThingCameraActivatorBuilder
import com.thingclips.smart.sdk.api.IThingActivatorGetToken
import com.thingclips.smart.sdk.api.IThingCameraDevActivator
import com.thingclips.smart.sdk.api.IThingSmartCameraActivatorListener
import com.thingclips.smart.sdk.bean.DeviceBean
import com.tuya.appbizsdk.activator.R
import com.tuya.appbizsdk.activator.util.qrcode.DensityUtil
import com.tuya.appbizsdk.activator.util.qrcode.QRCodeUtil
import com.tuya.appsdk.sample.resource.HomeModel

/**
 * TODO feature
 *二维码配网 QrCode Config
 * @author hou qing <a href="mailto:developer@tuya.com"/>
 * @since 2021/7/28 2:52 下午
 */
class QrCodeConfigActivity : AppCompatActivity(),View.OnClickListener{
    private var wifiSSId = ""
    private var wifiPwd = ""
    private lateinit var mIvQr: ImageView
    private lateinit var mLlInputWifi: LinearLayout
    private lateinit var mEtInputWifiSSid: EditText
    private lateinit var mEtInputWifiPwd: EditText
    private lateinit var mBtnSave: Button
    private var mTuyaActivator: IThingCameraDevActivator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_config_qr_code)
        val toolbar = findViewById<Toolbar>(R.id.toolbar_view)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }
        mLlInputWifi = findViewById(R.id.ll_input_wifi)
        mEtInputWifiSSid = findViewById(R.id.et_wifi_ssid)
        mEtInputWifiPwd = findViewById(R.id.et_wifi_pwd)
        mBtnSave = findViewById(R.id.btn_save)
        mBtnSave.setOnClickListener(this)
        mIvQr = findViewById(R.id.iv_qrcode)
    }
    private fun hideKeyboard(v: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(v.windowToken, 0)
    }


    override fun onClick(v: View) {
        if (v.id == R.id.btn_save) {
            wifiSSId = mEtInputWifiSSid.text.toString()
            wifiPwd = mEtInputWifiPwd.text.toString()
            val homeId: Long = HomeModel.INSTANCE.getCurrentHome(this)

            // Get Network Configuration Token
            ThingActivatorDeviceCoreKit.getActivatorInstance().getActivatorToken(homeId,
                object : IThingActivatorGetToken {
                    override fun onSuccess(token: String) {
                        //Create and show qrCode
                        val builder = ThingCameraActivatorBuilder()
                            .setToken(token)
                            .setPassword(wifiPwd)
                            .setTimeOut(100)
                            .setContext(this@QrCodeConfigActivity)
                            .setSsid(wifiSSId)
                            .setListener(object : IThingSmartCameraActivatorListener {
                                override fun onQRCodeSuccess(qrcodeUrl: String) {
                                    val bitmap: Bitmap
                                    try {
                                        val widthAndHeight: Int = DensityUtil.getScreenDispalyWidth(this@QrCodeConfigActivity) - DensityUtil.dip2px(this@QrCodeConfigActivity, 50f)
                                        bitmap = QRCodeUtil.createQRCode(qrcodeUrl, widthAndHeight)
                                        runOnUiThread {
                                            mIvQr.setImageBitmap(bitmap)
                                            mLlInputWifi.visibility = View.GONE
                                            mIvQr.visibility = View.VISIBLE
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }

                                override fun onError(errorCode: String, errorMsg: String) {
                                }
                                override fun onActiveSuccess(devResp: DeviceBean) {
                                    Toast.makeText(this@QrCodeConfigActivity, "config success!", Toast.LENGTH_LONG).show()
                                }
                            })
                        mTuyaActivator = ThingActivatorDeviceCoreKit.getActivatorInstance().newCameraDevActivator(builder)
                        mTuyaActivator?.createQRCode()
                        mTuyaActivator?.start()
                    }

                    override fun onFailure(errorCode: String, errorMsg: String) {}
                })
            hideKeyboard(v)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mTuyaActivator?.stop()
        mTuyaActivator?.onDestroy()
    }
}