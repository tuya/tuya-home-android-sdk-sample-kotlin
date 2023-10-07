package com.thing.appbizsdk.familybiz.room

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thing.appbizsdk.familybiz.R
import com.thingclips.smart.family.bean.TRoomBean

class RoomListAdapter(var items: MutableList<TRoomBean> = arrayListOf(), var roomItemClickListener:IRoomItemClickListener? = null) : RecyclerView.Adapter<RoomListAdapter.RoomListViewHolder>() {
    class RoomListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemText: TextView = itemView.findViewById(R.id.item_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
        return RoomListViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomListViewHolder, position: Int) {
        holder.itemText.text = items!![position].name
        holder.itemText.setOnClickListener {
            roomItemClickListener?.onClick(items!![position])
        }
    }

    fun updateItem(list: MutableList<TRoomBean>){
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    fun addItem(item: TRoomBean) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun removeItem(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        val item = items.removeAt(fromPosition)
        items.add(toPosition, item)
        notifyItemMoved(fromPosition, toPosition)
    }
}
interface IRoomItemClickListener{
    fun onClick(roomId: TRoomBean)
}