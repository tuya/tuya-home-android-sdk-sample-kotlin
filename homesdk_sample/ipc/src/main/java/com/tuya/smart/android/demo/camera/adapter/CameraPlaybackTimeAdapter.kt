package com.tuya.smart.android.demo.camera.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tuya.smart.android.demo.camera.R
import com.tuya.smart.android.demo.camera.bean.TimePieceBean
import com.tuya.smart.android.demo.camera.databinding.ActivityCameraPlaybackTimeTemBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * TODO feature
 *
 * @author houqing <a href="mailto:developer@tuya.com"/>
 * @since 2021/7/27 8:28 PM
 */
class CameraPlaybackTimeAdapter(beans: MutableList<TimePieceBean>) :
    RecyclerView.Adapter<CameraPlaybackTimeAdapter.MyViewHolder>() {
    private val timePieceBeans: MutableList<TimePieceBean> = beans
    private var listener: OnTimeItemListener? = null
    fun setListener(listener: OnTimeItemListener?) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding =
            ActivityCameraPlaybackTimeTemBinding.inflate(LayoutInflater.from(parent.context),
                parent,
                false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val ipcVideoBean = timePieceBeans[position]
        holder.mTvStartTime.text = timeFormat(ipcVideoBean.startTime * 1000L)
        val lastTime = ipcVideoBean.endTime - ipcVideoBean.startTime
        holder.mTvDuration.text =
            holder.mTvDuration.context.getString(R.string.duration) + changeSecond(lastTime)
        holder.itemView.setOnClickListener {
            listener?.onClick(ipcVideoBean)
        }
        holder.itemView.setOnLongClickListener {
            listener?.onLongClick(ipcVideoBean)
            true
        }
    }

    fun timeFormat(time: Long): String? {
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

    override fun getItemCount(): Int {
        return timePieceBeans.size
    }

    class MyViewHolder(viewBinding: ActivityCameraPlaybackTimeTemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        var mTvStartTime: TextView = viewBinding.timeStart
        var mTvDuration: TextView = viewBinding.timeDuration
    }

    interface OnTimeItemListener {
        fun onClick(o: TimePieceBean)

        fun onLongClick(o: TimePieceBean)
    }
}