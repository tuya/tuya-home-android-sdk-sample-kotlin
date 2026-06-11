package com.thingclips.sdk.aistream.ai.data

import android.content.Context
import android.util.Log
import com.thingclips.smart.android.network.Business
import com.thingclips.smart.android.network.http.BusinessResponse
import java.io.File

/**
 * One-shot diagnostic that exercises every read-only ATOP agent API and
 * records, per endpoint, the status / error code / error message / raw result
 * JSON / parsed bean. Results are written to a text file under the app's
 * external files dir so they can be pulled and shared.
 *
 * Write/destructive endpoints (role add/update/delete, bind, summary update,
 * memory/history delete, context clear) are intentionally NOT exercised here,
 * to avoid mutating cloud state during a diagnostic run.
 */
class AiAgentDiagnostics(
    private val context: Context,
    private val devId: String,
    private val bindRoleType: Int,
    private val roleId: String
) {

    private val biz = AiAgentBusiness()
    private val lines = StringBuilder()
    private val lock = Any()
    private var remaining = 0
    private var onDone: ((File?) -> Unit)? = null

    fun runAll(onDone: (File?) -> Unit) {
        this.onDone = onDone
        lines.append("AI Agent ATOP diagnostics\n")
        lines.append("devId=$devId bindRoleType=$bindRoleType roleId=$roleId\n")
        lines.append("ts=${System.currentTimeMillis()}\n")
        lines.append("note: write/destructive endpoints skipped on purpose\n")

        // Each entry increments remaining; record(...) decrements and flushes when 0.
        val tasks: List<() -> Unit> = listOf(
            { biz.listAvatars(devId, cap("config.list-support-avatars")) },
            { biz.listLanguages(devId, cap("config.list-support-languages")) },
            { biz.timbrePage(devId, 1, 20, null, null, null, cap("timbre.page")) },
            { biz.listRoleTemplates(devId, null, cap("role.role-template.list")) },
            { biz.pageCustomRoles(devId, 1, 20, null, cap("role.custom-role.page")) },
            { biz.getBindRole(devId, cap("role.get-bind-role")) },
            { biz.initAgentRoleBinding(devId, cap("role.initialize-agent-role-binding")) },
            { biz.roleTemplateDetail(devId, roleId, cap("role.role-template.detail [roleId=$roleId]")) },
            { biz.customRoleDetail(devId, roleId, cap("role.custom-role.detail [roleId=$roleId]")) },
            { biz.getMemorySwitch(devId, cap("chat.memory.get-switch")) },
            { biz.listMemory(devId, bindRoleType, roleId, cap("chat.memory.list")) },
            { biz.getSummary(devId, bindRoleType, roleId, cap("chat.chat-summary.get")) },
            { biz.currentEmotion(devId, cap("chat.chat-emotion.current")) },
            {
                biz.fetchHistory(
                    devId, bindRoleType, roleId, null, System.currentTimeMillis(), 20, true,
                    cap("chat.history.fetch")
                )
            }
        )
        remaining = tasks.size
        tasks.forEach { it() }
    }

    private fun <T> cap(name: String): Business.ResultListener<T> =
        object : Business.ResultListener<T> {
            override fun onSuccess(r: BusinessResponse?, result: T?, api: String?) =
                record(name, true, r, result)

            override fun onFailure(r: BusinessResponse?, result: T?, api: String?) =
                record(name, false, r, result)
        }

    private fun record(name: String, ok: Boolean, r: BusinessResponse?, parsed: Any?) {
        synchronized(lock) {
            lines.append("\n### ").append(name).append('\n')
            lines.append("status: ").append(if (ok) "OK" else "ERR").append('\n')
            lines.append("code: ").append(r?.errorCode ?: "").append('\n')
            lines.append("msg: ").append(r?.errorMsg ?: "").append('\n')
            lines.append("raw: ").append(r?.result ?: r?.originData ?: "").append('\n')
            lines.append("parsed: ").append(parsed?.toString() ?: "null").append('\n')
            remaining--
            if (remaining <= 0) flush()
        }
    }

    private fun flush() {
        val content = lines.toString()
        Log.i("ai_stream_Diag", content)
        var file: File? = null
        try {
            val dir = context.getExternalFilesDir(null) ?: context.cacheDir
            file = File(dir, "ai_agent_diag_${System.currentTimeMillis()}.txt")
            file.writeText(content)
        } catch (e: Exception) {
            Log.e("ai_stream_Diag", "write failed: ${e.message}")
        }
        onDone?.invoke(file)
    }
}
