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
import com.thingclips.smart.optimus.lock.api.bean.ProTempPasswordItem
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.activity.PasswordProOfflineRevokeActivity
import com.tuya.lock.demo.ble.activity.PasswordProOnlineDetailActivity
import com.tuya.lock.demo.ble.activity.ShowCodeActivity
import com.tuya.lock.demo.common.constant.Constant

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class PasswordProListAdapter : RecyclerView.Adapter<PasswordProListAdapter.ViewHolder>() {

    private var data:MutableList<ProTempPasswordItem> = ArrayList()
    private var callback: Callback? = null
    private var mDevId: String? = null

    fun getData(): MutableList<ProTempPasswordItem> {
        return data
    }

    fun setDevId(devId: String?) {
        mDevId = devId
    }

    fun setData(list: MutableList<ProTempPasswordItem>) {
        data = list
    }

    fun delete(callback: Callback?) {
        this.callback = callback
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.password_pro_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bean = data[position]
        Log.i(Constant.TAG, JSONObject.toJSONString(bean))
        holder.tvDeviceName.text = bean.name
        holder.itemView.setOnClickListener { v: View ->
            ShowCodeActivity.startActivity(
                v.context,
                JSONObject.toJSONString(bean)
            )
        }
        holder.button_clear.setOnClickListener { v: View ->
            PasswordProOfflineRevokeActivity.startActivity(
                v.context,
                mDevId,
                bean.unlockBindingId,
                bean.name
            )
        }
        if (bean.opModeSubType == 0 && bean.opModeType == 3) {
            holder.button_clear.visibility = View.VISIBLE
        } else {
            holder.button_clear.visibility = View.GONE
        }
        if (bean.opModeType < 3) {
            holder.button_delete.visibility = View.VISIBLE
            holder.button_edit.visibility = View.VISIBLE
            holder.button_delete.setOnClickListener { v: View? ->
                callback!!.remove(
                    bean,
                    position
                )
            }
            holder.button_edit.setOnClickListener { v: View ->
                PasswordProOnlineDetailActivity.startActivity(
                    v.context,
                    bean,
                    mDevId,
                    1
                )
            }
        } else {
            holder.button_delete.visibility = View.GONE
            holder.button_edit.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDeviceName: TextView
        val button_clear: Button
        val button_delete: Button
        val button_edit: Button

        init {
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName)
            button_clear = itemView.findViewById(R.id.button_clear)
            button_delete = itemView.findViewById(R.id.button_delete)
            button_edit = itemView.findViewById(R.id.button_edit)
        }
    }


    interface Callback {
        fun remove(passwordItem: ProTempPasswordItem, position: Int)
    }


    fun remove(position: Int) {
        data.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, data.size - position)
    }
}