package com.tuya.lock.demo.ble.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thingclips.smart.sdk.optimus.lock.bean.ble.UnlockInfoBean
import com.tuya.lock.demo.R
import com.tuya.lock.demo.common.bean.UnlockInfo

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class OpModeSelectListAdapter: RecyclerView.Adapter<OpModeSelectListAdapter.ViewHolder>() {

    var data: ArrayList<UnlockInfo> = ArrayList()

    var selectData: ArrayList<UnlockInfoBean> = ArrayList()

    fun getData(): List<UnlockInfo> {
        return data
    }

    fun setData(list: List<UnlockInfo>) {
        data.clear()
        data.addAll(list)
    }

    fun getSelectList(): List<UnlockInfoBean>? {
        return selectData
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
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
        val (_, name, _, count, infoBean) = data[position]
        if (holder is HeadHolder) {
            val headHolder: HeadHolder = holder
            val title = "$name ($count)"
            headHolder.name_view.text = title
            headHolder.add_view.visibility = View.GONE
        } else {
            val itemHolder: ItemHolder = holder as ItemHolder
            itemHolder.name_view.text = infoBean!!.unlockName
            itemHolder.checkbox_view.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectData.add(infoBean)
                } else {
                    selectData.remove(infoBean)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        val (type) = data[position]
        return type
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
        var edit_view: Button? = null

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