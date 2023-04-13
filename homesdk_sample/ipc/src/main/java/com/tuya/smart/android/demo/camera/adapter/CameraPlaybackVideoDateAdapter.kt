package com.tuya.smart.android.demo.camera.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tuya.smart.android.demo.camera.R

class CameraPlaybackVideoDateAdapter(context: Context?, dateList: List<String>?) :
    RecyclerView.Adapter<CameraPlaybackVideoDateAdapter.MyViewHolder?>() {
    private val mInflater: LayoutInflater
    private val dateList: List<String>?
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
        val str = dateList?.get(position)
        holder.mDate.text = str
        holder.itemView.setOnClickListener(View.OnClickListener {
            listener?.onClick(str)
        })
    }

    override fun getItemCount(): Int {
        return dateList?.size ?: 0
    }

    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var mDate: TextView

        init {
            mDate = view.findViewById<TextView>(R.id.tv_date)
        }
    }

    interface OnTimeItemListener {
        fun onClick(date: String?)
    }
}