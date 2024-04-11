package com.tuya.lock.demo.ble.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.sdk.optimus.lock.bean.ble.TempPasswordBeanV3
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.activity.PasswordOldOnlineDetailActivity
import com.tuya.lock.demo.ble.activity.ShowCodeActivity
import com.tuya.lock.demo.common.constant.Constant

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class PasswordOldOnlineListAdapter: RecyclerView.Adapter<PasswordOldOnlineListAdapter.ViewHolder>() {

    private var data:MutableList<TempPasswordBeanV3> = ArrayList()
    private var callback: Callback? = null
    private var mDevId: String? = null

    fun getData(): MutableList<TempPasswordBeanV3> {
        return data
    }

    fun setDevId(devId: String?) {
        mDevId = devId
    }

    fun setData(list: MutableList<TempPasswordBeanV3>) {
        data = list
    }

    fun delete(callback: Callback?) {
        this.callback = callback
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.password_old_online_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bean = data[position]
        Log.i(Constant.TAG, JSONObject.toJSONString(bean))
        holder.tvDeviceName.text = bean.name
        holder.tvDeviceStatus.text = bean.passwordId.toString()
        holder.button_delete.setOnClickListener { v: View? ->
            callback!!.remove(
                bean,
                position
            )
        }
        holder.button_edit.setOnClickListener { v: View ->
            PasswordOldOnlineDetailActivity.startActivity(
                v.context,
                bean,
                mDevId,
                1,
                bean.availTimes
            )
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
        val button_edit: Button
        val button_delete: Button

        init {
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName)
            tvDeviceStatus = itemView.findViewById(R.id.tvDeviceStatus)
            button_edit = itemView.findViewById(R.id.button_edit)
            button_delete = itemView.findViewById(R.id.button_delete)
        }
    }


    interface Callback {
        fun remove(bean: TempPasswordBeanV3, position: Int)
    }


    fun remove(position: Int) {
        data.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, data.size - position)
    }

}