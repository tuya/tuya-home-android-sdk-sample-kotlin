package com.tuya.smart.android.demo.camera

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.thingclips.smart.android.camera.sdk.ThingIPCSdk
import com.thingclips.smart.android.camera.sdk.bean.ThingDoorBellCallModel
import com.thingclips.smart.android.camera.sdk.callback.ThingSmartDoorBellObserver
import com.tuya.smart.android.demo.camera.databinding.ActivityCameraDoorbellCallingBinding
import com.tuya.smart.android.demo.camera.utils.Constants
import com.thingclips.smart.home.sdk.ThingHomeSdk

/**
 * DoorBell Call
 * @author hou qing <a href="mailto:developer@tuya.com"/>
 * @since 2021/7/28 11:22 AM
 */
class CameraDoorBellActivity : AppCompatActivity() {
    private var mMessageId: String? = null
    private val mDoorBellInstance = ThingIPCSdk.getDoorbell().ipcDoorBellManagerInstance
    private lateinit var viewBinding: ActivityCameraDoorbellCallingBinding
    private val mObserver: ThingSmartDoorBellObserver = object : ThingSmartDoorBellObserver() {
        override fun doorBellCallDidCanceled(callModel: ThingDoorBellCallModel, isTimeOut: Boolean) {
            if (isTimeOut) {
                Toast.makeText(
                    this@CameraDoorBellActivity,
                    "Automatically hang up when the doorbell expires",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this@CameraDoorBellActivity,
                    "The doorbell was cancelled by the device",
                    Toast.LENGTH_LONG
                ).show()
            }
            finish()
        }

        override fun doorBellCallDidHangUp(callModel: ThingDoorBellCallModel) {
            Toast.makeText(this@CameraDoorBellActivity, "Hung up", Toast.LENGTH_LONG).show()
            finish()
        }

        override fun doorBellCallDidAnsweredByOther(callModel: ThingDoorBellCallModel) {
            Toast.makeText(
                this@CameraDoorBellActivity,
                "The doorbell is answered by another user",
                Toast.LENGTH_LONG
            ).show()
            mDoorBellInstance.refuseDoorBellCall(callModel.messageId)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraDoorbellCallingBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        mMessageId = intent.getStringExtra(Constants.INTENT_MSGID)
        initData()
        initView()
    }

    private fun initData() {
        if (TextUtils.isEmpty(mMessageId)) {
            finish()
            return
        }
        mDoorBellInstance.addObserver(mObserver)
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        val model = mDoorBellInstance.getCallModelByMessageId(mMessageId)
        val deviceBean = ThingHomeSdk.getDataInstance().getDeviceBean(model.devId)
        viewBinding.tvState.text = """${deviceBean!!.getName()} call, waiting to be answered.."""
        viewBinding.btnRefuse.setOnClickListener {
            if (isAnsweredBySelf()) {
                mDoorBellInstance.hangupDoorBellCall(mMessageId)
            } else {
                mDoorBellInstance.refuseDoorBellCall(mMessageId)
            }
            finish()
        }
        viewBinding.btnAccept.setOnClickListener {
            mDoorBellInstance.answerDoorBellCall(mMessageId)
            viewBinding.tvState.text = "The doorbell has been answered."
            it.visibility = View.GONE
            viewBinding.btnRefuse.setText(R.string.ipc_doorbell_hangup)
        }
    }

    private fun isAnsweredBySelf(): Boolean {
        val callModel = mDoorBellInstance.getCallModelByMessageId(mMessageId) ?: return false
        return callModel.isAnsweredBySelf
    }

    override fun onDestroy() {
        super.onDestroy()
        mDoorBellInstance.removeObserver(mObserver)
    }
}