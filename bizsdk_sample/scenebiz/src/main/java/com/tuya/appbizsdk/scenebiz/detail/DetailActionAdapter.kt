package com.tuya.appbizsdk.scenebiz.detail

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuya.appbizsdk.scenebiz.databinding.ActionListItemBinding

class DetailActionAdapter constructor() : ListAdapter<ActionItemData, RecyclerView.ViewHolder>(ActionListDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ActionListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SceneActionListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val actionItemData = getItem(position)
        (holder as SceneActionListViewHolder).binding(actionItemData, itemCount)
    }
}

class SceneActionListViewHolder(
    private val binding: ActionListItemBinding,
    private val onActionListener: ((String?) -> Unit)? = null
) : RecyclerView.ViewHolder(binding.root) {
    fun binding(actionItemData: ActionItemData, itemCount: Int) {
        actionItemData.run {
            binding.root.setOnClickListener {
                onActionListener?.invoke(id)
            }
            binding.cellActionList.apply {
                text = "${actionExecutor} - ${executorPropertyStr}"
            }
        }
    }
}


object ActionListDiff : DiffUtil.ItemCallback<ActionItemData>() {
    override fun areItemsTheSame(oldItem: ActionItemData, newItem: ActionItemData): Boolean {
        // 新建时无id
        return oldItem == newItem || (oldItem.id != null && oldItem.id == newItem.id)
    }

    override fun areContentsTheSame(oldItem: ActionItemData, newItem: ActionItemData): Boolean {
        return oldItem.actionExecutor == newItem.actionExecutor && oldItem.executorPropertyStr == newItem.executorPropertyStr
    }
}

data class ActionItemData(val id: String?, val actionExecutor: String, val executorPropertyStr: String)