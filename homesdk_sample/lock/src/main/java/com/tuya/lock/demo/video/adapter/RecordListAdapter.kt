package com.tuya.lock.demo.video.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.android.common.utils.L
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.sdk.bean.DeviceBean
import com.thingclips.thinglock.videolock.bean.LogsListBean.LogsInfoBean
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.activity.ShowCodeActivity
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.Utils
import com.tuya.lock.demo.video.activity.LogRecordDetailActivity

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class RecordListAdapter : RecyclerView.Adapter<RecordListAdapter.ViewHolder>() {

    private var data: MutableList<LogsInfoBean> = ArrayList()
    private var deviceBean: DeviceBean? = null

    fun setDevice(devId: String?) {
        deviceBean = ThingHomeSdk.getDataInstance().getDeviceBean(devId)
    }

    fun setData(list: MutableList<LogsInfoBean>) {
        data = list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_video_lock_records, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemData = data[position]
        if (null == deviceBean || null == deviceBean!!.getSchemaMap()) {
            L.e(Constant.TAG, "deviceBean OR getSchemaMap is null")
            return
        }
        holder.userNameView.text = itemData.logCategory
        holder.unlockTimeView.setText(Utils.getDateDay(itemData.time))
        holder.itemView.setOnClickListener { v: View ->
            ShowCodeActivity.startActivity(
                v.context,
                JSONObject.toJSONString(itemData)
            )
        }
        if (null != itemData.mediaInfoList && itemData.mediaInfoList.size > 0) {
            val mediaInfo = itemData.mediaInfoList[0]
            holder.bindView.visibility = View.VISIBLE
            holder.bindView.text = "点击查看"
            holder.bindView.setOnClickListener { v ->
                LogRecordDetailActivity.startActivity(
                    v.context,
                    mediaInfo
                )
            }
        } else {
            holder.bindView.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameView: TextView
        val unlockTimeView: TextView
        val bindView: Button

        init {
            userNameView = itemView.findViewById(R.id.userName)
            unlockTimeView = itemView.findViewById(R.id.unlockTime)
            bindView = itemView.findViewById(R.id.bindView)
        }
    }

}