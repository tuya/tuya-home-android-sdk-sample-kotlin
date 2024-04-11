package com.tuya.lock.demo.zigbee.adapter

import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.fastjson.JSONObject
import com.thingclips.smart.android.common.utils.L
import com.thingclips.smart.optimus.lock.api.zigbee.response.PasswordBean
import com.tuya.lock.demo.R
import com.tuya.lock.demo.ble.activity.ShowCodeActivity
import com.tuya.lock.demo.common.constant.Constant
import com.tuya.lock.demo.common.utils.DialogUtils
import com.tuya.lock.demo.common.utils.Utils
import com.tuya.lock.demo.zigbee.activity.PasswordUpdateActivity
import java.util.Locale

/**
 *
 * Created by HuiYao on 2024/3/1
 */
class PasswordListAdapter: RecyclerView.Adapter<PasswordListAdapter.ViewHolder>() {
    private var data: MutableList<PasswordBean.DataBean> = ArrayList()
    private var callback: Callback? = null
    private var mDevId: String? = null
    private var isShowDelete = true

    fun getData(): List<PasswordBean.DataBean> {
        return data
    }

    fun setDevId(devId: String?) {
        mDevId = devId
    }

    fun setData(list: List<PasswordBean.DataBean>) {
        data.clear()
        data.addAll(list)
    }

    fun addCallback(callback: Callback?) {
        this.callback = callback
    }

    fun hideDelete() {
        isShowDelete = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_zigbee_password_list, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bean = data[position]
        Log.i(Constant.TAG, JSONObject.toJSONString(bean))
        var operate = ""
        var color = holder.itemView.resources.getColor(R.color.black)
        if (bean.phase == 2) {
            if (bean.operate == 125) {
                if (TextUtils.equals(bean.deliveryStatus, "1")) {
                    operate = holder.itemView.resources.getString(R.string.zigbee_deleting)
                    color = holder.itemView.resources.getColor(R.color.red)
                }
            } else {
                if (TextUtils.equals(bean.deliveryStatus, "1")) {
                    operate = holder.itemView.resources.getString(R.string.zigbee_editing)
                    color = holder.itemView.resources.getColor(R.color.lock_org)
                } else if (TextUtils.equals(bean.deliveryStatus, "2") && bean.isIfEffective) {
                    operate = holder.itemView.resources.getString(R.string.zigbee_in_force)
                    color = holder.itemView.resources.getColor(R.color.green)
                } else if (TextUtils.equals(bean.deliveryStatus, "2") && !bean.isIfEffective) {
                    operate = holder.itemView.resources.getString(R.string.zigbee_not_active)
                    color = holder.itemView.resources.getColor(R.color.gray)
                }
            }
        } else if (bean.phase == 3) {
            if (TextUtils.equals(bean.deliveryStatus, "1")) {
                operate = holder.itemView.resources.getString(R.string.zigbee_editing)
                color = holder.itemView.resources.getColor(R.color.lock_org)
            } else if (TextUtils.equals(bean.deliveryStatus, "2")) {
                operate = holder.itemView.resources.getString(R.string.zigbee_frozen)
                color = holder.itemView.resources.getColor(R.color.red)
            }
        }
        val count =
            if (bean.oneTime == 1) "[" + holder.itemView.resources.getString(R.string.zigbee_disposable) + "] " else "[" + holder.itemView.resources.getString(
                R.string.zigbee_cycle
            ) + "] "
        val name = count + bean.name
        holder.tvDeviceName.text = name
        holder.tvDeviceStatus.text = operate
        holder.tvDeviceStatus.setTextColor(color)
        val passwordStr = "password:" + bean.password
        holder.password_view.text = passwordStr
        val timeStr: String =
            Utils.getDateOneDay(bean.effectiveTime) + " ~ " + Utils.getDateOneDay(bean.invalidTime)
        val stringBuilder = StringBuilder()
        if (bean.modifyData.scheduleList.size > 0) {
            val scheduleBean = bean.modifyData.scheduleList[0]
            stringBuilder.append(getDayName(scheduleBean.workingDay))
            stringBuilder.append(" [")
            val effectiveTimeStr = scheduleBean.effectiveTime.toString()
            if (effectiveTimeStr.length == 3) {
                val effectiveTimeEnd =
                    effectiveTimeStr[0].toString() + ":" + effectiveTimeStr.substring(1, 3)
                stringBuilder.append(effectiveTimeEnd)
            } else if (scheduleBean.effectiveTime.toString().length == 4) {
                val effectiveTimeEnd =
                    effectiveTimeStr.substring(0, 2) + ":" + effectiveTimeStr.substring(2, 4)
                stringBuilder.append(effectiveTimeEnd)
            } else {
                stringBuilder.append(scheduleBean.effectiveTime)
            }
            stringBuilder.append("-")
            val invalidTimeStr = scheduleBean.invalidTime.toString()
            if (invalidTimeStr.length == 3) {
                val invalidTimeEnd =
                    invalidTimeStr[0].toString() + ":" + invalidTimeStr.substring(1, 3)
                stringBuilder.append(invalidTimeEnd)
            } else if (invalidTimeStr.length == 4) {
                val invalidTimeEnd =
                    invalidTimeStr.substring(0, 2) + ":" + invalidTimeStr.substring(2, 4)
                stringBuilder.append(invalidTimeEnd)
            } else {
                stringBuilder.append(scheduleBean.invalidTime)
            }
            stringBuilder.append("]")
            if (scheduleBean.isAllDay || scheduleBean.invalidTime == 0 && scheduleBean.effectiveTime == 0) {
                holder.schedule_view.visibility = View.GONE
            } else {
                holder.schedule_view.text = stringBuilder.toString()
                holder.schedule_view.visibility = View.VISIBLE
            }
        } else {
            holder.schedule_view.visibility = View.GONE
        }
        holder.time_view.text = timeStr
        holder.itemView.setOnClickListener { v: View ->
            if (isShowDelete) {
                showDialog(v.context, bean, position)
            } else {
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

    private fun showDialog(context: Context, bean: PasswordBean.DataBean, position: Int) {
        DialogUtils.showPassword(context, bean, object : DialogUtils.Callback {
            override fun edit(bean: PasswordBean.DataBean?) {
                callback!!.edit(bean, position)
            }

            override fun delete(bean: PasswordBean.DataBean?) {
                callback!!.remove(bean, position)
            }

            override fun rename(bean: PasswordBean.DataBean?) {
                PasswordUpdateActivity.startActivity(context, bean, mDevId)
            }

            override fun freeze(bean: PasswordBean.DataBean?, isFreeze: Boolean) {
                callback!!.freeze(bean, position, isFreeze)
            }

            override fun showCode(bean: PasswordBean.DataBean?) {
                ShowCodeActivity.startActivity(context, JSONObject.toJSONString(bean))
            }
        })
    }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDeviceName: TextView
        val tvDeviceStatus: TextView
        val password_view: TextView
        val time_view: TextView
        val schedule_view: TextView

        init {
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName)
            tvDeviceStatus = itemView.findViewById(R.id.tvDeviceStatus)
            password_view = itemView.findViewById(R.id.password_view)
            time_view = itemView.findViewById(R.id.time_view)
            schedule_view = itemView.findViewById(R.id.schedule_view)
        }
    }


    interface Callback {
        fun remove(bean: PasswordBean.DataBean?, position: Int)
        fun freeze(bean: PasswordBean.DataBean?, position: Int, isFreeze: Boolean)
        fun edit(bean: PasswordBean.DataBean?, position: Int)
    }


    fun remove(position: Int) {
        data.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, data.size - position)
    }

    private fun getDayName(workingDay: Int): String? {
        val workDay =
            String.format(Locale.CHINA, "%07d", Integer.toBinaryString(workingDay).toInt())
        L.i(Constant.TAG, "workDay:$workDay")
        val dayName: MutableList<String> = ArrayList()
        if (workDay[0] == '1') {
            dayName.add("7")
        }
        if (workDay[1] == '1') {
            dayName.add("1")
        }
        if (workDay[2] == '1') {
            dayName.add("2")
        }
        if (workDay[3] == '1') {
            dayName.add("3")
        }
        if (workDay[4] == '1') {
            dayName.add("4")
        }
        if (workDay[5] == '1') {
            dayName.add("5")
        }
        if (workDay[6] == '1') {
            dayName.add("6")
        }
        return java.lang.String.join("、", dayName)
    }
}