package com.thingclips.sdk.aistream.ai

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.tuya.appsdk.sample.user.R

/**
 * "角色记忆" hub mirroring the official app: clear chat history, clear
 * context, and long-term memory entries (format memory / datasheet memory /
 * chat summary / clear all). Requires "devId", "roleId", "bindRoleType"
 * extras. Returns RESULT_OK with "historyCleared"=true after the chat
 * history is wiped so the chat page can refresh.
 */
class MemoryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_HISTORY_CLEARED = "historyCleared"
    }

    private lateinit var agent: AiAgentManager
    private lateinit var devId: String
    private lateinit var roleId: String
    private var bindRoleType: Int = BindRoleType.DEFAULT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ai_activity_memory)
        devId = intent.getStringExtra("devId") ?: ""
        roleId = intent.getStringExtra("roleId") ?: ""
        bindRoleType = intent.getIntExtra("bindRoleType", BindRoleType.DEFAULT)
        if (devId.isEmpty() || roleId.isEmpty()) {
            Toast.makeText(this, "devId/roleId required", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        agent = AiAgentManager(devId)

        findViewById<ImageView>(R.id.iv_back).setOnClickListener { finish() }
        findViewById<View>(R.id.row_clear_history).setOnClickListener {
            confirm(R.string.ai_confirm_clear_history) { clearHistory() }
        }
        findViewById<View>(R.id.row_clear_context).setOnClickListener {
            confirm(R.string.ai_confirm_clear_context) { clearContext() }
        }
        findViewById<View>(R.id.row_format_memory).setOnClickListener {
            openMemoryList(MemoryListActivity.MODE_FORMAT)
        }
        findViewById<View>(R.id.row_datasheet_memory).setOnClickListener {
            openMemoryList(MemoryListActivity.MODE_DATASHEET)
        }
        findViewById<View>(R.id.row_summary).setOnClickListener {
            startActivity(withRoleExtras(Intent(this, SummaryActivity::class.java)))
        }
        findViewById<View>(R.id.row_clear_longterm).setOnClickListener {
            confirm(R.string.ai_confirm_clear_longterm) { clearLongTermMemory() }
        }

        loadMemorySwitch()
    }

    private fun withRoleExtras(intent: Intent): Intent = intent
        .putExtra("devId", devId)
        .putExtra("roleId", roleId)
        .putExtra("bindRoleType", bindRoleType)

    private fun openMemoryList(mode: Int) {
        startActivity(
            withRoleExtras(Intent(this, MemoryListActivity::class.java))
                .putExtra(MemoryListActivity.EXTRA_MODE, mode)
        )
    }

    private fun loadMemorySwitch() {
        agent.getMemorySwitch(object : Cb<MemorySwitch> {
            override fun onOk(data: MemorySwitch?) {
                if (data?.memoryOpen == false) {
                    runOnUiThread {
                        findViewById<TextView>(R.id.tv_longterm_section).text =
                            getString(R.string.ai_memory_longterm_section) +
                                "（" + getString(R.string.ai_memory_switch_off) + "）"
                    }
                }
            }

            override fun onErr(code: Int, msg: String?) {}
        })
    }

    private fun confirm(messageRes: Int, action: () -> Unit) {
        AlertDialog.Builder(this)
            .setMessage(messageRes)
            .setNegativeButton(R.string.ai_action_cancel, null)
            .setPositiveButton(R.string.ai_action_confirm) { _, _ -> action() }
            .show()
    }

    private fun clearHistory() {
        agent.deleteHistory(bindRoleType, roleId, true, null, object : Cb<Boolean> {
            override fun onOk(data: Boolean?) {
                clearLocalHistory()
                runOnUiThread {
                    setResult(
                        Activity.RESULT_OK,
                        Intent().putExtra(EXTRA_HISTORY_CLEARED, true)
                    )
                }
                toast(getString(R.string.ai_memory_clear_history) + " OK")
            }

            override fun onErr(code: Int, msg: String?) {
                toast("Clear history failed: $msg")
            }
        })
    }

    private fun clearLocalHistory() {
        val uid = ThingHomeSdk.getUserInstance().user?.uid ?: "0"
        Thread {
            val helper = AiChatRecordDbHelper(applicationContext, uid)
            try {
                helper.deleteByRole(devId, roleId)
            } finally {
                helper.close()
            }
        }.start()
    }

    private fun clearContext() {
        agent.clearContext(bindRoleType, roleId, object : Cb<Boolean> {
            override fun onOk(data: Boolean?) {
                toast(getString(R.string.ai_memory_clear_context) + " OK")
            }

            override fun onErr(code: Int, msg: String?) {
                toast("Clear context failed: $msg")
            }
        })
    }

    private fun clearLongTermMemory() {
        agent.deleteMemory(bindRoleType, roleId, true, null, object : Cb<Boolean> {
            override fun onOk(data: Boolean?) {
                toast(getString(R.string.ai_memory_clear_longterm) + " OK")
            }

            override fun onErr(code: Int, msg: String?) {
                toast("Clear memory failed: $msg")
            }
        })
    }

    private fun toast(msg: String) {
        runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
    }
}
