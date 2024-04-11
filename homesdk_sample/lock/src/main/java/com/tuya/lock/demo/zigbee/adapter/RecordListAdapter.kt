package com.tuya.lock.demo.zigbee.adapter

import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.android.common.utils.L
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.optimus.lock.api.zigbee.response.RecordBean
import com.thingclips.smart.sdk.bean.DeviceBean
import com.thingclips.smart.sdk.optimus.lock.bean.ZigBeeDatePoint
import com.thingclips.smart.sdk.optimus.lock.utils.LockUtil
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.activity.ShowCodeActivity
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.Utils
import com.tuya.lock.demo.zigbee.activity.MemberSelectListActivity

/**
 *
 * Created by HuiYao on 2024/3/1
 */
class RecordListAdapter: RecyclerView.Adapter<RecordListAdapter.ViewHolder>() {

    private var data: MutableList<RecordBean.DataBean> = ArrayList()
    private var deviceBean: DeviceBean? = null

    fun setDevice(devId: String?) {
        deviceBean = ThingHomeSdk.getDataInstance().getDeviceBean(devId)
    }

    fun setData(list: MutableList<RecordBean.DataBean>) {
        data = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_zigbee_lock_records, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemData = data[position]
        if (null == deviceBean || null == deviceBean!!.getSchemaMap()) {
            L.e(Constant.TAG, "deviceBean OR getSchemaMap is null")
            return
        }
        for ((key, schemaItem) in deviceBean!!.getSchemaMap()) {
            if (TextUtils.equals(key, itemData.dpId.toString())) {
                var recordTitle =
                    itemData.userName + schemaItem.name + "(" + itemData.dpValue + ")"
                if (itemData.tags == 1) {
                    recordTitle = "[hiJack]$recordTitle"
                }
                holder.userNameView.text = recordTitle
                break
            }
        }
        holder.unlockTimeView.setText(Utils.getDateDay(itemData.gmtCreate))
        if (TextUtils.equals(
                LockUtil.convertCode2Id(
                    deviceBean!!.devId,
                    ZigBeeDatePoint.UNLOCK_FINGERPRINT
                ), itemData.dpId.toString()
            ) ||
            TextUtils.equals(
                LockUtil.convertCode2Id(
                    deviceBean!!.devId,
                    ZigBeeDatePoint.UNLOCK_PASSWORD
                ), itemData.dpId.toString()
            ) ||
            TextUtils.equals(
                LockUtil.convertCode2Id(
                    deviceBean!!.devId,
                    ZigBeeDatePoint.UNLOCK_CARD
                ), itemData.dpId.toString()
            )
        ) {
            if (TextUtils.isEmpty(itemData.unlockName)) {
                holder.bindView.visibility = View.VISIBLE
                holder.bindView.setOnClickListener { v: View? ->
                    val list: MutableList<String> =
                        ArrayList()
                    val unlockId =
                        itemData.dpId.toString() + "-" + itemData.dpValue
                    list.add(unlockId)
                    MemberSelectListActivity.startActivity(
                        holder.bindView.context,
                        deviceBean!!.devId,
                        list,
                        1
                    )
                }
            } else {
                holder.bindView.visibility = View.GONE
            }
        } else {
            holder.bindView.visibility = View.GONE
        }
        holder.itemView.setOnClickListener { v: View ->
            ShowCodeActivity.startActivity(
                v.context,
                JSONObject.toJSONString(itemData)
            )
        }
        if (!TextUtils.isEmpty(itemData.avatar)) {
            Utils.showImageUrl(itemData.avatar, holder.user_face)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }


    open class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameView: TextView
        val unlockTimeView: TextView
        val bindView: Button
        val user_face: ImageView

        init {
            userNameView = itemView.findViewById(R.id.userName)
            unlockTimeView = itemView.findViewById(R.id.unlockTime)
            user_face = itemView.findViewById(R.id.user_face)
            bindView = itemView.findViewById(R.id.bindView)
        }
    }

}