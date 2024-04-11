package com.tuya.lock.demo.ble.activity

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.sdk.api.IResultCallback
import com.thingclips.smart.sdk.api.IThingDevice
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.DialogUtils.showDelete

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class DeleteDeviceActivity: AppCompatActivity() {

    companion object {
        fun startActivity(context: Context?, devId: String?) {
            val intent = Intent(context, DeleteDeviceActivity::class.java)
            //设备id
            intent.putExtra(Constant.DEVICE_ID, devId)
            context?.startActivity(intent)
        }
    }

    private var mDevice: IThingDevice? = null

    fun startActivity(context: Context, devId: String?) {
        val intent = Intent(context, DeleteDeviceActivity::class.java)
        //设备id
        intent.putExtra(Constant.DEVICE_ID, devId)
        context.startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_deletel)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v: View? -> finish() }
        val deviceId = intent.getStringExtra(Constant.DEVICE_ID)
        mDevice = ThingHomeSdk.newDeviceInstance(deviceId)
        findViewById<View>(R.id.delete_view).setOnClickListener { v: View ->
            showDelete(
                v.context,
                resources.getString(R.string.whether_to_remove_device)
            ) { dialog: DialogInterface?, which: Int ->
                mDevice?.removeDevice(object : IResultCallback {
                    override fun onError(code: String, error: String) {
                        Log.e(
                            Constant.TAG,
                            "removeDevice onError code:$code, error:$error"
                        )
                        Toast.makeText(
                            v.context,
                            error,
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onSuccess() {
                        Toast.makeText(
                            v.context,
                            "onSuccess",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                })
            }
        }
        findViewById<View>(R.id.clear_view).setOnClickListener { v: View ->
            showDelete(
                v.context,
                resources.getString(R.string.whether_to_remove_device_clear)
            ) { dialog: DialogInterface?, which: Int ->
                mDevice?.resetFactory(object : IResultCallback {
                    override fun onError(code: String, error: String) {
                        Log.e(
                            Constant.TAG,
                            "removeDevice onError code:$code, error:$error"
                        )
                        Toast.makeText(
                            v.context,
                            error,
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onSuccess() {
                        Toast.makeText(
                            v.context,
                            "onSuccess",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                })
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mDevice?.onDestroy()
    }
}