package com.tuya.appbizsdk.scenebiz.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuya.appbizsdk.scenebiz.databinding.DetailTitleItemBinding

class DetailTitleAdapter() : ListAdapter<Int, RecyclerView.ViewHolder>(TitleDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = DetailTitleItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TitleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val titleResId = getItem(position)
        (holder as TitleViewHolder).binding(titleResId)
    }
}

class TitleViewHolder(
    private val binding: DetailTitleItemBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun binding(titleResId: Int) {
        binding.root.text = binding.root.context.getString(titleResId)
    }
}

private object TitleDiff : DiffUtil.ItemCallback<Int>() {
    override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
        return oldItem == newItem
    }
}