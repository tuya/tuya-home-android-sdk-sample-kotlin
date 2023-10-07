package com.thing.appbizsdk.familybiz

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.thingclips.smart.family.bean.FamilyBean
import com.thingclips.smart.home.sdk.anntation.HomeStatus

class ChooseFamilyPopupWindow(
    context: Context,
    val familyList: List<FamilyBean>,
    val mCurrentFamilyId: Long,
    val listener:IChooseFamilyListener
) : PopupWindow() {
    init {
        val inflater = LayoutInflater.from(context)
        this.contentView = inflater.inflate(R.layout.layout_popupwindow_choose_family, null) //布局xml
        this.width = LinearLayout.LayoutParams.WRAP_CONTENT
        this.height = LinearLayout.LayoutParams.WRAP_CONTENT
        this.isOutsideTouchable = true
        this.isClippingEnabled = false
        val colorDrawable = ColorDrawable(Color.parseColor("#00000000"))
        this.setBackgroundDrawable(colorDrawable)
        val recyclerview = this.contentView.findViewById<RecyclerView>(R.id.recyclerview)
        recyclerview.layoutManager = LinearLayoutManager(context)
        recyclerview.adapter = ChooseFamilyListAdapter(context,familyList,mCurrentFamilyId,object :IChooseFamilyListener{
            override fun onChooseed(homeId: Long) {
                listener.onChooseed(homeId)
                dismiss()
            }
        })
    }

    fun show() {
        showAtLocation(contentView, Gravity.TOP or Gravity.LEFT, 20, 120)
    }

}
class ChooseFamilyListAdapter(val mContext: Context, val mDataList: List<FamilyBean>,val mCurrentFId: Long,val listener:IChooseFamilyListener) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return  DataViewHolder(
            View.inflate(
                mContext,
                R.layout.familylist_recycle_item_family,
                null
            )
        )
    }

    override fun getItemCount(): Int {
        return   mDataList!!.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

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
        if(familyBean.homeId == mCurrentFId){
            viewHolder.tv.setTextColor(mContext.getColor(R.color.color_accent))
            viewHolder.imv.visibility = View.VISIBLE
        }else{
            viewHolder.tv.setTextColor(mContext.getColor(R.color.black))
            viewHolder.imv.visibility = View.GONE
        }
        viewHolder.itemView.setOnClickListener {
            if (familyBean.familyStatus == HomeStatus.WAITING) {
                Toast.makeText(
                    mContext,
                    mContext.getText(R.string.home_wait_join),
                    Toast.LENGTH_LONG
                ).show()
            }else {
                listener.onChooseed(familyBean.homeId)
            }
        }
    }

    internal class DataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tv: TextView
        var dealStatusTv: TextView
        var imv:ImageView

        init {
            tv = itemView.findViewById(R.id.tv_family)
            dealStatusTv = itemView.findViewById(R.id.family_manage_dealstatus_tv)
            imv =  itemView.findViewById(R.id.imvfamily)

        }
    }
}
interface IChooseFamilyListener{
    fun onChooseed(homeId:Long)
}