package com.tuya.lock.demo.ble.adapter

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.optimus.lock.api.bean.OfflineTempPasswordItem
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.activity.PasswordOldOfflineAddRevokeActivity
import com.tuya.lock.demo.ble.activity.ShowCodeActivity
import com.tuya.lock.demo.common.constant.Constant

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class PasswordOldOfflineListAdapter :
    RecyclerView.Adapter<PasswordOldOfflineListAdapter.ViewHolder>() {

    private var data: MutableList<OfflineTempPasswordItem> = ArrayList()
    private var mDevId: String? = null

    fun getData(): MutableList<OfflineTempPasswordItem> {
        return data
    }

    fun setDevId(devId: String?) {
        mDevId = devId
    }

    fun setData(list: MutableList<OfflineTempPasswordItem>) {
        data = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.password_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bean = data[position]
        Log.i(Constant.TAG, JSONObject.toJSONString(bean))
        holder.tvDeviceName.text = bean.pwdName
        holder.tvDeviceStatus.text = bean.pwd.toString()
        holder.button_detail.setOnClickListener { v: View ->
            PasswordOldOfflineAddRevokeActivity.startActivity(v.context, mDevId, bean.pwdId)
        }
        if (bean.opModeSubType == 0 && bean.opModeType == 3) {
            holder.button_detail.visibility = View.VISIBLE
        } else {
            holder.button_detail.visibility = View.GONE
        }
        holder.itemView.setOnClickListener { v: View ->
            ShowCodeActivity.startActivity(
                v.context,
                JSONObject.toJSONString(bean)
            )
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDeviceName: TextView
        val tvDeviceStatus: TextView
        val button_detail: Button

        init {
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName)
            tvDeviceStatus = itemView.findViewById(R.id.tvDeviceStatus)
            button_detail = itemView.findViewById(R.id.button_detail)
        }
    }

}