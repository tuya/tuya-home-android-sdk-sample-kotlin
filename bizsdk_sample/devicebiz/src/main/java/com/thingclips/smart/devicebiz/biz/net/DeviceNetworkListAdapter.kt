package com.thingclips.smart.devicebiz.biz.net

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thingclips.smart.devicebiz.R
import com.thingclips.smart.sdk.api.wifibackup.api.bean.BackupWifiBean

class DeviceNetworkListAdapter : RecyclerView.Adapter<DeviceNetworkListAdapter.ViewHolder>() {

    private var backWifiList: List<BackupWifiBean> = ArrayList()

    private lateinit var mOnItemClickListener: OnItemClickListener

    fun setData(beans: List<BackupWifiBean>) {
        backWifiList = beans
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_device_net, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return backWifiList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val backWifi = backWifiList[position]
        holder.deviceNet?.text = backWifi.ssid
        holder.itemView.setOnClickListener {
            mOnItemClickListener.onItemClick(backWifi, position)
        }
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var deviceNet: TextView? = null

        init {
            deviceNet = itemView.findViewById(R.id.deviceNet)
        }
    }

    fun setOnItemClickListener(l: OnItemClickListener) {
        this.mOnItemClickListener = l
    }

    interface OnItemClickListener {
        fun onItemClick(bean: BackupWifiBean?, position: Int)
    }
}