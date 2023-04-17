package com.tuya.smart.android.demo.camera.utils

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLongClickListener
import android.view.View.OnTouchListener
import android.widget.TextView
import android.widget.Toast
import com.alibaba.fastjson.JSONObject
import com.thingclips.drawee.view.DecryptImageView
import com.thingclips.smart.android.camera.sdk.ThingIPCSdk
import com.thingclips.smart.android.camera.sdk.bean.CollectionPointBean
import com.thingclips.smart.android.camera.sdk.constant.PTZDPModel
import com.tuya.smart.android.demo.camera.R
import com.thingclips.smart.android.device.bean.EnumSchemaBean
import com.thingclips.smart.home.sdk.callback.IThingResultCallback
import com.thingclips.smart.sdk.api.IResultCallback
import java.util.*

/**
 * Created by HuangXin on 2021/9/25.
 */
@Suppress("DEPRECATION")
class CameraPTZHelper(devId: String) : View.OnClickListener, OnLongClickListener {
    private val thingIPCPTZ = ThingIPCSdk.getPTZInstance(devId)
    private var ptzBoard: View? = null
    private lateinit var context: Context
    private var progressDialog: android.app.ProgressDialog? = null
    private var collectionPointSize = 0

    @SuppressLint("ClickableViewAccessibility")
    fun bindPtzBoard(ptzBoard: View) {
        this.ptzBoard = ptzBoard
        context = ptzBoard.context
        progressDialog = android.app.ProgressDialog(ptzBoard.context)
        ptzBoard.findViewById<View>(R.id.tv_ptz_close).setOnClickListener(this)
        //PTZ Control
        ptzBoard.findViewById<View>(R.id.tv_ptz_left)
            .setOnTouchListener(PTZControlTouchListener("6"))
        ptzBoard.findViewById<View>(R.id.tv_ptz_top)
            .setOnTouchListener(PTZControlTouchListener("0"))
        ptzBoard.findViewById<View>(R.id.tv_ptz_right)
            .setOnTouchListener(PTZControlTouchListener("2"))
        ptzBoard.findViewById<View>(R.id.tv_ptz_bottom)
            .setOnTouchListener(PTZControlTouchListener("4"))
        val isPTZControl = thingIPCPTZ.querySupportByDPCode(PTZDPModel.DP_PTZ_CONTROL)
        ptzBoard.findViewById<View>(R.id.group_ptz_control).visibility =
            if (isPTZControl) View.VISIBLE else View.GONE
        //Focal
        ptzBoard.findViewById<View>(R.id.tv_focal_increase)
            .setOnTouchListener(FocalTouchListener("1"))
        ptzBoard.findViewById<View>(R.id.tv_focal_reduce)
            .setOnTouchListener(FocalTouchListener("0"))
        val isSupportZoom = thingIPCPTZ.querySupportByDPCode(PTZDPModel.DP_ZOOM_CONTROL)
        ptzBoard.findViewById<View>(R.id.group_focal).visibility =
            if (isSupportZoom) View.VISIBLE else View.GONE
        //Collection Point
        ptzBoard.findViewById<View>(R.id.tv_collection_add).setOnClickListener(this)
        ptzBoard.findViewById<View>(R.id.tv_collection_delete).setOnClickListener(this)
        ptzBoard.findViewById<View>(R.id.tv_collection_item).setOnClickListener(this)
        ptzBoard.findViewById<View>(R.id.tv_collection_item).setOnLongClickListener(this)
        val isSupportCollection = thingIPCPTZ.querySupportByDPCode(PTZDPModel.DP_MEMORY_POINT_SET)
        ptzBoard.findViewById<View>(R.id.group_collection).visibility =
            if (isSupportCollection) View.VISIBLE else View.GONE
        //Cruise
        val tvCruiseSwitch = ptzBoard.findViewById<TextView>(R.id.tv_cruise_switch)
        tvCruiseSwitch.setOnClickListener(this)
        val isCruiseOpen =
            thingIPCPTZ.getCurrentValue(PTZDPModel.DP_CRUISE_SWITCH, Boolean::class.java) == true
        tvCruiseSwitch.text = if (isCruiseOpen) "Opened" else "closed"
        ptzBoard.findViewById<View>(R.id.tv_cruise_mode).setOnClickListener(this)
        ptzBoard.findViewById<View>(R.id.tv_cruise_time).setOnClickListener(this)
        val isSupportCruise = thingIPCPTZ.querySupportByDPCode(PTZDPModel.DP_CRUISE_SWITCH)
        ptzBoard.findViewById<View>(R.id.group_cruise).visibility =
            if (isSupportCruise) View.VISIBLE else View.GONE
        //Tracking
        val tTrackingSwitch = ptzBoard.findViewById<TextView>(R.id.tv_tracking_switch)
        tTrackingSwitch.setOnClickListener(this)
        val isTrackingOpen =
            thingIPCPTZ.getCurrentValue(PTZDPModel.DP_MOTION_TRACKING, Boolean::class.java) == true
        tTrackingSwitch.text = if (isTrackingOpen) "Opened" else "closed"
        val isSupportTracking = thingIPCPTZ.querySupportByDPCode(PTZDPModel.DP_MOTION_TRACKING)
        ptzBoard.findViewById<View>(R.id.group_tracking).visibility =
            if (isSupportTracking) View.VISIBLE else View.GONE
        //Preset Point
        ptzBoard.findViewById<View>(R.id.tv_preset_select).setOnClickListener(this)
        val isSupportPreset = thingIPCPTZ.querySupportByDPCode(PTZDPModel.DP_PRESET_POINT)
        ptzBoard.findViewById<View>(R.id.group_preset).visibility =
            if (isSupportPreset) View.VISIBLE else View.GONE
        val tvPtzEmpty = ptzBoard.findViewById<View>(R.id.tv_ptz_empty)
        val isNotSupportPTZ =
            !isPTZControl && !isSupportZoom && !isSupportCollection && !isSupportCruise && !isSupportTracking && !isSupportPreset
        tvPtzEmpty.visibility =
            if (isNotSupportPTZ) View.VISIBLE else View.GONE
    }

    fun show() {
        ptzBoard?.let {
            it.alpha = 0f
            it.visibility = View.VISIBLE
            it.animate().alpha(1f).setDuration(200).start()
            requestCollectionPointList()
        }
    }

    private fun dismiss() {
        ptzBoard?.let {
            it.animate().alpha(0f).setDuration(200).start()
            it.postDelayed({ ptzBoard?.visibility = View.INVISIBLE }, 200)
        }
    }

    override fun onClick(v: View) {
        if (v.id == R.id.tv_ptz_close) {
            dismiss()
        } else if (v.id == R.id.tv_collection_add) {
            addCollectionPoint()
        } else if (v.id == R.id.tv_collection_delete) {
            deleteCollectionPoint()
        } else if (v.id == R.id.tv_collection_item) {
            if (v.tag is CollectionPointBean) {
                thingIPCPTZ.viewCollectionPoint(
                    (v.tag as CollectionPointBean),
                    ResultCallback("viewCollectionPoint")
                )
            }
        } else if (v.id == R.id.tv_cruise_switch) {
            val isOpen =
                thingIPCPTZ.getCurrentValue(PTZDPModel.DP_CRUISE_SWITCH, Boolean::class.java) == true
            thingIPCPTZ.publishDps(
                PTZDPModel.DP_CRUISE_SWITCH,
                !isOpen,
                object : ResultCallback("cruise_switch") {
                    override fun onSuccess() {
                        super.onSuccess()
                        val value = thingIPCPTZ.getCurrentValue(
                            PTZDPModel.DP_CRUISE_SWITCH,
                            Boolean::class.java
                        ) == true
                        (v as TextView).text = if (value) "Opened" else "closed"
                    }
                })
        } else if (v.id == R.id.tv_cruise_mode) {
            val itemMap = mapOf(
                "0" to context.getString(R.string.ipc_panoramic_cruise),
                "1" to context.getString(R.string.ipc_collection_point_cruise)
            )
            val items = mutableListOf<String>()
            thingIPCPTZ.getSchemaProperty(
                PTZDPModel.DP_CRUISE_MODE,
                EnumSchemaBean::class.java
            )?.range?.forEach {
                itemMap[it]?.let { item ->
                    items.add(item)
                }
            }
            showSelectDialog(items.toTypedArray()) { _: DialogInterface?, which: Int ->
                itemMap.entries.find { it.value == items[which] }?.key?.let { mode ->
                    thingIPCPTZ.setCruiseMode(mode, ResultCallback("setCruiseMode $mode"))
                }
            }
        } else if (v.id == R.id.tv_cruise_time) {
            val items = arrayOf(
                context.getString(R.string.ipc_full_day_cruise), context.getString(
                    R.string.ipc_custom_cruise
                )
            )
            showSelectDialog(items) { _: DialogInterface?, which: Int ->
                if (which == 0) {
                    thingIPCPTZ.publishDps(
                        PTZDPModel.DP_CRUISE_TIME_MODE,
                        "0",
                        ResultCallback("cruise_time_mode 0")
                    )
                } else if (which == 1) {
                    thingIPCPTZ.setCruiseTiming(
                        "09:00",
                        "16:00",
                        ResultCallback("cruise_time_mode 1")
                    )
                }
            }
        } else if (v.id == R.id.tv_tracking_switch) {
            onClickTracking(v as TextView)
        } else if (v.id == R.id.tv_preset_select) {
            onClickPreset()
        }
    }

    private fun onClickTracking(textView: TextView) {
        progressDialog?.show()
        val isOpen = true == thingIPCPTZ.getCurrentValue(
            PTZDPModel.DP_MOTION_TRACKING,
            Boolean::class.java
        )
        thingIPCPTZ.publishDps(
            PTZDPModel.DP_MOTION_TRACKING,
            !isOpen,
            object : ResultCallback("motion_tracking") {
                override fun onSuccess() {
                    super.onSuccess()
                    val value = true == thingIPCPTZ.getCurrentValue(
                        PTZDPModel.DP_MOTION_TRACKING,
                        Boolean::class.java
                    )
                    textView.text = if (value) "Opened" else "closed"
                    progressDialog?.dismiss()
                }

                override fun onError(code: String, error: String) {
                    super.onError(code, error)
                    progressDialog?.dismiss()
                }
            })
    }

    private fun onClickPreset() {
        val enumSchemaBean =
            thingIPCPTZ.getSchemaProperty(PTZDPModel.DP_PRESET_POINT, EnumSchemaBean::class.java)
        val items = enumSchemaBean.getRange().toTypedArray()
        showSelectDialog(items) { _: DialogInterface?, which: Int ->
            thingIPCPTZ.publishDps(
                PTZDPModel.DP_PRESET_POINT,
                items[which],
                ResultCallback("ipc_preset_set " + items[which])
            )
        }
    }

    fun ptzControl(direction: String?) {
        thingIPCPTZ.publishDps(PTZDPModel.DP_PTZ_CONTROL, direction!!, ResultCallback("ptzControl"))
    }

    fun ptzStop() {
        thingIPCPTZ.publishDps(PTZDPModel.DP_PTZ_STOP, true, ResultCallback("ptzStop"))
    }

    private fun requestCollectionPointList() {
        thingIPCPTZ.requestCollectionPointList(object :
            IThingResultCallback<List<CollectionPointBean>?> {
            override fun onSuccess(result: List<CollectionPointBean>?) {
                val decryptImageView: DecryptImageView =
                    ptzBoard?.findViewById(R.id.iv_collection) ?: return
                val tvName = ptzBoard?.findViewById<TextView>(R.id.tv_collection_item) ?: return
                if (result != null && result.isNotEmpty()) {
                    collectionPointSize = result.size
                    var collectionPointBean = result[result.size - 1]
                    try {
                        for (i in result.indices) {
                            val item = result[i]
                            if (item.mpId.toInt() > collectionPointBean.mpId.toInt()) {
                                collectionPointBean = item
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    tvName.text = collectionPointBean.name
                    if (collectionPointBean.encryption is JSONObject) {
                        val jsonObject = collectionPointBean.encryption as JSONObject
                        val key = jsonObject["key"]
                        if (key == null) {
                            decryptImageView.setImageURI(collectionPointBean.pic)
                        } else {
                            decryptImageView.setImageURI(
                                collectionPointBean.pic,
                                key.toString().toByteArray()
                            )
                        }
                    } else {
                        decryptImageView.setImageURI(collectionPointBean.pic)
                    }
                    tvName.tag = collectionPointBean
                } else {
                    collectionPointSize = 0
                    tvName.text = ""
                    tvName.tag = null
                    decryptImageView.setImageResource(0)
                }
            }

            override fun onError(errorCode: String, errorMessage: String) {}
        })
    }

    private fun addCollectionPoint() {
        progressDialog?.show()
        thingIPCPTZ.addCollectionPoint(
            "Collection" + collectionPointSize++,
            object : IResultCallback {
                override fun onError(code: String, error: String) {
                    Log.d(TAG, "addCollectionPoint invoke error")
                    progressDialog?.dismiss()
                }

                override fun onSuccess() {
                    ptzBoard?.postDelayed({
                        requestCollectionPointList()
                        progressDialog?.dismiss()
                    }, 1000)
                }
            })
    }

    private fun deleteCollectionPoint() {
        val tvCollectionItem = ptzBoard?.findViewById<TextView>(R.id.tv_collection_item) ?: return
        if (tvCollectionItem.tag !is CollectionPointBean) {
            Toast.makeText(context, "Operation failed", Toast.LENGTH_SHORT).show()
            return
        }
        progressDialog?.show()
        val items: MutableList<CollectionPointBean> = ArrayList()
        val item = tvCollectionItem.tag as CollectionPointBean
        items.add(item)
        thingIPCPTZ.deleteCollectionPoints(items, object : IResultCallback {
            override fun onError(code: String, error: String) {
                Log.d(TAG, "deleteCollectionPoint invoke error")
                progressDialog?.dismiss()
            }

            override fun onSuccess() {
                requestCollectionPointList()
                progressDialog?.dismiss()
            }
        })
    }

    override fun onLongClick(v: View): Boolean {
        if (v.id == R.id.tv_collection_item) {
            val tvCollectionItem = ptzBoard?.findViewById<TextView>(R.id.tv_collection_item)
            if (tvCollectionItem?.tag is CollectionPointBean) {
                val item = tvCollectionItem.tag as CollectionPointBean
                val nameNew = item.name + " New"
                thingIPCPTZ.modifyCollectionPoint(item, nameNew, object : IResultCallback {
                    override fun onError(code: String, error: String) {
                        Toast.makeText(context, "Operation failed", Toast.LENGTH_SHORT).show()
                    }

                    @SuppressLint("SetTextI18n")
                    override fun onSuccess() {
                        (v as TextView).text = nameNew
                        Toast.makeText(context, "Operation success", Toast.LENGTH_SHORT).show()
                    }
                })
            }
        }
        return true
    }

    private fun showSelectDialog(
        items: Array<String>,
        onClickListener: DialogInterface.OnClickListener
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setItems(items, onClickListener)
        builder.setNegativeButton("Close") { dialog: DialogInterface, _: Int -> dialog.dismiss() }
        builder.create().show()
    }

    internal open inner class ResultCallback(private val method: String) : IResultCallback {
        override fun onError(code: String, error: String) {
            Log.d(TAG, "$method invoke error: $error")
            Toast.makeText(context, "Operation failed", Toast.LENGTH_SHORT).show()
        }

        override fun onSuccess() {
            Log.d(TAG, "$method invoke success")
            Toast.makeText(context, "Operation success", Toast.LENGTH_SHORT).show()
        }
    }

    private inner class PTZControlTouchListener(var direction: String) : OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> ptzControl(direction)
                MotionEvent.ACTION_UP -> ptzStop()
                else -> {
                }
            }
            return true
        }
    }

    private inner class FocalTouchListener(var zoom: String) : OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> thingIPCPTZ.publishDps(
                    PTZDPModel.DP_ZOOM_CONTROL, zoom, ResultCallback(
                        "zoom_control$zoom"
                    )
                )
                MotionEvent.ACTION_UP -> thingIPCPTZ.publishDps(
                    PTZDPModel.DP_ZOOM_STOP,
                    true,
                    ResultCallback("zoom_stop")
                )
                else -> {
                }
            }
            return true
        }
    }

    companion object {
        private const val TAG = "CameraPTZHelper"
    }
}