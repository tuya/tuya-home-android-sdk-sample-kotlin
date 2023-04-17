package com.tuya.smart.android.demo.camera

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.alibaba.fastjson.JSONObject
import com.tuya.smart.android.demo.camera.CameraSettingActivity.DPCallback
import com.tuya.smart.android.demo.camera.databinding.ActivityCameraSettingBinding
import com.tuya.smart.android.demo.camera.utils.Constants
import com.tuya.smart.android.demo.camera.utils.DPConstants
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.sdk.api.IDevListener
import com.thingclips.smart.sdk.api.IResultCallback
import com.thingclips.smart.sdk.api.IThingDevice

/**
 * SdCard Setting and WaterMark Setting
 * @author houqing <a href="mailto:developer@tuya.com"/>
 * @since 2021/7/27 3:40 PM
 */
class CameraSettingActivity : AppCompatActivity() {
    companion object {
        private val TAG = CameraSettingActivity::class.java.simpleName
    }

    private var devId: String? = null
    private var iTuyaDevice: IThingDevice? = null
    private lateinit var viewBinding: ActivityCameraSettingBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraSettingBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        setSupportActionBar(viewBinding.toolbarView)
        viewBinding.toolbarView.setNavigationOnClickListener { onBackPressed() }
        devId = intent.getStringExtra(Constants.INTENT_DEV_ID)
        sdStatus()//SD card status
        sdCardFormat()//SD card format
        watermark()//Watermark switch
        sdCardSave()//SD card recording switch
        sdCardSaveModel()//SD card recording mode
        record()
    }

    private fun sdCardSave() {
        viewBinding.tvSdSaveVideo.text = getString(R.string.not_support)
        queryValueByDPID("150")?.let {
            viewBinding.tvSdSaveVideo.text = it.toString()
            viewBinding.openSdSaveVideo.setOnClickListener {
                publishDps(
                    "150",
                    !java.lang.Boolean.parseBoolean(viewBinding.tvSdSaveVideo.text.toString())
                )
            }
            listenDPUpdate("150", object : DPCallback {
                override fun callback(obj: Any) {
                    viewBinding.tvSdSaveVideo.text = obj.toString()
                }
            })
        }
    }

    private fun sdCardSaveModel() {
        viewBinding.tvSdSaveVideoModel.text = getString(R.string.not_support)
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
                                viewBinding.tvSdFormat.text =
                                    queryValueByDPID(DPConstants.SD_STORAGE)?.toString()
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
                publishDps(
                    DPConstants.WATERMARK,
                    !java.lang.Boolean.parseBoolean(viewBinding.tvWatermark.text.toString())
                )
            }
            listenDPUpdate(DPConstants.WATERMARK, object : DPCallback {
                override fun callback(obj: Any) {
                    viewBinding.tvWatermark.text = obj.toString()
                }
            })
        }
    }

    private fun record() {
        val dpId: String = DPConstants.SD_CARD_RECORD_SWITCH
        val tv = findViewById<TextView>(R.id.tv_record)
        val value = queryValueByDPID(dpId)
        if (value != null) {
            tv.text = value.toString()
            val btn = findViewById<Button>(R.id.btn_record)
            btn.visibility = View.VISIBLE
            btn.setOnClickListener {
                publishDps(
                    DPConstants.SD_CARD_RECORD_SWITCH,
                    !java.lang.Boolean.parseBoolean(tv.text.toString())
                )
            }
            listenDPUpdate(dpId, object : DPCallback {
                override fun callback(obj: Any) {
                    tv.text = obj.toString()
                }
            })
        } else {
            tv.text = getString(R.string.not_support)
        }
    }

    private fun queryValueByDPID(dpId: String): Any? {
        ThingHomeSdk.getDataInstance().getDeviceBean(devId)?.also {
            return it.getDps()?.get(dpId)
        }
        return null
    }

    private fun publishDps(dpId: String, value: Any) {
        if (iTuyaDevice == null) {
            iTuyaDevice = ThingHomeSdk.newDeviceInstance(devId)
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
        ThingHomeSdk.newDeviceInstance(devId).registerDevListener(object : IDevListener {
            override fun onDpUpdate(devId: String, dpStr: String) {
                callback?.let {
                    val dps: Map<String, Any> =
                        JSONObject.parseObject<Map<String, Any>>(dpStr, MutableMap::class.java)
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