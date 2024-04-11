package com.tuya.lock.demo.ble.adapter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.sdk.optimus.lock.bean.ble.MemberInfoBean
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.activity.MemberDetailActivity
import com.tuya.lock.demo.ble.activity.MemberTimeActivity
import com.tuya.lock.demo.ble.activity.OpModeListActivity
import com.tuya.lock.demo.ble.activity.ShowCodeActivity
import com.tuya.lock.demo.common.utils.Utils

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class MemberListAdapter : RecyclerView.Adapter<MemberListAdapter.ViewHolder>() {

    private var data: MutableList<MemberInfoBean> = ArrayList()
    private var callback: Callback? = null
    private var mDevId: String? = null
    private var isProDevice = false

    fun getData(): MutableList<MemberInfoBean> {
        return data
    }

    fun setDevId(devId: String?) {
        mDevId = devId
    }

    fun setProDevice(proDevice: Boolean) {
        isProDevice = proDevice
    }

    fun setData(list: MutableList<MemberInfoBean>) {
        data.clear()
        data.addAll(list)
    }

    fun deleteUser(callback: Callback?) {
        this.callback = callback
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.member_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bean = data[position]
        holder.tvDeviceName.text = bean.nickName
        var expiredTime = bean.timeScheduleInfo.expiredTime
        if (expiredTime.toString().length == 10) {
            expiredTime *= 1000
        }
        val endTime = "过期时间：" + Utils.getDateDay(expiredTime, "yyyy-MM-dd")
        val statusStr =
            if (bean.timeScheduleInfo.isPermanent) holder.itemView.context.getString(R.string.user_in_permanent) else endTime
        holder.tvStatus.text = statusStr
        holder.user_delete.setOnClickListener { v: View? ->
            if (null != callback) {
                callback!!.remove(bean, position)
            }
        }
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
        holder.user_time.setOnClickListener { v: View ->
            MemberTimeActivity.startActivity(
                v.context,
                bean,
                mDevId
            )
        }
        holder.user_unlock.setOnClickListener { v: View ->
            OpModeListActivity.startActivity(
                v.context,
                mDevId,
                bean.userId,
                bean.lockUserId
            )
        }
        if (bean.userType == 50) {
            holder.user_delete.visibility = View.GONE
        } else {
            holder.user_delete.visibility = View.VISIBLE
        }
        if (isProDevice) {
            holder.user_time.visibility = View.VISIBLE
        } else {
            holder.user_time.visibility = View.GONE
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
        val user_delete: Button
        val user_face: ImageView
        val user_time: Button
        val user_update: Button
        val user_unlock: Button

        init {
            user_face = itemView.findViewById(R.id.user_face)
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName)
            tvStatus = itemView.findViewById(R.id.tvDeviceStatus)
            user_delete = itemView.findViewById(R.id.user_delete)
            user_time = itemView.findViewById(R.id.user_time)
            user_update = itemView.findViewById(R.id.user_update)
            user_unlock = itemView.findViewById(R.id.user_unlock)
        }
    }


    interface Callback {
        fun remove(infoBean: MemberInfoBean?, position: Int)
    }


    fun remove(position: Int) {
        data.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, data.size - position)
    }
}