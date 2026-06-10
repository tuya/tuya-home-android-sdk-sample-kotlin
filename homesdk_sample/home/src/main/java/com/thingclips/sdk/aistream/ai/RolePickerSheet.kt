package com.thingclips.sdk.aistream.ai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tuya.appsdk.sample.user.R

/**
 * Bottom sheet listing role templates (bindRoleType=1) and custom roles (bindRoleType=0).
 * onPick(bindRoleType, roleId) fires on selection.
 */
class RolePickerSheet(
    private val devId: String,
    private val onPick: (bindRoleType: Int, roleId: String) -> Unit
) : BottomSheetDialogFragment() {

    private data class Row(val bindRoleType: Int, val roleId: String, val name: String, val desc: String?, val img: String?)

    private val agent = AiAgentManager(devId)
    private val rows = mutableListOf<Row>()
    private lateinit var adapter: RowAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, saved: Bundle?): View {
        val view = inflater.inflate(R.layout.ai_bottomsheet_roles, container, false)
        val rv = view.findViewById<RecyclerView>(R.id.rv_roles)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = RowAdapter()
        rv.adapter = adapter
        loadRoles()
        return view
    }

    private fun loadRoles() {
        agent.listRoleTemplates(null, object : Cb<ArrayList<RoleTemplate>> {
            override fun onOk(data: ArrayList<RoleTemplate>?) {
                data?.forEach {
                    val id = it.roleId ?: it.templateId ?: return@forEach
                    rows.add(Row(BindRoleType.TEMPLATE, id, "[Template] ${it.roleName ?: id}", it.roleIntroduce ?: it.roleDesc, it.roleImgUrl))
                }
                adapter.notifyDataSetChanged()
            }

            override fun onErr(code: Int, msg: String?) {}
        })
        agent.pageCustomRoles(1, 50, null, object : Cb<ArrayList<RoleSummary>> {
            override fun onOk(data: ArrayList<RoleSummary>?) {
                data?.forEach {
                    val id = it.roleId ?: return@forEach
                    rows.add(Row(BindRoleType.CUSTOM, id, "[Custom] ${it.roleName ?: id}", it.roleIntroduce ?: it.roleDesc, it.roleImgUrl))
                }
                adapter.notifyDataSetChanged()
            }

            override fun onErr(code: Int, msg: String?) {}
        })
    }

    private inner class RowAdapter : RecyclerView.Adapter<RowVH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowVH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.ai_item_role, parent, false)
            return RowVH(v)
        }

        override fun getItemCount() = rows.size

        override fun onBindViewHolder(holder: RowVH, position: Int) {
            val row = rows[position]
            holder.name.text = row.name
            holder.desc.text = row.desc ?: ""
            if (!row.img.isNullOrEmpty()) {
                Glide.with(holder.avatar).load(row.img).into(holder.avatar)
            } else {
                holder.avatar.setImageResource(android.R.drawable.sym_def_app_icon)
            }
            holder.itemView.setOnClickListener {
                onPick(row.bindRoleType, row.roleId)
                dismiss()
            }
        }
    }

    private inner class RowVH(v: View) : RecyclerView.ViewHolder(v) {
        val avatar: ImageView = v.findViewById(R.id.iv_role_avatar)
        val name: TextView = v.findViewById(R.id.tv_role_name)
        val desc: TextView = v.findViewById(R.id.tv_role_desc)
    }
}
