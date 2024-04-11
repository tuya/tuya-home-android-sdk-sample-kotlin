package com.thingclips.smart.devicebiz.biz.group

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.thingclips.group_usecase_api.bean.GroupDeviceDetailBean
import com.thingclips.smart.devicebiz.R

class AddedDeviceAdapter(
    private val mContext: Context,
    private val list: ArrayList<GroupDeviceDetailBean>
) : RecyclerView.Adapter<AddedDeviceHolder>() {
    private var onItemClickListener: ((GroupDeviceDetailBean) -> Unit)? = null
    fun setDataList(data: List<GroupDeviceDetailBean>) {
        list.clear()
        list.addAll(data)
        notifyDataSetChanged()
    }

    fun addData(bean: GroupDeviceDetailBean) {
        if (!list.contains(bean)) {
            list.add(bean)
            notifyDataSetChanged()
        }
    }

    fun removeData(bean: GroupDeviceDetailBean) {
        list.remove(bean)
        notifyDataSetChanged()
    }

    fun getData(): ArrayList<GroupDeviceDetailBean> {
        return list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddedDeviceHolder {
        return AddedDeviceHolder(
            LayoutInflater.from(mContext)
                .inflate(R.layout.item_group_list, null)
        )
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: AddedDeviceHolder, position: Int) {
        val groupBean: GroupDeviceDetailBean = list[position]
        if (groupBean.isOnline) {
            // 在线
            val drawableRight = mContext.resources.getDrawable(R.mipmap.group_icon_delete)
            holder.imgIcon.setImageDrawable(drawableRight)
            holder.imgIcon.setBackgroundResource(R.drawable.group_shape_delete)

            holder.tvOnLine.visibility = View.GONE
        } else {
            // 离线
            val drawableRight = mContext.resources.getDrawable(R.mipmap.group_icon_delete)
            holder.imgIcon.setImageDrawable(drawableRight)
            holder.imgIcon.setBackgroundResource(R.drawable.group_shape_added_offline)

            holder.tvOnLine.visibility = View.VISIBLE
        }

        if (groupBean.deviceBean == null) {
            holder.imgDevice.setImageResource(R.mipmap.panel_device_icon)
        } else {
            if (TextUtils.isEmpty(groupBean.deviceBean.iconUrl)) {
                holder.imgDevice.setImageResource(R.mipmap.panel_device_icon)
            } else {
                val uri = Uri.parse(groupBean.deviceBean.iconUrl)
                holder.imgDevice.setImageURI(uri)
            }
        }

        holder.tvDeviceName.text = if (groupBean.deviceBean == null) {
            "New Device"
        } else {
            groupBean.deviceBean.name ?: "New Device"
        }



        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(groupBean)
        }
    }

    /**
     * check 点击事件
     */
    fun setOnItemClickListener(listener: ((GroupDeviceDetailBean) -> Unit)) {
        onItemClickListener = listener
    }
}


class AddedDeviceHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val imgIcon: ImageView = itemView.findViewById(R.id.imgIcon)
    val imgDevice: SimpleDraweeView = itemView.findViewById(R.id.imgDevice)
    val tvDeviceName: TextView = itemView.findViewById(R.id.tvDeviceName)
    val tvOnLine: TextView = itemView.findViewById(R.id.tvOnLine)
}