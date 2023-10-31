package com.tuya.appbizsdk.scenebiz.act

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox.STATE_CHECKED
import com.google.android.material.checkbox.MaterialCheckBox.STATE_UNCHECKED
import com.thingclips.smart.scene.model.constant.SceneType
import com.tuya.appbizsdk.scenebiz.databinding.ChooseLinkageListItemBinding

class LinkageChooseListAdapter constructor(
    private val onLinkageCheckListener: (String, SceneType, Boolean) -> Unit
) : ListAdapter<LinkageCheckItemData, RecyclerView.ViewHolder>(LinkageCheckListDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = ChooseLinkageListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LinkageChooseListViewHolder(binding, onLinkageCheckListener)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val linkageCheckItemData = getItem(position)
        (holder as LinkageChooseListViewHolder).binding(linkageCheckItemData, itemCount)
    }
}

class LinkageChooseListViewHolder(
    private val binding: ChooseLinkageListItemBinding,
    private val onLinkageCheckListener: (String, SceneType, Boolean) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    fun binding(linkageCheckItemData: LinkageCheckItemData, itemCount: Int) {
        linkageCheckItemData.run {
            binding.mchLinkage.checkedState = if (this.checked) {
                STATE_CHECKED
            } else {
                STATE_UNCHECKED
            }
            binding.mchLinkage.setOnCheckedChangeListener { _, isChecked ->
                onLinkageCheckListener(id, sceneType, isChecked)
            }
            binding.cellLinkageList.apply {
                text = name
            }
            binding.linkageBadge.apply {
                this.text = if (linkageCheckItemData.sceneType == SceneType.SCENE_TYPE_AUTOMATION) {
                    "A"
                } else {
                    "M"
                }
            }
        }
    }
}


object LinkageCheckListDiff : DiffUtil.ItemCallback<LinkageCheckItemData>() {
    override fun areItemsTheSame(oldItem: LinkageCheckItemData, newItem: LinkageCheckItemData): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: LinkageCheckItemData, newItem: LinkageCheckItemData): Boolean {
        return oldItem.name == newItem.name && oldItem.sceneType == newItem.sceneType && oldItem.checked == newItem.checked
    }
}

data class LinkageCheckItemData(val id: String, val sceneType: SceneType, val name: String, val checked: Boolean = false)