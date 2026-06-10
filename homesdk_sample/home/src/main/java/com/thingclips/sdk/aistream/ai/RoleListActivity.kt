package com.thingclips.sdk.aistream.ai

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tuya.appsdk.sample.user.R

/**
 * Full-page role switcher mirroring the official app's "切换角色" page:
 * 推荐 tab = role templates (bindRoleType=1), 自定义 tab = custom roles
 * (bindRoleType=0). Picking a card returns {bindRoleType, roleId} via
 * setResult; the pencil edits a custom role and 创建角色 opens RoleEditActivity.
 */
class RoleListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DEV_ID = "devId"
        const val EXTRA_CURRENT_ROLE_ID = "currentRoleId"
        const val EXTRA_BIND_ROLE_TYPE = "bindRoleType"
        const val EXTRA_ROLE_ID = "roleId"
    }

    private data class Row(
        val bindRoleType: Int,
        val roleId: String,
        val name: String,
        val desc: String?,
        val img: String?,
        val langName: String?
    )

    private lateinit var agent: AiAgentManager
    private lateinit var devId: String
    private var currentRoleId: String = ""

    private lateinit var tabRecommend: TextView
    private lateinit var tabCustom: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: RowAdapter

    private val rows = mutableListOf<Row>()
    private var showingCustom = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ai_activity_role_list)
        devId = intent.getStringExtra(EXTRA_DEV_ID) ?: ""
        currentRoleId = intent.getStringExtra(EXTRA_CURRENT_ROLE_ID) ?: ""
        if (devId.isEmpty()) {
            Toast.makeText(this, "devId required", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        agent = AiAgentManager(devId)

        findViewById<ImageView>(R.id.iv_back).setOnClickListener { finish() }
        tabRecommend = findViewById(R.id.tab_recommend)
        tabCustom = findViewById(R.id.tab_custom)
        tvEmpty = findViewById(R.id.tv_empty)
        tabRecommend.setOnClickListener { selectTab(custom = false) }
        tabCustom.setOnClickListener { selectTab(custom = true) }
        findViewById<TextView>(R.id.btn_create_role).setOnClickListener {
            startActivity(Intent(this, RoleEditActivity::class.java).putExtra("devId", devId))
        }

        val rv = findViewById<RecyclerView>(R.id.rv_roles)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = RowAdapter()
        rv.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        // Reload on return from create/edit so new roles show up.
        loadRoles()
    }

    private fun selectTab(custom: Boolean) {
        if (showingCustom == custom) return
        showingCustom = custom
        applyTabStyle()
        loadRoles()
    }

    private fun applyTabStyle() {
        val white = ContextCompat.getColor(this, android.R.color.white)
        val dark = ContextCompat.getColor(this, R.color.ai_text_primary)
        tabRecommend.setBackgroundResource(
            if (!showingCustom) R.drawable.ai_bg_tab_selected else R.drawable.ai_bg_tab_normal
        )
        tabRecommend.setTextColor(if (!showingCustom) white else dark)
        tabCustom.setBackgroundResource(
            if (showingCustom) R.drawable.ai_bg_tab_selected else R.drawable.ai_bg_tab_normal
        )
        tabCustom.setTextColor(if (showingCustom) white else dark)
    }

    private fun loadRoles() {
        if (showingCustom) loadCustomRoles() else loadTemplates()
    }

    private fun loadTemplates() {
        agent.listRoleTemplates(null, object : Cb<ArrayList<RoleTemplate>> {
            override fun onOk(data: ArrayList<RoleTemplate>?) {
                if (showingCustom) return
                rows.clear()
                data?.forEach {
                    val id = it.roleId ?: it.templateId ?: return@forEach
                    rows.add(
                        Row(
                            BindRoleType.TEMPLATE, id, it.roleName ?: id,
                            it.roleIntroduce ?: it.roleDesc, it.roleImgUrl, it.useLangName
                        )
                    )
                }
                publishRows()
            }

            override fun onErr(code: Int, msg: String?) {
                toastErr("templates", msg)
            }
        })
    }

    private fun loadCustomRoles() {
        agent.pageCustomRoles(1, 50, null, object : Cb<ArrayList<RoleSummary>> {
            override fun onOk(data: ArrayList<RoleSummary>?) {
                if (!showingCustom) return
                rows.clear()
                data?.forEach {
                    val id = it.roleId ?: return@forEach
                    rows.add(
                        Row(
                            BindRoleType.CUSTOM, id, it.roleName ?: id,
                            it.roleIntroduce ?: it.roleDesc, it.roleImgUrl, it.useLangName
                        )
                    )
                }
                publishRows()
            }

            override fun onErr(code: Int, msg: String?) {
                toastErr("custom roles", msg)
            }
        })
    }

    private fun publishRows() {
        runOnUiThread {
            adapter.notifyDataSetChanged()
            tvEmpty.isVisible = rows.isEmpty()
        }
    }

    private fun toastErr(what: String, msg: String?) {
        runOnUiThread {
            Toast.makeText(this, "Load $what failed: $msg", Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickRole(row: Row) {
        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtra(EXTRA_BIND_ROLE_TYPE, row.bindRoleType)
                .putExtra(EXTRA_ROLE_ID, row.roleId)
        )
        finish()
    }

    private inner class RowAdapter : RecyclerView.Adapter<RowVH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowVH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.ai_item_role_card, parent, false)
            return RowVH(v)
        }

        override fun getItemCount() = rows.size

        override fun onBindViewHolder(holder: RowVH, position: Int) {
            val row = rows[position]
            holder.name.text = row.name
            holder.desc.text = row.desc ?: ""
            holder.desc.isVisible = !row.desc.isNullOrEmpty()
            holder.lang.text = row.langName ?: ""
            holder.lang.isVisible = !row.langName.isNullOrEmpty()
            if (!row.img.isNullOrEmpty()) {
                Glide.with(holder.avatar).load(row.img).circleCrop().into(holder.avatar)
            } else {
                holder.avatar.setImageResource(android.R.drawable.sym_def_app_icon)
            }
            holder.itemView.setBackgroundResource(
                if (row.roleId == currentRoleId) R.drawable.ai_bg_card_selected
                else R.drawable.ai_bg_card
            )
            holder.edit.isVisible = row.bindRoleType == BindRoleType.CUSTOM
            holder.edit.setOnClickListener {
                startActivity(
                    Intent(this@RoleListActivity, RoleEditActivity::class.java)
                        .putExtra("devId", devId)
                        .putExtra("roleId", row.roleId)
                )
            }
            holder.itemView.setOnClickListener { pickRole(row) }
        }
    }

    private inner class RowVH(v: View) : RecyclerView.ViewHolder(v) {
        val avatar: ImageView = v.findViewById(R.id.iv_role_avatar)
        val name: TextView = v.findViewById(R.id.tv_role_name)
        val desc: TextView = v.findViewById(R.id.tv_role_desc)
        val lang: TextView = v.findViewById(R.id.tv_role_lang)
        val edit: ImageView = v.findViewById(R.id.iv_role_edit)
    }
}
