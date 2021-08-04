package com.tuya.smart.android.demo.camera

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.fastjson.JSONObject
import com.tuya.smart.android.demo.camera.databinding.ActivityCameraSettingBinding
import com.tuya.smart.android.demo.camera.utils.Constants
import com.tuya.smart.android.demo.camera.utils.DPConstants
import com.tuya.smart.home.sdk.TuyaHomeSdk
import com.tuya.smart.sdk.api.IDevListener
import com.tuya.smart.sdk.api.IResultCallback
import com.tuya.smart.sdk.api.ITuyaDevice

/**

 * TODO feature
 *存储卡管理，水印管理
 * SdCard Setting and WaterMark Setting
 * @author houqing <a href="mailto:developer@tuya.com"/>

 * @since 2021/7/27 3:40 下午

 */
class CameraSettingActivity :AppCompatActivity(){
    companion object{
        private val TAG = CameraSettingActivity::class.java.simpleName
    }
    private var devId: String? = null
    private var iTuyaDevice: ITuyaDevice? = null
    private lateinit var viewBinding:ActivityCameraSettingBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraSettingBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        setSupportActionBar(viewBinding.toolbarView)
        viewBinding.toolbarView.setNavigationOnClickListener { onBackPressed() }
        devId = intent.getStringExtra(Constants.INTENT_DEV_ID)
        sdStatus()//存储卡状态
        sdCardFormat()//存储卡格式化
        watermark()//水印开关
        sdCardSave()//存储卡录像开关
        sdCardSaveModel()//存储卡录像模式
    }
    private fun sdCardSave(){
        viewBinding.tvSdSaveVideo.text = getString(R.string.not_support)
        queryValueByDPID("150")?.let {
            viewBinding.tvSdSaveVideo.text = it.toString()
            viewBinding.openSdSaveVideo.setOnClickListener{
                publishDps("150", !java.lang.Boolean.parseBoolean(viewBinding.tvSdSaveVideo.text.toString()))
            }
            listenDPUpdate("150", object : DPCallback {
                override fun callback(obj: Any) {
                    viewBinding.tvSdSaveVideo.text = obj.toString()
                }
            })
        }
    }
    private fun sdCardSaveModel(){
        viewBinding.tvSdSaveVideoModel.text=getString(R.string.not_support)
        queryValueByDPID("151")?.let {
            viewBinding.tvSdSaveVideoModel.text = it.toString()
            listenDPUpdate("151", object : DPCallback {
                override fun callback(obj: Any) {
                    viewBinding.tvSdSaveVideo.text = obj.toString()
                }
            })
        }
    }


    private fun sdStatus() {
        viewBinding.tvSdStatus.text = getString(R.string.not_support)
        queryValueByDPID(DPConstants.SD_STATUS)?.let {
            viewBinding.tvSdStatus.text = it.toString()
            listenDPUpdate(DPConstants.SD_STATUS, object : DPCallback {
                override fun callback(obj: Any) {
                    viewBinding.tvSdStatus.text = it.toString()
                }
            })
        }
    }

    private fun sdCardFormat() {
        viewBinding.tvSdFormat.text = getString(R.string.not_support)
        queryValueByDPID(DPConstants.SD_STORAGE)?.let {
            viewBinding.tvSdFormat.text = it.toString()
            queryValueByDPID(DPConstants.SD_FORMAT)?.run {
                viewBinding.btnSdFormat.visibility = View.VISIBLE
                viewBinding.btnSdFormat.setOnClickListener {
                    publishDps(DPConstants.SD_FORMAT, true)
                    listenDPUpdate(DPConstants.SD_FORMAT_STATUS, object : DPCallback {
                        override fun callback(obj: Any) {
                            viewBinding.tvSdFormat.text = getString(R.string.format_status) + obj
                            if ("100" == obj.toString()) {
                                viewBinding.tvSdFormat.text = queryValueByDPID(DPConstants.SD_STORAGE)?.toString()
                            }
                        }
                    })
                }
            }
        }
    }

    private fun watermark() {
        viewBinding.tvWatermark.text = getString(R.string.not_support)
        queryValueByDPID(DPConstants.WATERMARK)?.let {
            viewBinding.tvWatermark.text = it.toString()
            viewBinding.btnWatermark.visibility = View.VISIBLE
            viewBinding.btnWatermark.setOnClickListener {
                publishDps(DPConstants.WATERMARK, !java.lang.Boolean.parseBoolean(viewBinding.tvWatermark.text.toString()))
            }
            listenDPUpdate(DPConstants.WATERMARK, object : DPCallback {
                override fun callback(obj: Any) {
                    viewBinding.tvWatermark.text = obj.toString()
                }
            })
        }
    }

    private fun queryValueByDPID(dpId: String): Any? {
        TuyaHomeSdk.getDataInstance().getDeviceBean(devId)?.also {
             return it.getDps()?.get(dpId)
        }
        return null
    }

    /**
     * 下发dp点
     * @param dpId String
     * @param value Any
     */
    private fun publishDps(dpId: String, value: Any) {
        if (iTuyaDevice == null) {
            iTuyaDevice = TuyaHomeSdk.newDeviceInstance(devId)
        }
        val jsonObject = JSONObject()
        jsonObject[dpId] = value
        val dps = jsonObject.toString()
        iTuyaDevice!!.publishDps(dps, object : IResultCallback {
            override fun onError(code: String, error: String) {
                Log.e(TAG, "publishDps err $dps")
            }

            override fun onSuccess() {
                Log.i(TAG, "publishDps suc $dps")
            }
        })
    }

    private fun listenDPUpdate(dpId: String, callback: DPCallback?) {
        TuyaHomeSdk.newDeviceInstance(devId).registerDevListener(object : IDevListener {
            override fun onDpUpdate(devId: String, dpStr: String) {
                callback?.let {
                    val dps: Map<String, Any> = JSONObject.parseObject<Map<String, Any>>(dpStr, MutableMap::class.java)
                    if (dps.containsKey(dpId)) {
                        dps[dpId]?.let { it1 -> callback.callback(it1) }
                    }
                }
            }

            override fun onRemoved(devId: String) {}
            override fun onStatusChanged(devId: String, online: Boolean) {}
            override fun onNetworkStatusChanged(devId: String, status: Boolean) {}
            override fun onDevInfoUpdate(devId: String) {}
        })
    }
    private interface DPCallback {
        fun callback(obj: Any)
    }
}