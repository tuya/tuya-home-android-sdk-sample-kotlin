package com.tuya.lock.demo.wifi.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.optimus.lock.api.bean.TempPassword
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.activity.ShowCodeActivity
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.Utils

/**
 *
 * Created by HuiYao on 2024/3/1
 */
class PasswordListAdapter: RecyclerView.Adapter<PasswordListAdapter.ViewHolder>() {

    private var data: MutableList<TempPassword> = ArrayList()
    private var callback: Callback? = null

    fun getData(): MutableList<TempPassword> {
        return data
    }

    fun setData(list: MutableList<TempPassword>) {
        data.clear()
        data.addAll(list)
    }

    fun addCallback(callback: Callback?) {
        this.callback = callback
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_zigbee_password_list, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bean = data[position]
        Log.i(Constant.TAG, JSONObject.toJSONString(bean))
        holder.tvDeviceName.text = bean.name
        val timeStr: String =
            Utils.getDateOneDay(bean.effectiveTime) + " ~ " + Utils.getDateOneDay(bean.invalidTime)
        holder.time_view.text = timeStr
        holder.itemView.setOnClickListener { v: View ->
            ShowCodeActivity.startActivity(
                v.context,
                JSONObject.toJSONString(bean)
            )
        }
        holder.itemView.setOnLongClickListener { v: View? ->
            callback!!.remove(bean, position)
            false
        }
        holder.password_view.visibility = View.GONE
        holder.schedule_view.visibility = View.GONE
        holder.tvDeviceStatus.text = getStatus(bean.status)
    }

    private fun getStatus(status: Int): String? {
        var statusStr = ""
        when (status) {
            TempPassword.Status.REMOVED -> statusStr = "Removed"
            TempPassword.Status.INVALID -> statusStr = "Invalid"
            TempPassword.Status.TO_BE_PUBILSH -> statusStr = "ToBePublish"
            TempPassword.Status.WORKING -> statusStr = "Working"
            TempPassword.Status.TO_BE_DELETED -> statusStr = "ToBeDeleted"
            TempPassword.Status.EXPIRED -> statusStr = "Expired"
        }
        return statusStr
    }

    override fun getItemCount(): Int {
        return data.size
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDeviceName: TextView
        val tvDeviceStatus: TextView
        val password_view: TextView
        val time_view: TextView
        val schedule_view: TextView

        init {
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName)
            tvDeviceStatus = itemView.findViewById(R.id.tvDeviceStatus)
            password_view = itemView.findViewById(R.id.password_view)
            time_view = itemView.findViewById(R.id.time_view)
            schedule_view = itemView.findViewById(R.id.schedule_view)
        }
    }


    interface Callback {
        fun remove(bean: TempPassword?, position: Int)
    }


    fun remove(position: Int) {
        data.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, data.size - position)
    }
}