package com.tuya.appbizsdk.scenebiz.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuya.appbizsdk.scenebiz.R
import com.tuya.appbizsdk.scenebiz.databinding.DetailAddItemBinding
import com.tuya.appbizsdk.scenebiz.detail.SceneDetailActivity.Companion.COND

class DetailAddAdapter(private val disable: Boolean, private val onClickListener: (Int) -> Unit) : ListAdapter<Int, RecyclerView.ViewHolder>(AddTitleDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = DetailAddItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AddViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val type = getItem(position)
        (holder as AddViewHolder).binding(type, disable, onClickListener)
    }
}

class AddViewHolder(
    private val binding: DetailAddItemBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun binding(type: Int, disable: Boolean, onClickListener: (Int) -> Unit) {
        val tipStr: String = if (type == COND) {
            binding.root.context.getString(R.string.add_condition)
        } else {
            binding.root.context.getString(R.string.add_action)
        }
        binding.btnAdd.text = tipStr

        if (disable) {
            binding.root.alpha = 0.1f
            binding.root.setOnClickListener(null)
        } else {
            binding.root.alpha = 1.0f
            binding.root.setOnClickListener {
                onClickListener(type)
            }
        }
    }
}

private object AddTitleDiff : DiffUtil.ItemCallback<Int>() {
    override fun areItemsTheSame(oldItem: Int, newItem: Int): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Int, newItem: Int): Boolean {
        return oldItem == newItem
    }
}