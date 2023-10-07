package com.thing.appbizsdk.familybiz.roomdev

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thing.appbizsdk.familybiz.R
import com.thingclips.smart.family.bean.DeviceInRoomBean

class RoomDevSettingAdapter(val mContext: Context?, var mRoomName: String? = null) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    val TYPE_NAME = 0x1
    val TYPE_TEXT = 0x2
    val TYPE_IN_ROOM_LIST = 0x3
    val TYPE_OUT_ROOM_LIST = 0x4
    var mInRoomDevList: ArrayList<DeviceInRoomBean> = ArrayList()
    var mOutRoomDevList: ArrayList<DeviceInRoomBean> = ArrayList()

    var mListener: OnAddRemoveListener? = null


    fun notifyDataChange(
        inRoom: ArrayList<DeviceInRoomBean>?,
        outRoom: ArrayList<DeviceInRoomBean>?
    ) {
        inRoom?.let {
            mInRoomDevList = it
        }
        outRoom?.let {
            mOutRoomDevList = it
        }
        super.notifyDataSetChanged()
    }

    fun updateName(name: String?) {
        mRoomName = name
        super.notifyItemChanged(0)
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        val item = mInRoomDevList?.removeAt(fromPosition - 1)
        if (item != null) {
            mInRoomDevList?.add(toPosition - 1, item)
        }
        notifyItemMoved(fromPosition, toPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_NAME -> NameViewHolder(
                View.inflate(
                    mContext,
                    R.layout.family_recycle_item_room_dev_setting_name,
                    null
                )
            )
            TYPE_IN_ROOM_LIST -> RoomViewHolder(
                View.inflate(
                    mContext,
                    R.layout.family_recycle_item_in_room_dev,
                    null
                )
            )
            TYPE_TEXT -> TextViewHolder(
                View.inflate(
                    mContext,
                    R.layout.family_recycle_item_dev_in_room_text,
                    null
                )
            )
            TYPE_OUT_ROOM_LIST -> OutRoomViewHolder(
                View.inflate(
                    mContext,
                    R.layout.family_recycle_item_dev_out_in_room,
                    null
                )
            )
            else -> OutRoomViewHolder(
                View.inflate(
                    mContext,
                    R.layout.family_recycle_item_dev_out_in_room,
                    null
                )
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val itemViewType: Int = getItemViewType(position)
        when (itemViewType) {
            TYPE_NAME -> {
                val nameViewHolder: NameViewHolder = holder as NameViewHolder
                nameViewHolder.tv.text = mRoomName
                nameViewHolder.ll.setOnClickListener(View.OnClickListener {
                    if (null != mListener) {
                        mListener?.onUpdateName()
                    }
                })
            }
            TYPE_TEXT -> {}
            TYPE_IN_ROOM_LIST -> {
                val devBean = mInRoomDevList!![position - 1]
                val roomViewHolder: RoomViewHolder = holder as RoomViewHolder
                roomViewHolder.tv_device.setText(devBean.name)
                roomViewHolder.iv_remove.setOnClickListener(View.OnClickListener {
                    if (null != mListener) {
                        mListener?.onRemove(holder.adapterPosition)
                    }
                })
                val roomName = devBean.roomName
                if (TextUtils.isEmpty(roomName) || TextUtils.equals(roomName, mRoomName)) {
                    roomViewHolder.tv_room.visibility = View.GONE
                } else {
                    roomViewHolder.tv_room.visibility = View.VISIBLE
                    roomViewHolder.tv_room.text =
                        mContext!!.getString(R.string.thing_will_remove, roomName)
                }
                if (!TextUtils.isEmpty(devBean.iconUrl)) {
                    roomViewHolder.iv_device.setImageURI(Uri.parse(devBean.iconUrl))
                }
            }
            TYPE_OUT_ROOM_LIST -> {
                val outBean = mOutRoomDevList!![position - mInRoomDevList!!.size - 2]
                val outRoomViewHolder: OutRoomViewHolder = holder as OutRoomViewHolder
                outRoomViewHolder.tv_device.text = outBean.name
                outRoomViewHolder.iv_add.setOnClickListener(View.OnClickListener {
                    mListener?.onAdd(holder.adapterPosition)
                })
                if (!TextUtils.isEmpty(outBean.roomName)) {
                    outRoomViewHolder.tv_belong_room.visibility = View.VISIBLE
                    outRoomViewHolder.tv_belong_room.text = outBean.roomName
                } else {
                    outRoomViewHolder.tv_belong_room.visibility = View.GONE
                }
                if (!TextUtils.isEmpty(outBean.iconUrl)) {
                    outRoomViewHolder.iv_device.setImageURI(Uri.parse(outBean.iconUrl))
                }
            }
            else -> {}
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            TYPE_NAME
        } else if (position <= mInRoomDevList!!.size) {
            TYPE_IN_ROOM_LIST
        } else if (position == mInRoomDevList!!.size + 1) {
            TYPE_TEXT
        } else {
            TYPE_OUT_ROOM_LIST
        }
    }

    override fun getItemCount(): Int {
        var init = 2
        if (null != mInRoomDevList) {
            init += mInRoomDevList!!.size
        }
        if (null != mOutRoomDevList) {
            init += mOutRoomDevList!!.size
        }
        return init
    }


    interface OnAddRemoveListener {
        fun onRemove(position: Int)
        fun onAdd(position: Int)
        fun onUpdateName()
    }

    fun setOnAddRemoveListener(listener: OnAddRemoveListener?) {
        mListener = listener
    }

    class NameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tv: TextView
        var ll: LinearLayout

        init {
            tv = itemView.findViewById<View>(R.id.tv_room_name) as TextView
            ll = itemView.findViewById<View>(R.id.ll_update_room_name) as LinearLayout
        }
    }

    class TextViewHolder(itemView: View?) : RecyclerView.ViewHolder(
        itemView!!
    )

    class RoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var iv_remove: ImageView
        var iv_device: ImageView
        var rl_touch: View
        var tv_device: TextView
        var tv_room: TextView

        init {
            iv_remove = itemView.findViewById<View>(R.id.iv_remove) as ImageView
            iv_device = itemView.findViewById<View>(R.id.iv_device) as ImageView
            tv_device = itemView.findViewById<View>(R.id.tv_device) as TextView
            tv_room = itemView.findViewById<View>(R.id.tv_room) as TextView
            rl_touch = itemView.findViewById(R.id.rl_touch)
        }
    }

    class OutRoomViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var iv_add: ImageView
        var iv_device: ImageView
        var tv_device: TextView
        var tv_belong_room: TextView

        init {
            iv_add = itemView.findViewById<View>(R.id.iv_add) as ImageView
            iv_device = itemView.findViewById<View>(R.id.iv_device) as ImageView
            tv_device = itemView.findViewById<View>(R.id.tv_device) as TextView
            tv_belong_room = itemView.findViewById<View>(R.id.tv_belong_room) as TextView
        }
    }

}