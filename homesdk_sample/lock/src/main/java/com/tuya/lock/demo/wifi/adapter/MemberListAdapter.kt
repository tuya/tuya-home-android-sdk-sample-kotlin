package com.tuya.lock.demo.wifi.adapter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.optimus.lock.api.bean.WifiLockUser
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.activity.ShowCodeActivity
import com.tuya.lock.demo.common.utils.Utils
import com.tuya.lock.demo.wifi.activity.MemberDetailActivity

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class MemberListAdapter: RecyclerView.Adapter<MemberListAdapter.ViewHolder>() {

    private var data:MutableList<WifiLockUser> = ArrayList()
    private var callback: Callback? = null
    private var mDevId: String? = null

    fun getData(): MutableList<WifiLockUser> {
        return data
    }

    fun setDevId(devId: String?) {
        mDevId = devId
    }

    fun setData(list: List<WifiLockUser>) {
        data.clear()
        for (item in list) {
            if (null != item.userId && item.userId.length > 2) {
                data.add(item)
            }
        }
    }

    fun deleteUser(callback: Callback?) {
        this.callback = callback
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_zigbee_member_list, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bean = data[position]
        var userName = bean.userName
        if (bean.userType == 1) {
            userName = "[Family]" + bean.userName
        }
        holder.tvDeviceName.text = userName
        holder.tvStatus.text = bean.contact
        holder.itemView.setOnClickListener { v: View ->
            ShowCodeActivity.startActivity(
                v.context,
                JSONObject.toJSONString(bean)
            )
        }
        holder.user_update.setOnClickListener { v: View ->
            MemberDetailActivity.startActivity(
                v.context,
                bean,
                mDevId,
                1
            )
        }
        holder.user_unlock.visibility = View.GONE
        holder.itemView.setOnLongClickListener { v: View? ->
            if (null != callback) {
                callback!!.remove(bean, position)
            }
            false
        }
        if (!TextUtils.isEmpty(bean.avatarUrl)) {
            Utils.showImageUrl(bean.avatarUrl, holder.user_face)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDeviceName: TextView
        val tvStatus: TextView
        val user_face: ImageView
        val user_update: Button
        val user_unlock: Button

        init {
            user_face = itemView.findViewById(R.id.user_face)
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName)
            tvStatus = itemView.findViewById(R.id.tvDeviceStatus)
            user_update = itemView.findViewById(R.id.user_update)
            user_unlock = itemView.findViewById(R.id.user_unlock)
        }
    }


    interface Callback {
        fun remove(infoBean: WifiLockUser?, position: Int)
    }


    fun remove(position: Int) {
        data.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, data.size - position)
    }
}