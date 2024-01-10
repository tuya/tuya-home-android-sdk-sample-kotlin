package com.tuya.appbizsdk.scenebiz.cond

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tuya.appbizsdk.scenebiz.databinding.WeatherListItemBinding

class WeatherListAdapter constructor(
    private val onWeatherListener: (Int, String) -> Unit
) : ListAdapter<WeatherItemData, RecyclerView.ViewHolder>(WeatherListDiff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = WeatherListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WeatherListViewHolder(binding, onWeatherListener)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val weatherItemData = getItem(position)
        (holder as WeatherListViewHolder).binding(weatherItemData, itemCount)
    }
}

class WeatherListViewHolder(
    private val binding: WeatherListItemBinding,
    private val onWeatherListener: (Int, String) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    fun binding(weatherItemData: WeatherItemData, itemCount: Int) {
        weatherItemData.run {
            with(binding) {
                root.setOnClickListener {
                    onWeatherListener(entityType, entitySubId)
                }
                cellWeatherList.apply {
                    text = weatherTypeName
                }
            }
        }
    }
}


object WeatherListDiff : DiffUtil.ItemCallback<WeatherItemData>() {
    override fun areItemsTheSame(oldItem: WeatherItemData, newItem: WeatherItemData): Boolean {
        return oldItem.entityType == newItem.entityType && oldItem.entitySubId == newItem.entitySubId
    }

    override fun areContentsTheSame(oldItem: WeatherItemData, newItem: WeatherItemData): Boolean {
        return oldItem.weatherTypeName == newItem.weatherTypeName
    }
}

data class WeatherItemData(val entityType: Int, val entitySubId: String, val weatherTypeName: String)