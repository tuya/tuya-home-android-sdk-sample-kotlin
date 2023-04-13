package com.tuya.smart.android.demo.camera.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thingclips.smart.camera.middleware.cloud.bean.TimePieceBean
import com.tuya.smart.android.demo.camera.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by huangdaju on 2018/3/5.
 */
class CameraVideoTimeAdapter(context: Context?, timePieceBeans: List<TimePieceBean>) :
    RecyclerView.Adapter<CameraVideoTimeAdapter.MyViewHolder?>() {
    private val mInflater: LayoutInflater
    private val timePieceBeans: List<TimePieceBean>
    private var listener: OnTimeItemListener? = null

    init {
        mInflater = LayoutInflater.from(context)
        this.timePieceBeans = timePieceBeans
    }

    fun setListener(listener: OnTimeItemListener?) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(mInflater.inflate(R.layout.activity_camera_video_time_tem,
            parent,
            false))
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val ipcVideoBean: TimePieceBean = timePieceBeans[position]
        holder.mTvStartTime.text = timeFormat(ipcVideoBean.startTime * 1000L)
        val lastTime: Int = ipcVideoBean.endTime - ipcVideoBean.startTime
        holder.mTvDuration.text = holder.mTvDuration.context
            .getString(R.string.duration) + changeSecond(lastTime)
        holder.itemView.setOnClickListener(View.OnClickListener {
            listener?.onClick(ipcVideoBean)
        })
        holder.itemView.setOnLongClickListener {
            listener?.onLongClick(ipcVideoBean)
            true
        }
    }

    override fun getItemCount(): Int {
        return timePieceBeans.size
    }

    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var mTvStartTime: TextView
        var mTvDuration: TextView

        init {
            mTvStartTime = view.findViewById(R.id.time_start)
            mTvDuration = view.findViewById(R.id.time_duration)
        }
    }

    interface OnTimeItemListener {
        fun onClick(o: TimePieceBean)
        fun onLongClick(o: TimePieceBean)
    }

    companion object {
        fun timeFormat(time: Long): String {
            val sdf = SimpleDateFormat("HH:mm:ss")
            val date = Date(time)
            return sdf.format(date)
        }

        fun changeSecond(seconds: Int): String {
            val timer = StringBuilder()
            var temp: Int = seconds / 3600
            timer.append(if (temp < 10) "0$temp:" else "$temp:")
            temp = seconds % 3600 / 60
            timer.append(if (temp < 10) "0$temp:" else "$temp:")
            temp = seconds % 3600 % 60
            timer.append(if (temp < 10) "0$temp" else "" + temp)
            return timer.toString()
        }
    }
}