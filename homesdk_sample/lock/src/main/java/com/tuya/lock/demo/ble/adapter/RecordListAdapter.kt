package com.tuya.lock.demo.ble.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.optimus.lock.api.bean.Record
import com.thingclips.smart.sdk.bean.DeviceBean
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.activity.ShowCodeActivity
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.Utils.getDateDay

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class RecordListAdapter: RecyclerView.Adapter<RecordListAdapter.ViewHolder>() {

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
            LayoutInflater.from(parent.context).inflate(R.layout.lock_records_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemData = data[position]
        if (null == deviceBean || null == deviceBean!!.getSchemaMap()) {
            Log.e(Constant.TAG, "deviceBean OR getSchemaMap is null")
            return
        }
        for ((key, schemaItem) in deviceBean!!.getSchemaMap()) {
            if (key == itemData.dpId) {
                holder.userNameView.text = schemaItem.name
                break
            }
        }
        holder.unlockTimeView.text = getDateDay(itemData.createTime)
        holder.unlockTypeView.text = itemData.userName
        if (itemData.tags == 1) {
            holder.unlockTagsView.text = "劫持"
        } else {
            holder.unlockTagsView.text = ""
        }
        holder.itemView.setOnClickListener { v: View ->
            ShowCodeActivity.startActivity(
                v.context,
                JSONObject.toJSONString(itemData)
            )
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameView: TextView
        val unlockTimeView: TextView
        val unlockTypeView: TextView
        val unlockTagsView: TextView

        init {
            userNameView = itemView.findViewById(R.id.userName)
            unlockTimeView = itemView.findViewById(R.id.unlockTime)
            unlockTypeView = itemView.findViewById(R.id.unlockType)
            unlockTagsView = itemView.findViewById(R.id.unlockTags)
        }
    }

}