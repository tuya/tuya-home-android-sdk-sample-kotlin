package com.tuya.lock.demo.wifi.adapter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.android.common.utils.L
import com.thingclips.smart.optimus.lock.api.ThingUnlockType
import com.thingclips.smart.optimus.lock.api.bean.UnlockRelation
import com.thingclips.smart.sdk.optimus.lock.utils.StandardDpConverter
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.activity.ShowCodeActivity
import com.tuya.lock.demo.common.bean.WifiUnlockInfo
import com.tuya.lock.demo.common.constant.Constant

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class OpModeListAdapter: RecyclerView.Adapter<OpModeListAdapter.ViewHolder>() {
    var data: ArrayList<WifiUnlockInfo> = ArrayList()
    private var mCallback: Callback? = null
    private var mDevId: String? = null

    fun setDeviceId(deviceId: String?) {
        mDevId = deviceId
    }

    fun getData(): List<UnlockRelation> {
        val itemList: MutableList<UnlockRelation> = ArrayList()
        for (item in data) {
            if (item.type == 1) {
                val unlockRelation = UnlockRelation()
                unlockRelation.unlockType = item.dpCode
                unlockRelation.passwordNumber = item.name!!.toInt()
                itemList.add(unlockRelation)
            }
        }
        return itemList
    }

    fun setData(list: List<UnlockRelation>) {
        data.clear()
        val unlockFingerList: MutableList<WifiUnlockInfo> = ArrayList()
        val unlockPasswordList: MutableList<WifiUnlockInfo> = ArrayList()
        var fingerName = ""
        var passwordName = ""
        for ((_, schemaItem) in StandardDpConverter.getSchemaMap(mDevId)) {
            if (!TextUtils.isEmpty(fingerName) && !TextUtils.isEmpty(passwordName)) {
                break
            }
            if (TextUtils.equals(schemaItem.code, ThingUnlockType.FINGERPRINT)) {
                fingerName = schemaItem.name
            } else if (TextUtils.equals(schemaItem.code, ThingUnlockType.PASSWORD)) {
                passwordName = schemaItem.name
            }
        }
        val unlockFinger = WifiUnlockInfo()
        unlockFinger.type = 0
        unlockFinger.dpCode = ThingUnlockType.FINGERPRINT
        unlockFinger.name = fingerName
        unlockFingerList.add(unlockFinger)
        val unlockPassword = WifiUnlockInfo()
        unlockPassword.type = 0
        unlockPassword.dpCode = ThingUnlockType.PASSWORD
        unlockPassword.name = passwordName
        unlockPasswordList.add(unlockPassword)
        for (itemDetail in list) {
            if (TextUtils.equals(itemDetail.unlockType, ThingUnlockType.FINGERPRINT)) {
                val unlockInfo = WifiUnlockInfo()
                unlockInfo.type = 1
                unlockInfo.dpCode = ThingUnlockType.FINGERPRINT
                unlockInfo.name = itemDetail.passwordNumber.toString()
                unlockFingerList.add(unlockInfo)
            }
            if (TextUtils.equals(itemDetail.unlockType, ThingUnlockType.PASSWORD)) {
                val unlockInfo = WifiUnlockInfo()
                unlockInfo.type = 1
                unlockInfo.dpCode = ThingUnlockType.PASSWORD
                unlockInfo.name = itemDetail.passwordNumber.toString()
                unlockPasswordList.add(unlockInfo)
            }
        }
        data.addAll(unlockFingerList)
        data.addAll(unlockPasswordList)
        L.i(Constant.TAG, "setData======>" + JSONObject.toJSONString(data))
    }

    fun addCallback(callback: Callback?) {
        mCallback = callback
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == 0) {
            val view: View = LayoutInflater.from(parent.context)
                .inflate(R.layout.unlock_mode_list_head, parent, false)
            HeadHolder(view)
        } else {
            val view: View = LayoutInflater.from(parent.context)
                .inflate(R.layout.unlock_mode_list_item, parent, false)
            ItemHolder(view)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bean: WifiUnlockInfo = data[position]
        if (holder is HeadHolder) {
            val headHolder = holder as HeadHolder
            val title: String? = bean.name
            headHolder.name_view.text = title
            headHolder.add_view.setOnClickListener { v: View? ->
                if (null != mCallback) {
                    mCallback!!.add(bean, position)
                }
            }
        } else {
            val itemHolder = holder as ItemHolder
            itemHolder.name_view.setText(bean.name)
            itemHolder.delete_view.setOnClickListener { v: View? ->
                if (null != mCallback) {
                    mCallback!!.delete(bean, position)
                }
            }
            itemHolder.edit_view.setOnClickListener { v: View? ->
                if (null != mCallback) {
                    mCallback!!.edit(bean, position)
                }
            }
            itemHolder.itemView.setOnClickListener { v: View ->
                ShowCodeActivity.startActivity(
                    v.context,
                    JSONObject.toJSONString(bean)
                )
            }
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun getItemViewType(position: Int): Int {
        val bean: WifiUnlockInfo = data[position]
        return bean.type
    }


    open class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)


    internal class HeadHolder(itemView: View) : ViewHolder(itemView) {
        var name_view: TextView
        var add_view: Button

        init {
            name_view = itemView.findViewById<TextView>(R.id.name_view)
            add_view = itemView.findViewById<Button>(R.id.add_view)
        }
    }


    internal class ItemHolder(itemView: View) : ViewHolder(itemView) {
        var name_view: TextView
        var delete_view: Button
        var edit_view: Button

        init {
            name_view = itemView.findViewById(R.id.name_view)
            delete_view = itemView.findViewById(R.id.delete_view)
            edit_view = itemView.findViewById(R.id.edit_view)
        }
    }


    interface Callback {
        fun edit(info: WifiUnlockInfo?, position: Int)
        fun delete(info: WifiUnlockInfo?, position: Int)
        fun add(info: WifiUnlockInfo?, position: Int)
    }


    fun remove(position: Int) {
        data.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, data.size - position)
    }
}