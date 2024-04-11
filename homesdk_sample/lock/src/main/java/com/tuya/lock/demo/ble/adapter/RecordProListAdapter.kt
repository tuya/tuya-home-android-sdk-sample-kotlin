package com.tuya.lock.demo.ble.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.optimus.lock.api.bean.ProRecord
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.activity.ShowCodeActivity
import com.tuya.lock.demo.common.utils.Utils.getDateDay

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class RecordProListAdapter : RecyclerView.Adapter<RecordProListAdapter.ViewHolder>() {

    private var data: MutableList<ProRecord.DataBean> = ArrayList()

    fun setData(list: MutableList<ProRecord.DataBean>) {
        data = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.lock_records_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemData = data[position]
        holder.userNameView.text = itemData.userName
        holder.unlockTimeView.text = getDateDay(itemData.time)
        holder.unlockTypeView.text = itemData.data
        holder.unlockTagsView.text = itemData.logType
        holder.itemView.setOnClickListener { v: View ->
            ShowCodeActivity.startActivity(
                v.context,
                JSONObject.toJSONString(itemData)
            )
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameView: TextView
        val unlockTimeView: TextView
        val unlockTypeView: TextView
        val unlockTagsView: TextView

        init {
            userNameView = itemView.findViewById(R.id.userName)
            unlockTimeView = itemView.findViewById(R.id.unlockTime)
            unlockTypeView = itemView.findViewById(R.id.unlockType)
            unlockTagsView = itemView.findViewById(R.id.unlockTags)
        }
    }

}