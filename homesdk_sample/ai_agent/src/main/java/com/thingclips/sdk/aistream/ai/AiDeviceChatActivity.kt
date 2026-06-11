package com.thingclips.sdk.aistream.ai

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.thingclips.sdk.aistream.R

/**
 * Device-identity chat. On top of the shared chat surface it owns everything
 * role-related — the role APIs are devId-scoped and only exist for this
 * identity: the expandable role header card, role switching/editing, role
 * memory, cloud chat history and the local role cache for instant first paint.
 * Requires the "devId" extra.
 */
class AiDeviceChatActivity : BaseAiChatActivity() {

    companion object {
        private const val REQUEST_SWITCH_ROLE = 203
        private const val REQUEST_MEMORY = 204

        private const val MENU_MEMORY = 303
        private const val MENU_TEST_ALL = 307

        private const val PREFS_ROLE_CACHE = "ai_role_cache"
    }

    override val identity: Int = AiIdentityConfig.IDENTITY_DEVICE
    override val layoutResId: Int = R.layout.activity_ai_chat

    // Role header card views
    private lateinit var llRoleExpanded: LinearLayout
    private lateinit var llRoleCollapsed: LinearLayout
    private lateinit var ivRoleAvatar: ImageView
    private lateinit var ivRoleAvatarSmall: ImageView
    private lateinit var tvRoleName: TextView
    private lateinit var tvRoleNameSmall: TextView
    private lateinit var tvChipTag: TextView
    private lateinit var tvChipLang: TextView
    private lateinit var tvChipTimbre: TextView
    private lateinit var btnEditRole: TextView
    private lateinit var btnSwitchRole: TextView

    private lateinit var agent: AiAgentManager
    private var currentBindRoleType: Int = BindRoleType.DEFAULT
    private var currentRoleDetail: RoleDetail? = null

    override fun initIdentity(): Boolean {
        mDevId = intent.getStringExtra("devId") ?: ""
        if (mDevId.isEmpty()) {
            Log.e(TAG, "devId is required for device-identity connection.")
            Toast.makeText(this, "devId is required", Toast.LENGTH_SHORT).show()
            finish()
            return false
        }
        agent = AiAgentManager(mDevId)
        return true
    }

    override fun setupIdentityUi() {
        tvEmoji = findViewById(R.id.tv_emoji)
        llRoleExpanded = findViewById(R.id.ll_role_expanded)
        llRoleCollapsed = findViewById(R.id.ll_role_collapsed)
        ivRoleAvatar = findViewById(R.id.iv_role_avatar)
        ivRoleAvatarSmall = findViewById(R.id.iv_role_avatar_small)
        tvRoleName = findViewById(R.id.tv_role_name)
        tvRoleNameSmall = findViewById(R.id.tv_role_name_small)
        tvChipTag = findViewById(R.id.tv_chip_tag)
        tvChipLang = findViewById(R.id.tv_chip_lang)
        tvChipTimbre = findViewById(R.id.tv_chip_timbre)
        btnEditRole = findViewById(R.id.btn_edit_role)
        btnSwitchRole = findViewById(R.id.btn_switch_role)

        findViewById<ImageView>(R.id.iv_more).setOnClickListener { showMoreMenu(it) }
        findViewById<ImageView>(R.id.iv_role_collapse).setOnClickListener {
            setRoleCardExpanded(false)
        }
        llRoleCollapsed.setOnClickListener { setRoleCardExpanded(true) }

        btnSwitchRole.setOnClickListener {
            if (!isSessionActive()) {
                showToast("Session not active")
                return@setOnClickListener
            }
            startActivityForResult(
                Intent(this, RoleListActivity::class.java)
                    .putExtra(RoleListActivity.EXTRA_DEV_ID, mDevId)
                    .putExtra(RoleListActivity.EXTRA_CURRENT_ROLE_ID, currentRoleId),
                REQUEST_SWITCH_ROLE
            )
        }
        btnEditRole.setOnClickListener {
            if (!requireRole()) return@setOnClickListener
            if (currentBindRoleType != BindRoleType.CUSTOM) {
                showToast("Only custom roles can be edited")
                return@setOnClickListener
            }
            startActivity(
                Intent(this, RoleEditActivity::class.java)
                    .putExtra("devId", mDevId)
                    .putExtra("roleId", currentRoleId)
            )
        }
    }

    // Render the cached role immediately; the network chain (connect ->
    // session -> getBindRole) only refreshes later.
    override fun preloadIdentityState() {
        loadCachedRole()
    }

    override fun onSessionEstablished() {
        resolveBoundRole()
    }

    private fun setRoleCardExpanded(expanded: Boolean) {
        llRoleExpanded.visibility = if (expanded) View.VISIBLE else View.GONE
        llRoleCollapsed.visibility = if (expanded) View.GONE else View.VISIBLE
    }

    // --- Role management ---
    private fun resolveBoundRole() {
        agent.initAgentRoleBinding(object : Cb<RoleDetail> {
            override fun onOk(data: RoleDetail?) {
                agent.getBindRole(object : Cb<RoleDetail> {
                    override fun onOk(bind: RoleDetail?) {
                        applyRole(bind ?: data)
                    }

                    override fun onErr(code: Int, msg: String?) {
                        applyRole(data)
                    }
                })
            }

            override fun onErr(code: Int, msg: String?) {
                Log.e(TAG, "initAgentRoleBinding failed: $code $msg")
            }
        })
    }

    private fun applyRole(detail: RoleDetail?) {
        if (detail?.roleId.isNullOrEmpty()) return
        // Same role as the one rendered from cache: refresh the header and
        // merge cloud history without clearing the visible conversation.
        val sameRole = detail!!.roleId == currentRoleId
        currentRoleId = detail.roleId!!
        currentBindRoleType = detail.bindRoleType
        currentRoleDetail = detail
        saveRoleCache(detail)
        runOnUiThread {
            updateRoleHeader(detail)
            if (!sameRole) clearConversationView()
        }
        if (!sameRole) loadLocalHistory()
        loadCloudHistory()
    }

    private fun updateRoleHeader(detail: RoleDetail) {
        val name = detail.roleName ?: detail.roleId ?: ""
        tvRoleName.text = name
        tvRoleNameSmall.text = name

        tvChipTag.setText(
            when (detail.bindRoleType) {
                BindRoleType.CUSTOM -> R.string.ai_role_tag_custom
                BindRoleType.TEMPLATE -> R.string.ai_role_tag_template
                else -> R.string.ai_role_tag_default
            }
        )

        val lang = detail.useLangName ?: detail.useLangCode
        tvChipLang.text = lang ?: ""
        tvChipLang.visibility = if (lang.isNullOrEmpty()) View.GONE else View.VISIBLE
        tvChipTimbre.text = detail.useTimbreName ?: ""
        tvChipTimbre.visibility =
            if (detail.useTimbreName.isNullOrEmpty()) View.GONE else View.VISIBLE

        if (!detail.roleImgUrl.isNullOrEmpty()) {
            Glide.with(this).load(detail.roleImgUrl)
                .transform(RoundedCorners(resources.displayMetrics.density.times(14).toInt()))
                .into(ivRoleAvatar)
            Glide.with(this).load(detail.roleImgUrl).circleCrop().into(ivRoleAvatarSmall)
        }

        btnEditRole.visibility =
            if (detail.bindRoleType == BindRoleType.CUSTOM) View.VISIBLE else View.GONE
    }

    private fun switchRole(bindRoleType: Int, roleId: String) {
        if (roleId.isEmpty()) return
        agent.bindRole(bindRoleType, roleId, object : Cb<Boolean> {
            override fun onOk(data: Boolean?) {
                showToast("Role switched")
                currentRoleId = roleId
                currentBindRoleType = bindRoleType
                // Re-fetch the bound role so the header card reflects the new role.
                agent.getBindRole(object : Cb<RoleDetail> {
                    override fun onOk(bind: RoleDetail?) {
                        if (bind?.roleId.isNullOrEmpty()) refreshAfterRoleChange()
                        else applyRole(bind)
                    }

                    override fun onErr(code: Int, msg: String?) {
                        refreshAfterRoleChange()
                    }
                })
            }

            override fun onErr(code: Int, msg: String?) {
                showToast("Switch role failed: $msg")
            }
        })
    }

    private fun refreshAfterRoleChange() {
        runOnUiThread {
            tvRoleName.text = currentRoleId
            tvRoleNameSmall.text = currentRoleId
            clearConversationView()
        }
        loadLocalHistory()
        loadCloudHistory()
    }

    // --- Role cache: render last known role/header before any network IO ---
    private fun saveRoleCache(detail: RoleDetail) {
        getSharedPreferences(PREFS_ROLE_CACHE, Context.MODE_PRIVATE).edit()
            .putString("$mDevId.roleId", detail.roleId)
            .putInt("$mDevId.bindRoleType", detail.bindRoleType)
            .putString("$mDevId.roleName", detail.roleName)
            .putString("$mDevId.roleImgUrl", detail.roleImgUrl)
            .putString("$mDevId.useLangName", detail.useLangName ?: detail.useLangCode)
            .putString("$mDevId.useTimbreName", detail.useTimbreName)
            .apply()
    }

    private fun loadCachedRole() {
        val sp = getSharedPreferences(PREFS_ROLE_CACHE, Context.MODE_PRIVATE)
        val roleId = sp.getString("$mDevId.roleId", null) ?: return
        currentRoleId = roleId
        currentBindRoleType = sp.getInt("$mDevId.bindRoleType", BindRoleType.DEFAULT)
        updateRoleHeader(
            RoleDetail(
                roleId = roleId,
                roleName = sp.getString("$mDevId.roleName", null),
                roleImgUrl = sp.getString("$mDevId.roleImgUrl", null),
                useLangName = sp.getString("$mDevId.useLangName", null),
                useTimbreName = sp.getString("$mDevId.useTimbreName", null),
                bindRoleType = currentBindRoleType
            )
        )
    }

    // --- Cloud history ---
    private fun loadCloudHistory() {
        if (currentRoleId == "default" || currentRoleId.isEmpty()) return
        agent.fetchHistory(
            currentBindRoleType, currentRoleId,
            null, System.currentTimeMillis(), 50, true,
            object : Cb<ArrayList<ChatHistoryItem>> {
                override fun onOk(data: ArrayList<ChatHistoryItem>?) {
                    if (data.isNullOrEmpty()) return
                    mergeCloudHistory(data)
                }

                override fun onErr(code: Int, msg: String?) {
                    Log.w(TAG, "fetchHistory failed: $code $msg")
                }
            }
        )
    }

    private fun mergeCloudHistory(items: List<ChatHistoryItem>) {
        val seenBizIds = messageList.mapNotNull { it.bizId }.toMutableSet()
        val toAdd = mutableListOf<ChatMessage>()
        for (item in items) {
            val key = item.requestId ?: item.gmtCreate.toString()
            if (seenBizIds.contains(key)) continue
            seenBizIds.add(key)
            item.question?.forEach { part ->
                part.context?.takeIf { it.isNotEmpty() }?.let {
                    toAdd.add(
                        ChatMessage(
                            text = it,
                            isSentByUser = true,
                            messageType = ChatMessage.MessageType.TEXT,
                            bizId = key
                        )
                    )
                }
            }
            item.answer?.forEach { part ->
                part.context?.takeIf { it.isNotEmpty() }?.let {
                    toAdd.add(
                        ChatMessage(
                            text = it,
                            isSentByUser = false,
                            messageType = ChatMessage.MessageType.NLG_TEXT,
                            bizId = key
                        )
                    )
                }
            }
        }
        if (toAdd.isEmpty()) return
        runOnUiThread {
            messageList.addAll(toAdd)
            chatAdapter.notifyDataSetChanged()
            rvChatMessages.scrollToPosition(messageList.size - 1)
        }
    }

    // --- "..." popup menu ---
    private fun showMoreMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, MENU_MEMORY, 0, getString(R.string.ai_menu_memory))
        popup.menu.add(0, MENU_TEST_ALL, 1, getString(R.string.ai_menu_diagnostics))
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_MEMORY -> {
                    if (!requireRole()) return@setOnMenuItemClickListener true
                    startActivityForResult(
                        Intent(this, MemoryActivity::class.java)
                            .putExtra("devId", mDevId)
                            .putExtra("roleId", currentRoleId)
                            .putExtra("bindRoleType", currentBindRoleType),
                        REQUEST_MEMORY
                    )
                    true
                }

                MENU_TEST_ALL -> {
                    if (!isSessionActive()) {
                        showToast("Session not active")
                        return@setOnMenuItemClickListener true
                    }
                    runDiagnostics()
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    private fun requireRole(): Boolean {
        if (!isSessionActive()) { showToast("Session not active"); return false }
        if (currentRoleId == "default" || currentRoleId.isEmpty()) {
            showToast("Role not resolved yet")
            return false
        }
        return true
    }

    private fun runDiagnostics() {
        showToast("Running API diagnostics...")
        AiAgentDiagnostics(this, mDevId, currentBindRoleType, currentRoleId).runAll { file ->
            runOnUiThread {
                val path = file?.absolutePath ?: "(write failed, see logcat tag ai_stream_Diag)"
                AlertDialog.Builder(this)
                    .setTitle("Diagnostics done")
                    .setMessage("Saved to:\n$path\n\nAlso logged to logcat (tag: ai_stream_Diag).")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    @Deprecated("This method is deprecated in favor of the Activity Result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SWITCH_ROLE && resultCode == RESULT_OK && data != null) {
            val bindRoleType =
                data.getIntExtra(RoleListActivity.EXTRA_BIND_ROLE_TYPE, BindRoleType.TEMPLATE)
            val roleId = data.getStringExtra(RoleListActivity.EXTRA_ROLE_ID)
            if (!roleId.isNullOrEmpty() && roleId != currentRoleId) {
                switchRole(bindRoleType, roleId)
            }
        }
        if (requestCode == REQUEST_MEMORY && resultCode == RESULT_OK &&
            data?.getBooleanExtra(MemoryActivity.EXTRA_HISTORY_CLEARED, false) == true
        ) {
            clearConversationView()
        }
    }
}
