package com.tuya.appbizsdk.scenebiz.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.thingclips.smart.scene.model.constant.SceneType
import com.tuya.appbizsdk.scenebiz.databinding.SceneListItemBinding

class SceneListAdapter constructor(
    private val onSceneListener: (String) -> Unit
) : ListAdapter<SceneItemData, RecyclerView.ViewHolder>(SceneListDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = SceneListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SceneListViewHolder(binding, onSceneListener)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val sceneItemData = getItem(position)
        (holder as SceneListViewHolder).binding(sceneItemData, itemCount)
    }
}

class SceneListViewHolder(
    private val binding: SceneListItemBinding,
    private val onSceneListener: (String) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    fun binding(sceneItemData: SceneItemData, itemCount: Int) {
        sceneItemData.run {
            binding.root.setOnClickListener {
                onSceneListener(id)
            }
            binding.cellSceneList.apply {
                text = name
            }
            binding.sceneBadge.apply {
                this.text = if (sceneItemData.sceneType == SceneType.SCENE_TYPE_AUTOMATION) {
                    "A"
                } else {
                    "M"
                }
            }
        }
    }
}


object SceneListDiff : DiffUtil.ItemCallback<SceneItemData>() {
    override fun areItemsTheSame(oldItem: SceneItemData, newItem: SceneItemData): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SceneItemData, newItem: SceneItemData): Boolean {
        return oldItem.name == newItem.name && oldItem.sceneType == newItem.sceneType
    }
}

data class SceneItemData(val id: String, val sceneType: SceneType, val name: String)