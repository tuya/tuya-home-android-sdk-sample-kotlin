package com.tuya.appbizsdk.scenebiz.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuya.appbizsdk.scenebiz.databinding.ConditionListItemBinding

class DetailConditionAdapter constructor() : ListAdapter<ConditionItemData, RecyclerView.ViewHolder>(ConditionListDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ConditionListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SceneConditionListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val conditionItemData = getItem(position)
        (holder as SceneConditionListViewHolder).binding(conditionItemData, itemCount)
    }
}

class SceneConditionListViewHolder(
    private val binding: ConditionListItemBinding,
    private val onConditionListener: ((String?) -> Unit)? = null
) : RecyclerView.ViewHolder(binding.root) {
    fun binding(conditionItemData: ConditionItemData, itemCount: Int) {
        conditionItemData.run {
            binding.root.setOnClickListener {
                onConditionListener?.invoke(id)
            }
            binding.cellConditionList.apply {
                text = "$entityType - $exprStr"
            }
        }
    }
}


object ConditionListDiff : DiffUtil.ItemCallback<ConditionItemData>() {
    override fun areItemsTheSame(oldItem: ConditionItemData, newItem: ConditionItemData): Boolean {
        // 新建时无id
        return oldItem == newItem || (oldItem.id != null && oldItem.id == newItem.id)
    }

    override fun areContentsTheSame(oldItem: ConditionItemData, newItem: ConditionItemData): Boolean {
        return oldItem.entityType == newItem.entityType && oldItem.exprStr == newItem.exprStr
    }
}

data class ConditionItemData(val id: String?, val entityType: Int, val exprStr: String)