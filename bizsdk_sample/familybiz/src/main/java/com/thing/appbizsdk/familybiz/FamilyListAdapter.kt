package com.thing.appbizsdk.familybiz

import android.content.Context
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thingclips.smart.family.bean.FamilyBean
import com.thingclips.smart.home.sdk.anntation.HomeStatus

class FamilyListAdapter(val mContext: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val TYPE_LIST = 0
    private val TYPE_FOOTER = 1
    private var mDataList: List<FamilyBean>? = arrayListOf()
    private var mListener: OnFamilyMenuItemClickListener? = null

    fun notifyDataSetChanged(dataList: List<FamilyBean>) {
        this.mDataList = dataList
        super.notifyDataSetChanged()
    }

    fun setOnFooterItemClickListener(listener: OnFamilyMenuItemClickListener) {
        mListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val holder: RecyclerView.ViewHolder
        when (viewType) {
            TYPE_LIST -> {
                return DataViewHolder(
                    View.inflate(
                        mContext,
                        R.layout.familylist_recycle_item_family,
                        null
                    )
                )
            }
            TYPE_FOOTER -> {
                return FooterViewHolder(
                    View.inflate(
                        mContext,
                        R.layout.familylist_recycle_item_footer,
                        null
                    )
                )
            }
            else -> holder = DataViewHolder(
                View.inflate(
                    mContext,
                    R.layout.familylist_recycle_item_family,
                    null
                )
            )
        }
        return holder
    }

    override fun getItemCount(): Int {
        return if (mDataList == null) 1 else mDataList!!.size + 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            TYPE_LIST -> {
                val viewHolder = holder as DataViewHolder
                val familyBean = mDataList!![position]
                if (familyBean.familyStatus == HomeStatus.WAITING) {
                    viewHolder.dealStatusTv.text = mContext.getText(R.string.home_wait_join)
                    viewHolder.dealStatusTv.visibility = View.VISIBLE
                } else {
                    viewHolder.dealStatusTv.text = ""
                    viewHolder.dealStatusTv.visibility = View.GONE
                }
                viewHolder.tv.text = familyBean.familyName
                viewHolder.itemView.setOnClickListener {
                    mListener?.onClickFamily(familyBean)
                }
            }
            TYPE_FOOTER -> {
                val addHolder = holder as FooterViewHolder
                addHolder.mTvCreateFamily.setOnClickListener {
                    mListener?.onAddFamily()
                }
                addHolder.mTvJoinFamily.visibility = View.VISIBLE
                addHolder.mTvJoinFamily.setOnClickListener {
                    mListener?.onJoinFamily()
                }
            }
            else -> {}
        }
    }


    override fun getItemViewType(position: Int): Int {
        return if (position == mDataList!!.size) {
            TYPE_FOOTER
        } else TYPE_LIST
    }


    internal class DataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tv: TextView
        var dealStatusTv: TextView
        var imvgosetting:ImageView
        init {
            tv = itemView.findViewById<View>(R.id.tv_family) as TextView
            dealStatusTv = itemView.findViewById(R.id.family_manage_dealstatus_tv)
            imvgosetting =  itemView.findViewById(R.id.imvgosetting)
            imvgosetting.visibility = View.VISIBLE
        }

    }


    internal class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mTvCreateFamily: TextView
        val mTvJoinFamily: TextView

        init {
            mTvCreateFamily = itemView.findViewById(R.id.tv_add_item)
            mTvJoinFamily = itemView.findViewById(R.id.tv_join_family)
        }
    }

    interface OnFamilyMenuItemClickListener {
        fun onClickFamily(home:FamilyBean)
        fun onAddFamily()
        fun onJoinFamily()
    }

}