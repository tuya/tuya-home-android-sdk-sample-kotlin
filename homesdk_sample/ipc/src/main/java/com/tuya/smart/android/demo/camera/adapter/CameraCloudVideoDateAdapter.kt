package com.tuya.smart.android.demo.camera.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thingclips.smart.camera.middleware.cloud.bean.CloudDayBean
import com.tuya.smart.android.demo.camera.R

class CameraCloudVideoDateAdapter(context: Context?, dateList: List<CloudDayBean>) :
    RecyclerView.Adapter<CameraCloudVideoDateAdapter.MyViewHolder?>() {
    private val mInflater: LayoutInflater
    private val dateList: List<CloudDayBean>
    private var listener: OnTimeItemListener? = null

    init {
        mInflater = LayoutInflater.from(context)
        this.dateList = dateList
    }

    fun setListener(listener: OnTimeItemListener?) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(mInflater.inflate(R.layout.activity_camera_video_date_tem,
            parent,
            false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val bean: CloudDayBean = dateList[position]
        holder.mDate.text = bean.uploadDay
        holder.itemView.setOnClickListener {
            listener?.onClick(bean)
        }
    }

    override fun getItemCount(): Int {
        return dateList.size
    }

    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var mDate: TextView

        init {
            mDate = view.findViewById<TextView>(R.id.tv_date)
        }
    }

    interface OnTimeItemListener {
        fun onClick(date: CloudDayBean?)
    }
}