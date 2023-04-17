package com.tuya.appsdk.sample.device.config.zigbee.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tuya.appsdk.sample.device.config.R
import com.tuya.appsdk.sample.device.config.util.sp.Preference
import com.tuya.appsdk.sample.device.config.zigbee.sub.DeviceConfigZbSubDeviceActivity
import com.thingclips.smart.sdk.bean.DeviceBean

/**
 * Zigbee Gateway List
 *
 * @author aiwen <a href="mailto:developer@tuya.com"/>
 * @since 2/25/21 10:18 AM
 */
class ZigBeeGatewayListAdapter(context: Context) :
        RecyclerView.Adapter<ZigBeeGatewayListAdapter.ViewHolder>() {

    var data: ArrayList<DeviceBean> = arrayListOf()
    var currentGatewayId: String by Preference(
            context,
            DeviceConfigZbSubDeviceActivity.CURRENT_GATEWAY_ID,
            ""
    )
    var currentGatewayName: String by Preference(
            context,
            DeviceConfigZbSubDeviceActivity.CURRENT_GATEWAY_NAME,
            ""
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val inflate = LayoutInflater.from(parent.context)
                .inflate(R.layout.device_zb_gateway_list, parent, false)

        val viewHolder = ViewHolder(inflate)



        viewHolder.itemView.setOnClickListener {
            val deviceBean = data[viewHolder.adapterPosition]
            currentGatewayId = deviceBean.devId
            currentGatewayName = deviceBean.name
            notifyDataSetChanged()
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        data[position].let {
            holder.itemName.text = it.name
            // Switch ZigBee Gateway
            if (currentGatewayId == it.devId) {
                holder.itemIcon.setImageResource(R.drawable.ic_check)
            } else {
                holder.itemIcon.setImageResource(0)
            }
        }

    }

    override fun getItemCount(): Int {
        return data.size
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val itemName: TextView = itemView.findViewById(R.id.tvName)
        val itemIcon: ImageView = itemView.findViewById(R.id.ivIcon)

    }
}