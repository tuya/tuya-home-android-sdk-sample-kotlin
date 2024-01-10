package com.tuya.appbizsdk.scenebiz.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuya.appbizsdk.scenebiz.databinding.ActionTypeListItemBinding

class DetailActionTypeAdapter constructor(private val onActionTypeListener: ((String) -> Unit)? = null) :
    ListAdapter<ActionTypeItemData, RecyclerView.ViewHolder>(ActionTypeListDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ActionTypeListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SceneActionTypeListViewHolder(binding, onActionTypeListener)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val actionTypeItemData = getItem(position)
        (holder as SceneActionTypeListViewHolder).binding(actionTypeItemData, itemCount)
    }
}

class SceneActionTypeListViewHolder(
    private val binding: ActionTypeListItemBinding,
    private val onActionTypeListener: ((String) -> Unit)? = null
) : RecyclerView.ViewHolder(binding.root) {
    fun binding(actionItemData: ActionTypeItemData, itemCount: Int) {
        actionItemData.run {
            binding.root.setOnClickListener {
                onActionTypeListener?.invoke(actionType)
            }
            binding.ivActionType.apply {
                setImageResource(icon)
            }
            binding.cellActionTypeInfo.apply {
                text = name
            }
        }
    }
}


object ActionTypeListDiff : DiffUtil.ItemCallback<ActionTypeItemData>() {
    override fun areItemsTheSame(oldItem: ActionTypeItemData, newItem: ActionTypeItemData): Boolean {
        return oldItem.actionType == newItem.actionType
    }

    override fun areContentsTheSame(oldItem: ActionTypeItemData, newItem: ActionTypeItemData): Boolean {
        return oldItem.icon == newItem.icon && oldItem.name == newItem.name
    }
}

data class ActionTypeItemData(val actionType: String, @androidx.annotation.DrawableRes val icon: Int, val name: String)