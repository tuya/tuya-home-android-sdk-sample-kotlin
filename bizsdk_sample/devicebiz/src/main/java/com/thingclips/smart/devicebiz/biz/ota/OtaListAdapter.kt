package com.thingclips.smart.devicebiz.biz.ota

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import com.thingclips.sdk.device.enums.DevUpgradeStatusEnum
import com.thingclips.smart.devicebiz.R
import com.thingclips.smart.devicebiz.bean.OtaInfoItemBean

class OtaListAdapter : RecyclerView.Adapter<OtaListAdapter.ViewHolder>() {
    private var otaDevList: MutableList<OtaInfoItemBean> = ArrayList(8)

    private lateinit var mOnItemClickListener: OnItemClickListener

    fun setData(beans: MutableList<OtaInfoItemBean>,index:Int?) {
        otaDevList = beans
        if (index!=null){
            notifyItemChanged(index)
        }else{
            notifyDataSetChanged()
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_ota_list, parent, false)
        return ViewHolder(view)

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val otaDev = otaDevList[position]
        holder.deviceName?.text = otaDev.name
        holder.deviceIcon?.setImageURI(otaDev.icon)
        holder.updateStatus?.isEnabled = otaDev.isEnable
        holder.updateStatus?.setOnClickListener {
            mOnItemClickListener.onButtonClick(position)
            otaDev.isEnable = false
            otaDevList[position] = otaDev
            holder.updateStatus?.isEnabled = false
        }
        when (otaDev.status) {
            DevUpgradeStatusEnum.SUCCESS -> holder.updateStatus?.text = "success"
            DevUpgradeStatusEnum.FAILURE -> holder.updateStatus?.text = "fail"
            DevUpgradeStatusEnum.UPGRADING -> holder.updateStatus?.text = "updating"
            else -> {}
        }
    }

    override fun getItemCount(): Int {
        return otaDevList.size
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var deviceIcon: SimpleDraweeView? = null
        var deviceName: TextView? = null
        var updateStatus: Button? = null

        init {
            deviceIcon = itemView.findViewById(R.id.ota_item_icon)
            deviceName = itemView.findViewById(R.id.ota_item_title)
            updateStatus = itemView.findViewById(R.id.update_btn)
        }
    }

    fun setOnItemClickListener(l: OnItemClickListener) {
        this.mOnItemClickListener = l
    }

    interface OnItemClickListener {
        fun onItemClick(bean: OtaInfoItemBean?, position: Int)
        fun onButtonClick(position: Int)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        // 移除监听器，防止内存泄漏
        holder.updateStatus?.setOnClickListener(null)
        holder.itemView.setOnClickListener(null)
    }
}