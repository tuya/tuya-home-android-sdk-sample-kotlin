/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2021 Tuya Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tuya.appsdk.sample.device.mgt.list.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.thing.smart.sweeper.SweeperActivity
import com.thingclips.sdk.core.PluginManager
import com.thingclips.smart.interior.api.IAppDpParserPlugin
import com.thingclips.smart.interior.api.IThingDevicePlugin
import com.tuya.appsdk.sample.device.mgt.R
import com.tuya.appsdk.sample.device.mgt.SimpleDevice
import com.tuya.appsdk.sample.device.mgt.control.activity.DeviceMgtControlActivity
import com.tuya.appsdk.sample.device.mgt.getIconFontContent
import com.tuya.appsdk.sample.device.mgt.list.activity.DeviceSubZigbeeActivity
import com.tuya.appsdk.sample.device.mgt.list.enum.DeviceListTypePage
import com.tuya.lock.demo.LockDeviceUtils
import com.tuya.smart.android.demo.camera.CameraUtils

/**
 * Device list adapter
 *
 * @author qianqi <a href="mailto:developer@tuya.com"/>
 * @since 2021/1/21 10:06 AM
 */
class DeviceMgtAdapter(context: Context, val type: Int) :
    RecyclerView.Adapter<DeviceMgtAdapter.ViewHolder>() {
    private val differ by lazy {
        AsyncListDiffer(this, object : ItemCallback<SimpleDevice>() {
            override fun areItemsTheSame(oldItem: SimpleDevice, newItem: SimpleDevice): Boolean {
                return oldItem.devId == newItem.devId
            }

            override fun areContentsTheSame(oldItem: SimpleDevice, newItem: SimpleDevice): Boolean {
                return oldItem.sameContent(newItem)
            }
        })
    }

    private val typeface: Typeface

    init {
        typeface = Typeface.createFromAsset(context.assets, "fonts/iconfont.ttf")
    }

    fun update(data: List<SimpleDevice>) {
        differ.submitList(data)
    }

    fun itemClicked(view: View, position: Int) {
        val deviceBean = differ.currentList[position]
        if (CameraUtils.ipcProcess(view.context, deviceBean.devId)) {
            return
        }

        if (deviceBean.category?.contains("sd") == true) {
            val intent = Intent(view.context, SweeperActivity::class.java)
            intent.putExtra("deviceId", deviceBean.devId)
            view.context.startActivity(intent)
            return
        }

        if (LockDeviceUtils.check(view.context, deviceBean.devId)) {
            return
        }
        when (type) {
            DeviceListTypePage.ZIGBEE_GATEWAY_LIST -> {
                // Navigate to zigBee sub device list
                val intent = Intent(view.context, DeviceSubZigbeeActivity::class.java)
                intent.putExtra("deviceId", deviceBean.devId)
                view.context.startActivity(intent)
            }

            else -> {
                // Navigate to zigBee sub device management
                val intent = Intent(view.context, DeviceMgtControlActivity::class.java)
                intent.putExtra("deviceId", deviceBean.devId)
                view.context.startActivity(intent)
            }
        }
    }

    private fun switchClicked(position: Int) {
        try {
            val plugin = PluginManager.service(IThingDevicePlugin::class.java)
            val device = differ.currentList[position] ?: return
            val parserPlugin = PluginManager.service(IAppDpParserPlugin::class.java)
            val iSwitch = parserPlugin.getParser(device.devId)!!.getSwitchDp()
            plugin.newDeviceInstance(device.devId)
                .publishDps(iSwitch!!.getCommands(!device.simpleSwitch!!.switchOn), null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val holder = ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.device_item, parent, false),
            ::switchClicked
        )
        holder.itemView.setOnClickListener {
            val recyclerView = it.parent as RecyclerView
            itemClicked(it, recyclerView.getChildAdapterPosition(it))
        }
        return holder
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bean: SimpleDevice = differ.currentList[position]
        Glide.with(holder.itemView).load(bean.icon).into(holder.iconView)
        holder.deviceName.text = bean.name

        if (bean.online) {
            val switchDp = bean.simpleSwitch
            if (switchDp != null) {
                holder.switchView.visibility = View.VISIBLE
                holder.switchView.setImageResource(if (switchDp.switchOn) R.drawable.on else R.drawable.off)
            } else {
                holder.switchView.visibility = View.GONE
            }
            val displays = bean.displays
            val builder = StringBuilder()
            for (dp in displays) {
                val content = dp.status
                if (!TextUtils.isEmpty(content)) {
                    builder.append(getIconFontContent(dp.iconFont)).append(content).append(" ")
                }
            }
            if (builder.isNotEmpty()) {
                holder.statusView.visibility = View.VISIBLE
                holder.statusView.typeface = typeface
            } else {
                holder.statusView.visibility = View.GONE
                holder.statusView.typeface = null
            }
            holder.statusView.text = builder
            val operable = bean.operates
            if (operable.isEmpty()) {
                holder.recyclerView.adapter = null
                holder.recyclerView.visibility = View.GONE
                holder.devFunc.visibility = View.GONE
            } else {
                val adapter = holder.recyclerView.adapter as OperableDpAdapter?
                if (adapter == null) {
                    holder.recyclerView.adapter = OperableDpAdapter(typeface, operable) {
                        holder.itemView.performClick()
                    }
                } else {
                    adapter.update(operable)
                }
                holder.devFunc.visibility = View.VISIBLE
                holder.recyclerView.visibility = View.VISIBLE
            }
        } else {
            holder.statusView.typeface = null
            holder.statusView.setText(R.string.device_mgt_offline)
            holder.devFunc.visibility = View.GONE
            holder.statusView.visibility = View.VISIBLE
            holder.switchView.visibility = View.GONE
            holder.recyclerView.visibility = View.GONE
        }
    }

    class ViewHolder(itemView: View, clickListener: (Int) -> Unit) :
        RecyclerView.ViewHolder(itemView) {
        val deviceName = itemView.findViewById<TextView>(R.id.deviceName)
        val statusView = itemView.findViewById<TextView>(R.id.statusView)
        val iconView = itemView.findViewById<ImageView>(R.id.iconView)
        val switchView = itemView.findViewById<ImageView>(R.id.switchButton).apply {
//            setOnClickListener {
//                clickListener(adapterPosition)
//            }
        }
        val recyclerView = itemView.findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = GridLayoutManager(itemView.context, 4)
        }
        val devFunc = itemView.findViewById<View>(R.id.devFuncView)
    }
}