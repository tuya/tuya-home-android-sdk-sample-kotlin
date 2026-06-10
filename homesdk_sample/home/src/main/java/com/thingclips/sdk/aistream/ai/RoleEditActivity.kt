package com.thingclips.sdk.aistream.ai

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.thingclips.smart.android.network.Business
import com.thingclips.smart.android.network.http.BusinessResponse
import com.tuya.appsdk.sample.user.R

/**
 * Create or edit a custom agent role. Pass "devId" (required) and optional
 * "roleId" (edit mode). Avatar / language / timbre options come from cloud.
 */
class RoleEditActivity : AppCompatActivity() {

    private val business = AiAgentBusiness()
    private lateinit var devId: String
    private var roleId: String? = null

    private lateinit var etName: EditText
    private lateinit var etIntroduce: EditText
    private lateinit var etDesc: EditText
    private lateinit var spAvatar: Spinner
    private lateinit var spLanguage: Spinner
    private lateinit var spTimbre: Spinner

    private val avatars = mutableListOf<Avatar>()
    private val languages = mutableListOf<Language>()
    private val timbres = mutableListOf<Timbre>()

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
        title = if (roleId == null) "New Role" else "Edit Role"

        etName = findViewById(R.id.et_role_name)
        etIntroduce = findViewById(R.id.et_role_introduce)
        etDesc = findViewById(R.id.et_role_desc)
        spAvatar = findViewById(R.id.sp_avatar)
        spLanguage = findViewById(R.id.sp_language)
        spTimbre = findViewById(R.id.sp_timbre)

        findViewById<Button>(R.id.btn_submit).setOnClickListener { submit() }
        val btnDelete = findViewById<Button>(R.id.btn_delete)
        if (roleId != null) {
            btnDelete.visibility = Button.VISIBLE
            btnDelete.setOnClickListener { confirmDelete() }
        }

        loadAvatars()
        loadLanguages()
        loadTimbres()
        if (roleId != null) loadDetail()
    }

    private fun loadAvatars() {
        business.listAvatars(devId, object : Business.ResultListener<ArrayList<Avatar>> {
            override fun onSuccess(r: BusinessResponse?, result: ArrayList<Avatar>?, api: String?) {
                avatars.clear(); result?.let { avatars.addAll(it) }
                spAvatar.adapter = simpleAdapter(avatars.mapIndexed { i, a -> a.avatarId ?: "avatar $i" })
            }

            override fun onFailure(r: BusinessResponse?, result: ArrayList<Avatar>?, api: String?) {
                showError("avatars", r)
            }
        })
    }

    private fun loadLanguages() {
        business.listLanguages(devId, object : Business.ResultListener<ArrayList<Language>> {
            override fun onSuccess(r: BusinessResponse?, result: ArrayList<Language>?, api: String?) {
                languages.clear(); result?.let { languages.addAll(it) }
                spLanguage.adapter = simpleAdapter(languages.map { it.langName ?: it.langCode ?: "" })
                val def = languages.indexOfFirst { it.hasDefault }
                if (def >= 0) spLanguage.setSelection(def)
            }

            override fun onFailure(r: BusinessResponse?, result: ArrayList<Language>?, api: String?) {
                showError("languages", r)
            }
        })
    }

    private fun loadTimbres() {
        business.timbrePage(devId, 1, 50, null, null, null, object : Business.ResultListener<ArrayList<Timbre>> {
            override fun onSuccess(r: BusinessResponse?, result: ArrayList<Timbre>?, api: String?) {
                timbres.clear(); result?.let { timbres.addAll(it) }
                spTimbre.adapter = simpleAdapter(
                    listOf("(none)") + timbres.map { it.voiceName ?: it.voiceId ?: "" }
                )
            }

            override fun onFailure(r: BusinessResponse?, result: ArrayList<Timbre>?, api: String?) {
                showError("timbres", r)
            }
        })
    }

    private fun loadDetail() {
        business.customRoleDetail(devId, roleId!!, object : Business.ResultListener<RoleDetail> {
            override fun onSuccess(r: BusinessResponse?, d: RoleDetail?, api: String?) {
                d ?: return
                etName.setText(d.roleName ?: "")
                etIntroduce.setText(d.roleIntroduce ?: "")
                etDesc.setText(d.roleDesc ?: "")
                avatars.indexOfFirst { it.url == d.roleImgUrl }.takeIf { it >= 0 }?.let { spAvatar.setSelection(it) }
                languages.indexOfFirst { it.langCode == d.useLangCode }.takeIf { it >= 0 }?.let { spLanguage.setSelection(it) }
                timbres.indexOfFirst { it.voiceId == d.useTimbreId }.takeIf { it >= 0 }?.let { spTimbre.setSelection(it + 1) }
            }

            override fun onFailure(r: BusinessResponse?, d: RoleDetail?, api: String?) {
                showError("detail", r)
            }
        })
    }

    private fun submit() {
        val name = etName.text.toString().trim()
        val introduce = etIntroduce.text.toString().trim()
        val desc = etDesc.text.toString().trim().ifEmpty { null }
        if (name.isEmpty() || introduce.isEmpty()) {
            Toast.makeText(this, "Name and introduce are required", Toast.LENGTH_SHORT).show()
            return
        }
        val avatarUrl = avatars.getOrNull(spAvatar.selectedItemPosition)?.url
        val langCode = languages.getOrNull(spLanguage.selectedItemPosition)?.langCode
        if (avatarUrl.isNullOrEmpty() || langCode.isNullOrEmpty()) {
            Toast.makeText(this, "Avatar and language are required", Toast.LENGTH_SHORT).show()
            return
        }
        val timbreIdx = spTimbre.selectedItemPosition - 1
        val timbreId = timbres.getOrNull(timbreIdx)?.voiceId

        val current = roleId
        if (current == null) {
            business.addCustomRole(
                devId, name, introduce, avatarUrl, langCode, desc, timbreId, null,
                object : Business.ResultListener<String> {
                    override fun onSuccess(r: BusinessResponse?, result: String?, api: String?) {
                        Toast.makeText(this@RoleEditActivity, "Created: $result", Toast.LENGTH_SHORT).show()
                        finish()
                    }

                    override fun onFailure(r: BusinessResponse?, result: String?, api: String?) {
                        showError("create", r)
                    }
                }
            )
        } else {
            business.updateCustomRole(
                devId, current, name, desc, introduce, avatarUrl, langCode, timbreId, null, false,
                object : Business.ResultListener<Boolean> {
                    override fun onSuccess(r: BusinessResponse?, result: Boolean?, api: String?) {
                        Toast.makeText(this@RoleEditActivity, "Updated", Toast.LENGTH_SHORT).show()
                        finish()
                    }

                    override fun onFailure(r: BusinessResponse?, result: Boolean?, api: String?) {
                        showError("update", r)
                    }
                }
            )
        }
    }

    private fun confirmDelete() {
        val current = roleId ?: return
        AlertDialog.Builder(this)
            .setMessage("Delete this role?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                business.deleteCustomRole(devId, current, object : Business.ResultListener<Boolean> {
                    override fun onSuccess(r: BusinessResponse?, result: Boolean?, api: String?) {
                        Toast.makeText(this@RoleEditActivity, "Deleted", Toast.LENGTH_SHORT).show()
                        finish()
                    }

                    override fun onFailure(r: BusinessResponse?, result: Boolean?, api: String?) {
                        showError("delete", r)
                    }
                })
            }
            .show()
    }

    private fun simpleAdapter(items: List<String>): ArrayAdapter<String> =
        ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)

    private fun showError(what: String, r: BusinessResponse?) {
        Toast.makeText(this, "$what failed: ${r?.errorMsg}", Toast.LENGTH_SHORT).show()
    }
}
