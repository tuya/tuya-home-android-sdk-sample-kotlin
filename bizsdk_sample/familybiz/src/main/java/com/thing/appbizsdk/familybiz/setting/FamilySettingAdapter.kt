package com.thing.appbizsdk.familybiz.setting

import android.content.Context
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thing.appbizsdk.familybiz.R
import com.thingclips.sdk.core.PluginManager
import com.thingclips.smart.android.user.bean.User
import com.thingclips.smart.family.bean.FamilyBean
import com.thingclips.smart.family.bean.MemberBean
import com.thingclips.smart.home.sdk.anntation.MemberStatus
import com.thingclips.smart.interior.api.IThingUserPlugin

class FamilySettingAdapter(val mContext: Context?)  : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val TYPE_HEAD = 0
    private val TYPE_MEMBERS = 1
    private val TYPE_FOOT = 2

    private var members: List<MemberBean>? = ArrayList()
    private var mFamilyBean: FamilyBean? = null
    private var isAdmin = false
    private var showTransferButton = false
    private var isOwner = false
    private var currentUser: User? = null
    private val userPlugin = PluginManager.service(
        IThingUserPlugin::class.java
    )
    init {
        currentUser = userPlugin.userInstance.user
    }


    fun notifyDataSetChanged(members: List<MemberBean>?, isAdmin: Boolean) {
        this.members = members
        this.isAdmin = isAdmin
        super.notifyDataSetChanged()
    }

    fun setShowTransferButton(showTransferButton: Boolean) {
        this.showTransferButton = showTransferButton
    }

    fun setOwner(isOwner: Boolean) {
        this.isOwner = isOwner
    }

    fun updateHead(familyBean: FamilyBean?) {
        mFamilyBean  = familyBean
        notifyItemChanged(0)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val holder: RecyclerView.ViewHolder = when (viewType) {
            TYPE_HEAD -> HeadViewHolder(
                View.inflate(
                    mContext,
                    R.layout.family_recycle_item_setting_head,
                    null
                )
            )
            TYPE_MEMBERS -> MemberViewHolder(
                View.inflate(
                    mContext,
                    R.layout.family_recycle_item_member,
                    null
                )
            )
            TYPE_FOOT -> FootViewHolder(
                View.inflate(
                    mContext,
                    R.layout.family_recycle_item_setting_bottom,
                    null
                )
            )
            else -> MemberViewHolder(
                View.inflate(
                    mContext,
                    R.layout.family_recycle_item_member,
                    null
                )
            )
        }
        return holder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val itemViewType = getItemViewType(position)
        when (itemViewType) {
            TYPE_HEAD -> {
                val headViewHolder = holder as HeadViewHolder
                if (null != mFamilyBean) {
                    headViewHolder.family_name.text = mFamilyBean!!.familyName
                    val rooms = mFamilyBean!!.rooms
                    headViewHolder.roomsize.text =
                        mContext!!.getString(R.string._room, rooms?.size ?: 0)

                    headViewHolder.aiv_family_name.setOnClickListener { mHeadFootListener?.onFamilyNameClick() }
                    headViewHolder.aiv_room_manage.setOnClickListener { mHeadFootListener?.onRoomManageClick() }
                }
                if (!isAdmin) {
                    headViewHolder.imvset1.visibility = View.GONE
                    headViewHolder.imvset2.visibility = View.GONE
                } else {
                    headViewHolder.imvset1.visibility = View.VISIBLE
                    headViewHolder.imvset2.visibility = View.VISIBLE
                }

            }
            TYPE_MEMBERS -> {
                val memberViewHolder = holder as MemberViewHolder
                val memberBean = members!![position - 1]
                memberViewHolder.tv_member_name.text = memberBean.memberName
                //account text
                memberViewHolder.itemView.visibility = View.VISIBLE
                memberViewHolder.tv_member_account.setTextColor(mContext!!.getColor(R.color.text_color1))
                memberViewHolder.tv_admin.setTextColor(mContext!!.getColor(R.color.text_color1))
                when (memberBean.memberStatus) {
                    MemberStatus.ACCEPT -> if (TextUtils.isEmpty(memberBean.account)) {
                        memberViewHolder.tv_member_account.visibility = View.GONE
                    } else {
                        memberViewHolder.tv_member_account.visibility = View.VISIBLE
                        memberViewHolder.tv_member_account.text = memberBean.account
                    }
                    MemberStatus.REJECT -> memberViewHolder.itemView.visibility = View.GONE
                    MemberStatus.WAITING -> {
                        memberViewHolder.tv_member_account.visibility = View.VISIBLE
                        memberViewHolder.tv_member_account.text =
                            mContext!!.getString(R.string.home_wait_join)
                        if (memberBean.memberId == 0L && memberBean.invitationId > 0) {
                            memberViewHolder.tv_admin.setTextColor(mContext!!.getColor(R.color.orange_58))
                        }
                    }
                    MemberStatus.INVALID -> {
                        memberViewHolder.tv_member_account.visibility = View.VISIBLE
                        memberViewHolder.tv_member_account.setTextColor(
                            mContext!!.getColor(R.color.orange_58)
                        )
                        memberViewHolder.tv_member_account.text =
                            mContext!!.resources.getString(R.string.family_invitation_invalid)
                    }
                    else -> memberViewHolder.tv_member_account.visibility = View.GONE
                }
                memberViewHolder.tv_admin.text = getMemberRoleName(memberBean)
                memberViewHolder.rl_family_member.setOnClickListener {
                    if (null != mListener) {
                        mListener!!.onItemClick(memberBean)
                    }
                }
                memberViewHolder.rl_family_member.setOnLongClickListener {
                    if (null != mListener) {
                        mListener!!.onLongItemClick(memberBean)
                    }
                    true
                }
            }
            TYPE_FOOT -> {
                val footViewHolder = holder as FootViewHolder
                if (!isAdmin) {
                    footViewHolder.tv_add_member.visibility = View.GONE
                } else {
                    footViewHolder.tv_add_member.visibility = View.VISIBLE
                }
                footViewHolder.tv_leave_family.setOnClickListener {
                    if (null != mHeadFootListener) {
                        mHeadFootListener?.onLeaveFamilyClick()
                    }
                }
                footViewHolder.tv_invaition_member.setOnClickListener {
                    if (null != mHeadFootListener) {
                        mHeadFootListener?.onInvationMemberClick()
                    }
                }

                footViewHolder.tv_add_member.setOnClickListener {
                    if (null != mHeadFootListener) {
                        mHeadFootListener?.onAddMemberClick()
                    }
                }
                footViewHolder.tv_leave_family.setText(if (isOwner) R.string.family_remove else R.string.family_leave)
            }
            else -> {}
        }
    }

    private fun getMemberRoleName(memberBean: MemberBean): String? {
        return if (memberBean.memberId == 0L && memberBean.invitationId > 0) {
             getDatePeriod(mContext, memberBean.validTime)
        } else getMemberRoleName(memberBean.role)
    }

    private fun getMemberRoleName(role: Int): String? {
        return if (role == 1) {
            mContext!!.getString(R.string.familyrole_manager)
        } else if (role ==2) {
            mContext!!.getString(R.string.familyrole_owner)
        } else {
            ""
        }
    }
    fun getDatePeriod(context: Context?, validTime: Long): String? {
        var dateFormat = ""
        if (context != null && validTime > 0) {
            var days = validTime / 24
            val leftHours = validTime % 24
            if (days > 0) {
                days = days + if (leftHours > 0) 1 else 0
                dateFormat =
                    context.resources.getString(R.string.family_member_time_remain_days, days)
            } else {
                dateFormat =
                    context.resources.getString(R.string.family_member_time_remain_hours, validTime)
            }
        }
        return dateFormat
    }

    private var mListener: OnItemClickListener? = null
    private var mHeadFootListener: OnHeadFootClickListener? = null

    fun setOnItemClick(listener: OnItemClickListener?) {
        mListener = listener
    }


    fun setOnHeadFootClickListener(listener: OnHeadFootClickListener?) {
        mHeadFootListener = listener
    }


    override fun getItemCount(): Int {
        return if (null == members) 2 else members!!.size + 2
    }


    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            TYPE_HEAD
        } else if (position > 0 && members!!.size > 0 && position <= members!!.size) {
            TYPE_MEMBERS
        } else {
            TYPE_FOOT
        }
    }

    internal class HeadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var aiv_family_name: RelativeLayout
        var aiv_room_manage: RelativeLayout
        var family_name: TextView
        var roomsize: TextView
        var imvset1: ImageView
        var imvset2: ImageView

        init {
            aiv_family_name = itemView.findViewById(R.id.aiv_family_name)
            aiv_room_manage = itemView.findViewById(R.id.aiv_room_manage)
            family_name = itemView.findViewById(R.id.tv_family)
            roomsize = itemView.findViewById(R.id.tv_room)
            imvset1 = itemView.findViewById(R.id.imvset1)
            imvset2 = itemView.findViewById(R.id.imvset2)
        }
    }


    internal class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var tv_member_name: TextView
        var tv_member_account: TextView
        var rl_family_member: View
        var tv_admin: TextView

        init {
            tv_member_name = itemView.findViewById(R.id.member_name)
            tv_member_account = itemView.findViewById(R.id.member_account)
            rl_family_member = itemView.findViewById(R.id.rl_family_member)
            tv_admin = itemView.findViewById(R.id.tv_admin)
        }
    }

    internal class FootViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tv_leave_family: TextView
        var tv_add_member: TextView
        var tv_invaition_member: TextView

        init {
            tv_leave_family = itemView.findViewById(R.id.tv_leave_family)
            tv_add_member = itemView.findViewById(R.id.tv_add_member)
            tv_invaition_member = itemView.findViewById(R.id.tv_invaition_member)
//            rl_transfer_owner = itemView.findViewById(R.id.rl_transfer_family)
        }
    }
}

interface OnItemClickListener {
    fun onItemClick(memberBean: MemberBean?)
    fun onLongItemClick(memberBean: MemberBean?)
}

interface OnHeadFootClickListener {
    fun onFamilyNameClick()
    fun onRoomManageClick()
    fun onLeaveFamilyClick()
    fun onAddMemberClick()
    fun onInvationMemberClick()

}