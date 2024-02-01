package com.tuya.appsdk.sample.device.mgt.list.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.RecyclerView
import com.thingclips.sdk.core.PluginManager
import com.thingclips.smart.interior.api.IAppDpParserPlugin
import com.thingclips.smart.interior.api.IThingDevicePlugin
import com.tuya.appsdk.sample.device.mgt.R
import com.tuya.appsdk.sample.device.mgt.SimpleDp
import com.tuya.appsdk.sample.device.mgt.getIconFontContent

/**
 * create by dongdaqing[mibo] 2023/9/20 11:25
 */
class OperableDpAdapter(
    private val typeface: Typeface,
    data: List<SimpleDp>,
    clickListener: OnClickListener
) :
    RecyclerView.Adapter<DpViewHolder>() {

    private var listener: OnClickListener = clickListener

    private val differ by lazy {
        AsyncListDiffer(this, object : ItemCallback<SimpleDp>() {
            override fun areItemsTheSame(oldItem: SimpleDp, newItem: SimpleDp): Boolean {
                return oldItem.dpId == newItem.dpId
            }

            override fun areContentsTheSame(oldItem: SimpleDp, newItem: SimpleDp): Boolean {
                return oldItem.same(newItem)
            }
        })
    }

    init {
        differ.submitList(data)
    }

    fun update(data: List<SimpleDp>) {
        differ.submitList(data)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DpViewHolder {
        return DpViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.device_mgt_dp_item, parent, false),
            typeface
        )
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: DpViewHolder, position: Int) {
        val dp: SimpleDp = differ.currentList[position]
        if ("bool" == dp.type) {
            holder.itemView.setOnClickListener { v: View ->
                val recyclerView = v.parent as RecyclerView
                val p = recyclerView.getChildAdapterPosition(v)
                val plugin = PluginManager.service(IThingDevicePlugin::class.java)
                val simpleDp = differ.currentList[p] ?: return@setOnClickListener
                val parserPlugin = PluginManager.service(IAppDpParserPlugin::class.java)
                val list = parserPlugin.getParser(simpleDp.devId)!!.getOperableDp()
                for (parser in list) {
                    if (parser.getDpId() == simpleDp.dpId) {
                        plugin.newDeviceInstance(simpleDp.devId)
                            .publishDps(parser.getCommands((parser.getValue() as Boolean)), null)
                        return@setOnClickListener
                    }
                }
            }
        } else {
            holder.itemView.setOnClickListener(listener)
        }
        holder.iconView.text = getIconFontContent(dp.iconFont)
        holder.titleView.text = dp.dpName
        holder.statusView.text = dp.status
    }
}

class DpViewHolder(itemView: View, typeface: Typeface?) :
    RecyclerView.ViewHolder(itemView) {
    val iconView: TextView = itemView.findViewById<TextView>(R.id.iconView).apply {
        setTypeface(typeface)
    }
    val titleView: TextView = itemView.findViewById(R.id.titleView)
    val statusView: TextView = itemView.findViewById(R.id.statusView)
}