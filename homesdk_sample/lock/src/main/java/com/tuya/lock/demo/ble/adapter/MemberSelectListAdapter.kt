package com.tuya.lock.demo.ble.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thingclips.smart.sdk.optimus.lock.bean.ble.MemberInfoBean
import com.tuya.lock.demo.R

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class MemberSelectListAdapter : RecyclerView.Adapter<MemberSelectListAdapter.ViewHolder>() {

    private var data:MutableList<MemberInfoBean> = ArrayList()
    private var callback: Callback? = null

    fun getData(): MutableList<MemberInfoBean> {
        return data
    }

    fun setData(list: MutableList<MemberInfoBean>?) {
        list?.apply {
            data = list
        }

    }

    fun setAlloc(callback: Callback?) {
        this.callback = callback
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.member_select_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bean = data[position]
        holder.opMode_name_view.text = bean.nickName
        holder.itemView.setOnClickListener { v: View? ->
            if (null != callback) {
                callback!!.alloc(bean, position)
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val opMode_name_view: TextView

        init {
            opMode_name_view = itemView.findViewById(R.id.opMode_name)
        }
    }


    interface Callback {
        fun alloc(infoBean: MemberInfoBean?, position: Int)
    }

}