package com.tuya.appbizsdk.scenebiz.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuya.appbizsdk.scenebiz.databinding.ConditionTypeListItemBinding

class DetailConditionTypeAdapter constructor(private val onConditionTypeListener: ((Int) -> Unit)? = null) :
    ListAdapter<ConditionTypeItemData, RecyclerView.ViewHolder>(ConditionTypeListDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ConditionTypeListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SceneConditionTypeListViewHolder(binding, onConditionTypeListener)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val conditionTypeItemData = getItem(position)
        (holder as SceneConditionTypeListViewHolder).binding(conditionTypeItemData, itemCount)
    }
}

class SceneConditionTypeListViewHolder(
    private val binding: ConditionTypeListItemBinding,
    private val onConditionTypeListener: ((Int) -> Unit)? = null
) : RecyclerView.ViewHolder(binding.root) {
    fun binding(conditionItemData: ConditionTypeItemData, itemCount: Int) {
        conditionItemData.run {
            binding.root.setOnClickListener {
                onConditionTypeListener?.invoke(entityType)
            }
            binding.ivConditionType.apply {
                setImageResource(icon)
            }
            binding.cellConditionTypeInfo.apply {
                text = name
            }
        }
    }
}


object ConditionTypeListDiff : DiffUtil.ItemCallback<ConditionTypeItemData>() {
    override fun areItemsTheSame(oldItem: ConditionTypeItemData, newItem: ConditionTypeItemData): Boolean {
        return oldItem.entityType == newItem.entityType
    }

    override fun areContentsTheSame(oldItem: ConditionTypeItemData, newItem: ConditionTypeItemData): Boolean {
        return oldItem.icon == newItem.icon && oldItem.name == newItem.name
    }
}

data class ConditionTypeItemData(val entityType: Int, @androidx.annotation.DrawableRes val icon: Int, val name: String)