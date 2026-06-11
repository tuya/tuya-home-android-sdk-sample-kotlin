package com.thingclips.sdk.aistream.ai

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tuya.appsdk.sample.user.R

/**
 * Create / edit a custom agent role, mirroring the official app's 创建新角色
 * page: avatar picker (cloud default avatars in a bottom sheet grid), name
 * row, description card with a 3000-char counter, language and timbre
 * pickers. Pass "devId" (required) and optional "roleId" (edit mode).
 */
class RoleEditActivity : AppCompatActivity() {

    private lateinit var agent: AiAgentManager
    private lateinit var devId: String
    private var roleId: String? = null

    private lateinit var ivAvatarPreview: ImageView
    private lateinit var etName: EditText
    private lateinit var etIntroduce: EditText
    private lateinit var tvCounter: TextView
    private lateinit var tvLanguageValue: TextView
    private lateinit var tvTimbreValue: TextView

    private val avatars = mutableListOf<Avatar>()
    private val languages = mutableListOf<Language>()
    private val timbres = mutableListOf<Timbre>()

    private var selectedAvatarUrl: String? = null
    private var selectedLangIndex: Int = -1
    private var selectedTimbreIndex: Int = -1 // -1 = none
    private var existingRoleDesc: String? = null // preserved on update, not edited here

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ai_activity_role_edit)
        devId = intent.getStringExtra("devId") ?: ""
        roleId = intent.getStringExtra("roleId")
        if (devId.isEmpty()) {
            Toast.makeText(this, "devId required", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        agent = AiAgentManager(devId)

        findViewById<ImageView>(R.id.iv_back).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tv_title).setText(
            if (roleId == null) R.string.ai_role_create_title else R.string.ai_role_edit_title
        )

        ivAvatarPreview = findViewById(R.id.iv_avatar_preview)
        etName = findViewById(R.id.et_role_name)
        etIntroduce = findViewById(R.id.et_role_introduce)
        tvCounter = findViewById(R.id.tv_desc_counter)
        tvLanguageValue = findViewById(R.id.tv_language_value)
        tvTimbreValue = findViewById(R.id.tv_timbre_value)

        findViewById<View>(R.id.row_avatar).setOnClickListener { showAvatarSheet() }
        findViewById<View>(R.id.row_language).setOnClickListener { showLanguagePicker() }
        findViewById<View>(R.id.row_timbre).setOnClickListener { showTimbrePicker() }

        val btnSubmit = findViewById<TextView>(R.id.btn_submit)
        btnSubmit.setText(if (roleId == null) R.string.ai_create_role else R.string.ai_role_save)
        btnSubmit.setOnClickListener { submit() }

        val btnDelete = findViewById<TextView>(R.id.btn_delete)
        if (roleId != null) {
            btnDelete.visibility = View.VISIBLE
            btnDelete.setOnClickListener { confirmDelete() }
        }

        etIntroduce.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}

            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(s: Editable?) {
                tvCounter.text = "${s?.length ?: 0}/3000"
            }
        })

        loadAvatars()
        loadLanguages()
        loadTimbres()
        if (roleId != null) loadDetail()
    }

    // --- option data ---
    private fun loadAvatars() {
        agent.listAvatars(object : Cb<ArrayList<Avatar>> {
            override fun onOk(data: ArrayList<Avatar>?) {
                avatars.clear()
                data?.let { avatars.addAll(it) }
                // Edit mode may resolve the avatar before options arrive.
                runOnUiThread { renderAvatarPreview() }
            }

            override fun onErr(code: Int, msg: String?) {
                showError("avatars", msg)
            }
        })
    }

    private fun loadLanguages() {
        agent.listLanguages(object : Cb<ArrayList<Language>> {
            override fun onOk(data: ArrayList<Language>?) {
                if (data.isNullOrEmpty()) {
                    loadLanguagesFromTemplates()
                    return
                }
                languages.clear()
                languages.addAll(data)
                applyDefaultLanguage()
            }

            // list-support-languages may 500 on backend; fall back to languages
            // advertised by role templates so role creation stays usable.
            override fun onErr(code: Int, msg: String?) {
                loadLanguagesFromTemplates()
            }
        })
    }

    private fun loadLanguagesFromTemplates() {
        agent.listRoleTemplates(null, object : Cb<ArrayList<RoleTemplate>> {
            override fun onOk(data: ArrayList<RoleTemplate>?) {
                val seen = LinkedHashMap<String, String>()
                data?.forEach { t ->
                    val code = t.useLangCode
                    if (!code.isNullOrEmpty() && !seen.containsKey(code)) {
                        seen[code] = t.useLangName ?: code
                    }
                }
                languages.clear()
                seen.forEach { (code, name) ->
                    languages.add(Language(langCode = code, langName = name))
                }
                applyDefaultLanguage()
                if (languages.isEmpty()) {
                    showError("languages", "no fallback languages from templates")
                }
            }

            override fun onErr(code: Int, msg: String?) {
                showError("languages", msg)
            }
        })
    }

    private fun applyDefaultLanguage() {
        if (selectedLangIndex < 0) {
            val def = languages.indexOfFirst { it.hasDefault }
            if (def >= 0) selectedLangIndex = def
        }
        runOnUiThread { renderLanguageValue() }
    }

    private fun loadTimbres() {
        agent.timbrePage(1, 50, null, null, null, object : Cb<ArrayList<Timbre>> {
            override fun onOk(data: ArrayList<Timbre>?) {
                timbres.clear()
                data?.let { timbres.addAll(it) }
                runOnUiThread { renderTimbreValue() }
            }

            override fun onErr(code: Int, msg: String?) {
                showError("timbres", msg)
            }
        })
    }

    private fun loadDetail() {
        agent.customRoleDetail(roleId!!, object : Cb<RoleDetail> {
            override fun onOk(d: RoleDetail?) {
                d ?: return
                existingRoleDesc = d.roleDesc
                runOnUiThread {
                    etName.setText(d.roleName ?: "")
                    etIntroduce.setText(d.roleIntroduce ?: "")
                    selectedAvatarUrl = d.roleImgUrl
                    renderAvatarPreview()
                    languages.indexOfFirst { it.langCode == d.useLangCode }
                        .takeIf { it >= 0 }?.let { selectedLangIndex = it }
                    if (selectedLangIndex < 0 && !d.useLangCode.isNullOrEmpty()) {
                        // Keep the role's language even if options haven't loaded.
                        languages.add(Language(d.useLangCode, d.useLangName ?: d.useLangCode))
                        selectedLangIndex = languages.size - 1
                    }
                    renderLanguageValue()
                    timbres.indexOfFirst { it.voiceId == d.useTimbreId }
                        .takeIf { it >= 0 }?.let { selectedTimbreIndex = it }
                    if (selectedTimbreIndex < 0 && !d.useTimbreName.isNullOrEmpty()) {
                        tvTimbreValue.text = d.useTimbreName
                    }
                    renderTimbreValue()
                }
            }

            override fun onErr(code: Int, msg: String?) {
                showError("detail", msg)
            }
        })
    }

    // --- render ---
    private fun renderAvatarPreview() {
        val url = selectedAvatarUrl
        if (!url.isNullOrEmpty()) {
            Glide.with(this).load(url).circleCrop().into(ivAvatarPreview)
        }
    }

    private fun renderLanguageValue() {
        val lang = languages.getOrNull(selectedLangIndex)
        tvLanguageValue.text =
            lang?.langName ?: lang?.langCode ?: getString(R.string.ai_role_select_hint)
    }

    private fun renderTimbreValue() {
        val t = timbres.getOrNull(selectedTimbreIndex)
        if (t != null) tvTimbreValue.text = t.voiceName ?: t.voiceId ?: ""
        else if (selectedTimbreIndex < 0 && tvTimbreValue.text.isNullOrEmpty()) {
            tvTimbreValue.setText(R.string.ai_role_timbre_none)
        }
    }

    // --- pickers ---
    private fun showAvatarSheet() {
        if (avatars.isEmpty()) {
            Toast.makeText(this, "Avatars not loaded yet", Toast.LENGTH_SHORT).show()
            return
        }
        val sheet = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.ai_sheet_avatars, null)
        val rv = view.findViewById<RecyclerView>(R.id.rv_avatars)
        rv.layoutManager = GridLayoutManager(this, 4)
        rv.adapter = object : RecyclerView.Adapter<AvatarVH>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvatarVH {
                val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.ai_item_avatar, parent, false)
                return AvatarVH(v)
            }

            override fun getItemCount() = avatars.size

            override fun onBindViewHolder(holder: AvatarVH, position: Int) {
                val url = avatars[position].url
                if (!url.isNullOrEmpty()) {
                    Glide.with(holder.image).load(url).circleCrop().into(holder.image)
                }
                holder.image.setOnClickListener {
                    selectedAvatarUrl = url
                    renderAvatarPreview()
                    sheet.dismiss()
                }
            }
        }
        sheet.setContentView(view)
        sheet.show()
    }

    private class AvatarVH(v: View) : RecyclerView.ViewHolder(v) {
        val image: ImageView = v.findViewById(R.id.iv_avatar)
    }

    private fun showLanguagePicker() {
        if (languages.isEmpty()) {
            Toast.makeText(this, "Languages not loaded yet", Toast.LENGTH_SHORT).show()
            return
        }
        val names = languages.map { it.langName ?: it.langCode ?: "" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.ai_role_language)
            .setSingleChoiceItems(names, selectedLangIndex) { dialog, which ->
                selectedLangIndex = which
                renderLanguageValue()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.ai_action_cancel, null)
            .show()
    }

    private fun showTimbrePicker() {
        if (timbres.isEmpty()) {
            Toast.makeText(this, "Timbres not loaded yet", Toast.LENGTH_SHORT).show()
            return
        }
        val names = (listOf(getString(R.string.ai_role_timbre_none)) +
            timbres.map { it.voiceName ?: it.voiceId ?: "" }).toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.ai_role_timbre)
            .setSingleChoiceItems(names, selectedTimbreIndex + 1) { dialog, which ->
                selectedTimbreIndex = which - 1
                if (selectedTimbreIndex < 0) {
                    tvTimbreValue.setText(R.string.ai_role_timbre_none)
                } else {
                    renderTimbreValue()
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.ai_action_cancel, null)
            .show()
    }

    // --- submit / delete ---
    private fun submit() {
        val name = etName.text.toString().trim()
        val introduce = etIntroduce.text.toString().trim()
        val avatarUrl = selectedAvatarUrl
        val langCode = languages.getOrNull(selectedLangIndex)?.langCode
        if (name.isEmpty() || introduce.isEmpty() ||
            avatarUrl.isNullOrEmpty() || langCode.isNullOrEmpty()
        ) {
            Toast.makeText(this, R.string.ai_role_required_hint, Toast.LENGTH_SHORT).show()
            return
        }
        val timbreId = timbres.getOrNull(selectedTimbreIndex)?.voiceId

        val current = roleId
        if (current == null) {
            agent.addCustomRole(
                name, introduce, avatarUrl, langCode, null, timbreId, null,
                object : Cb<String> {
                    override fun onOk(data: String?) {
                        Toast.makeText(this@RoleEditActivity, "Created", Toast.LENGTH_SHORT).show()
                        finish()
                    }

                    override fun onErr(code: Int, msg: String?) {
                        showError("create", msg)
                    }
                }
            )
        } else {
            agent.updateCustomRole(
                current, name, existingRoleDesc, introduce, avatarUrl, langCode,
                timbreId, null, false,
                object : Cb<Boolean> {
                    override fun onOk(data: Boolean?) {
                        Toast.makeText(this@RoleEditActivity, "Updated", Toast.LENGTH_SHORT).show()
                        finish()
                    }

                    override fun onErr(code: Int, msg: String?) {
                        showError("update", msg)
                    }
                }
            )
        }
    }

    private fun confirmDelete() {
        val current = roleId ?: return
        AlertDialog.Builder(this)
            .setMessage(R.string.ai_confirm_delete_role)
            .setNegativeButton(R.string.ai_action_cancel, null)
            .setPositiveButton(R.string.ai_action_delete) { _, _ ->
                agent.deleteCustomRole(current, object : Cb<Boolean> {
                    override fun onOk(data: Boolean?) {
                        Toast.makeText(this@RoleEditActivity, "Deleted", Toast.LENGTH_SHORT).show()
                        finish()
                    }

                    override fun onErr(code: Int, msg: String?) {
                        showError("delete", msg)
                    }
                })
            }
            .show()
    }

    private fun showError(what: String, msg: String?) {
        runOnUiThread {
            Toast.makeText(this, "$what failed: $msg", Toast.LENGTH_SHORT).show()
        }
    }
}
