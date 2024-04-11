package com.tuya.lock.demo.zigbee.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
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
class OpModeSelectListAdapter: RecyclerView.Adapter<OpModeSelectListAdapter.ViewHolder>() {

    private var data: MutableList<ZigbeeUnlockInfo> = ArrayList()

    private var selectData: MutableList<OpModeBean> = ArrayList()

    fun getData(): List<ZigbeeUnlockInfo> {
        return data
    }

    fun setData(list: List<ZigbeeUnlockInfo>) {
        data.clear()
        data.addAll(list)
    }

    fun getSelectList(): List<String>? {
        val unlockIds: MutableList<String> = ArrayList()
        for (item in selectData) {
            unlockIds.add(item.unlockId)
        }
        return unlockIds
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == 0) {
            val view: View = LayoutInflater.from(parent.context)
                .inflate(R.layout.unlock_mode_list_head, parent, false)
            HeadHolder(view)
        } else {
            val view: View = LayoutInflater.from(parent.context)
                .inflate(R.layout.unlock_mode_select_list_item, parent, false)
            ItemHolder(view)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bean: ZigbeeUnlockInfo = data[position]
        if (holder is HeadHolder) {
            val title: String = (bean.name + " (" + bean.count) + ")"
            holder.name_view.text = title
            holder.add_view.visibility = View.GONE
        } else {
            val itemHolder = holder as ItemHolder
            val infoBean: OpModeBean? = bean.infoBean
            itemHolder.name_view.text = infoBean?.unlockName
            itemHolder.checkbox_view.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                if (isChecked) {
                    selectData.add(bean.infoBean!!)
                } else {
                    selectData.remove(bean.infoBean)
                }
            }
            itemHolder.itemView.setOnClickListener { v: View ->
                ShowCodeActivity.startActivity(
                    v.context,
                    JSONObject.toJSONString(bean)
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
        var checkbox_view: CheckBox

        init {
            name_view = itemView.findViewById(R.id.name_view)
            checkbox_view = itemView.findViewById(R.id.checkbox_view)
        }
    }


    fun remove(position: Int) {
        data.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, data.size - position)
    }

}