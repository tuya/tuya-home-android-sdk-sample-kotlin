package com.tuya.smart.android.demo.camera

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.thingclips.smart.android.camera.sdk.ThingIPCSdk
import com.tuya.smart.android.demo.camera.adapter.CameraInfoAdapter
import com.tuya.smart.android.demo.camera.databinding.ActivityCameraInfoBinding
import com.tuya.smart.android.demo.camera.utils.Constants

/**
 * Camera Device Info
 * @author houqing <a href="mailto:developer@tuya.com"/>
 * @since 2021/7/27 5:32 PM

 */
class CameraInfoActivity : AppCompatActivity() {
    private var mDevId: String? = null
    private lateinit var mData: MutableList<String>
    private lateinit var viewBinding: ActivityCameraInfoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraInfoBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        mDevId = intent.getStringExtra(Constants.INTENT_DEV_ID)
        initData()
        initView()
    }

    private fun initData() {
        mData = arrayListOf()
        ThingIPCSdk.getCameraInstance()?.let {
            mData.add(getString(R.string.low_power) + it.isLowPowerDevice(mDevId))
            it.getCameraConfig(mDevId)?.run {
                mData.add(getString(R.string.video_num) + this.videoNum)
                mData.add(getString(R.string.default_definition) + parseClarity(this.defaultDefinition))
                mData.add(getString(R.string.is_support_speaker) + this.isSupportSpeaker)
                mData.add(getString(R.string.is_support_picK_up) + this.isSupportPickup)
                mData.add(getString(R.string.is_support_talk) + this.isSupportChangeTalkBackMode)
                mData.add(getString(R.string.default_talk_mode) + this.defaultTalkBackMode)
                mData.add(getString(R.string.support_speed) + list2String(this.supportPlaySpeedList))
                mData.add(getString(R.string.raw_data) + this.rawDataJsonStr)
            }
        }
    }

    private fun parseClarity(clarityMode: Int): String {
        var info = getString(R.string.other)
        if (clarityMode == 4) {
            info = getString(R.string.hd)
        } else if (clarityMode == 2) {
            info = getString(R.string.sd)
        }
        return info
    }

    private fun list2String(list: List<Int>?): String {
        return if (list != null && list.isNotEmpty()) {
            val stringBuilder = StringBuilder()
            for (i in list.indices) {
                stringBuilder.append(list[i].toString())
                if (i < list.size - 1) {
                    stringBuilder.append(", ")
                }
            }
            stringBuilder.toString()
        } else {
            ""
        }
    }

    private fun initView() {
        viewBinding.cameraInfoRy.layoutManager = LinearLayoutManager(this)
        val cameraInfoAdapter = CameraInfoAdapter(mData)
        viewBinding.cameraInfoRy.adapter = cameraInfoAdapter
        cameraInfoAdapter.notifyDataSetChanged()
    }
}