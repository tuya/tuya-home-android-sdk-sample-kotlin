package com.tuya.lock.demo.zigbee.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.optimus.lock.api.zigbee.response.OpModeBean
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.activity.ShowCodeActivity
import com.tuya.lock.demo.common.bean.ZigbeeUnlockInfo

/**
 *
 * Created by HuiYao on 2024/3/1
 */
class OpModeListAdapter: RecyclerView.Adapter<OpModeListAdapter.ViewHolder>() {

    private var data: MutableList<ZigbeeUnlockInfo> = ArrayList()
    private var mCallback: Callback? = null

    fun getData(): List<ZigbeeUnlockInfo> {
        return data
    }

    fun setData(list: List<ZigbeeUnlockInfo>) {
        data.clear()
        data.addAll(list)
    }

    fun addCallback(callback: Callback?) {
        mCallback = callback
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == 0) {
            val view: View = LayoutInflater.from(parent.context)
                .inflate(R.layout.unlock_mode_list_head, parent, false)
            HeadHolder(view)
        } else {
            val view: View = LayoutInflater.from(parent.context)
                .inflate(R.layout.unlock_mode_list_item, parent, false)
            ItemHolder(view)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bean: ZigbeeUnlockInfo = data[position]
        if (holder is HeadHolder) {
            val title: String? = bean.name
            holder.name_view.text = title
            holder.add_view.setOnClickListener { v: View? ->
                if (null != mCallback) {
                    mCallback!!.add(bean, position)
                }
            }
        } else {
            val itemHolder = holder as ItemHolder
            val infoBean: OpModeBean? = bean.infoBean
            val hiJack =
                if (bean.infoBean?.unlockAttr == 1) "（" + holder.itemView.context.getString(R.string.zigbee_hijack) + "）" else ""
            val lockName = infoBean?.unlockName + hiJack
            itemHolder.name_view.text = lockName
            itemHolder.delete_view.setOnClickListener { v: View? ->
                if (null != mCallback) {
                    mCallback!!.delete(bean, position)
                }
            }
            itemHolder.edit_view.setOnClickListener { v: View? ->
                if (null != mCallback) {
                    mCallback!!.edit(bean, position)
                }
            }
            itemHolder.itemView.setOnClickListener { v: View ->
                ShowCodeActivity.startActivity(
                    v.context,
                    JSONObject.toJSONString(bean.infoBean)
                )
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        val bean: ZigbeeUnlockInfo = data[position]
        return bean.type
    }


    open class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)


    internal class HeadHolder(itemView: View) : ViewHolder(itemView) {
        var name_view: TextView
        var add_view: Button

        init {
            name_view = itemView.findViewById(R.id.name_view)
            add_view = itemView.findViewById(R.id.add_view)
        }
    }


    internal class ItemHolder(itemView: View) : ViewHolder(itemView) {
        var name_view: TextView
        var delete_view: Button
        var edit_view: Button

        init {
            name_view = itemView.findViewById(R.id.name_view)
            delete_view = itemView.findViewById(R.id.delete_view)
            edit_view = itemView.findViewById(R.id.edit_view)
        }
    }


    interface Callback {
        fun edit(info: ZigbeeUnlockInfo, position: Int)
        fun delete(info: ZigbeeUnlockInfo, position: Int)
        fun add(info: ZigbeeUnlockInfo, position: Int)
    }


    fun remove(position: Int) {
        data.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, data.size - position)
    }
}