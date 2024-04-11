package com.tuya.lock.demo.ble.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.optimus.lock.api.bean.OfflineTempPassword
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.activity.PasswordOldOfflineAddRevokeActivity
import com.tuya.lock.demo.ble.activity.ShowCodeActivity
import com.tuya.lock.demo.common.constant.Constant

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class PasswordSingleRevokeListAdapter :
    RecyclerView.Adapter<PasswordSingleRevokeListAdapter.ViewHolder>() {

    private var data: MutableList<OfflineTempPassword> = ArrayList()
    private var mDevId: String? = null

    fun getData(): MutableList<OfflineTempPassword> {
        return data
    }

    fun setDevId(devId: String?) {
        mDevId = devId
    }

    fun setData(list: MutableList<OfflineTempPassword>) {
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
        holder.tvDeviceStatus.text = bean.pwdId.toString()
        holder.button_detail.visibility = View.VISIBLE
        holder.button_detail.setOnClickListener { v: View ->
            PasswordOldOfflineAddRevokeActivity.startActivity(v.context, mDevId, bean.pwdId)
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