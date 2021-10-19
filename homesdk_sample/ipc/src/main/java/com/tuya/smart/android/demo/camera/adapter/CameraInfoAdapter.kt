package com.tuya.smart.android.demo.camera.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tuya.smart.android.demo.camera.databinding.RecyclerCameraInfoBinding

/**

 * TODO feature

 *

 * @author houqing <a href="mailto:developer@tuya.com"/>

 * @since 2021/7/27 2:50 PM

 */
class CameraInfoAdapter(data: MutableList<String>) : RecyclerView.Adapter<CameraInfoAdapter.ViewHolder>() {
    private var data: MutableList<String> = data
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RecyclerCameraInfoBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = data[position]
        holder.textView.maxLines = 5
    }

    override fun getItemCount(): Int {
       return data!!.size
    }
    class ViewHolder(binding: RecyclerCameraInfoBinding) : RecyclerView.ViewHolder(binding.root) {
        val textView: TextView = binding.recycleItemInfoText
    }

}