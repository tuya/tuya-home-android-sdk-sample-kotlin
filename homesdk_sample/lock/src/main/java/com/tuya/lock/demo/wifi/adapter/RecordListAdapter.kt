package com.tuya.lock.demo.wifi.adapter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.android.common.utils.L
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.optimus.lock.api.bean.Record
import com.thingclips.smart.sdk.bean.DeviceBean
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.activity.ShowCodeActivity
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.Utils

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class RecordListAdapter : RecyclerView.Adapter<RecordListAdapter.ViewHolder>() {
    private var data: MutableList<Record.DataBean> = ArrayList()
    private var deviceBean: DeviceBean? = null

    fun setDevice(devId: String?) {
        deviceBean = ThingHomeSdk.getDataInstance().getDeviceBean(devId)
    }

    fun setData(list: MutableList<Record.DataBean>) {
        data = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_zigbee_lock_records, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemData = data[position]
        if (null == deviceBean || null == deviceBean!!.getSchemaMap()) {
            L.e(Constant.TAG, "deviceBean OR getSchemaMap is null")
            return
        }
        for ((key, schemaItem) in deviceBean!!.getSchemaMap()) {
            if (TextUtils.equals(key, itemData.dpId.toString())) {
                var recordTitle =
                    itemData.userName + schemaItem.name + "(" + itemData.dpValue + ")"
                if (itemData.tags == 1) {
                    recordTitle = "[hiJack]$recordTitle"
                }
                holder.userNameView.text = recordTitle
                break
            }
        }
        holder.unlockTimeView.setText(Utils.getDateDay(itemData.createTime))
        holder.bindView.visibility = View.GONE
        holder.itemView.setOnClickListener { v: View ->
            ShowCodeActivity.startActivity(
                v.context,
                JSONObject.toJSONString(itemData)
            )
        }
        if (!TextUtils.isEmpty(itemData.avatarUrl)) {
            Utils.showImageUrl(itemData.avatarUrl, holder.user_face)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameView: TextView
        val unlockTimeView: TextView
        val bindView: Button
        val user_face: ImageView

        init {
            userNameView = itemView.findViewById(R.id.userName)
            unlockTimeView = itemView.findViewById(R.id.unlockTime)
            user_face = itemView.findViewById(R.id.user_face)
            bindView = itemView.findViewById(R.id.bindView)
        }
    }

}