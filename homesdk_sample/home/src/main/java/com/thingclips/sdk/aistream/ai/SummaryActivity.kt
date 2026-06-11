package com.thingclips.sdk.aistream.ai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tuya.appsdk.sample.user.R
import org.json.JSONArray

/**
 * "对话总结" page mirroring the official app: summary entries rendered as
 * cards (oldest dropped first once the capacity cap is hit, per the page
 * copy). The bottom button edits the raw summary text via updateSummary.
 */
class SummaryActivity : AppCompatActivity() {

    private lateinit var agent: AiAgentManager
    private lateinit var roleId: String
    private var bindRoleType: Int = BindRoleType.DEFAULT

    private val items = mutableListOf<String>()
    private var rawSummary: String = ""
    private lateinit var adapter: SummaryAdapter
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ai_activity_summary)
        val devId = intent.getStringExtra("devId") ?: ""
        roleId = intent.getStringExtra("roleId") ?: ""
        bindRoleType = intent.getIntExtra("bindRoleType", BindRoleType.DEFAULT)
        if (devId.isEmpty() || roleId.isEmpty()) {
            Toast.makeText(this, "devId/roleId required", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        agent = AiAgentManager(devId)

        findViewById<ImageView>(R.id.iv_back).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btn_edit_summary).setOnClickListener { showEditor() }
        tvEmpty = findViewById(R.id.tv_empty)

        val rv = findViewById<RecyclerView>(R.id.rv_items)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = SummaryAdapter()
        rv.adapter = adapter

        loadSummary()
    }

    private fun loadSummary() {
        agent.getSummary(bindRoleType, roleId, object : Cb<String> {
            override fun onOk(data: String?) {
                rawSummary = data ?: ""
                runOnUiThread {
                    items.clear()
                    items.addAll(parseSummaryItems(rawSummary))
                    tvEmpty.isVisible = items.isEmpty()
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onErr(code: Int, msg: String?) {
                runOnUiThread {
                    Toast.makeText(this@SummaryActivity, "Load failed: $msg", Toast.LENGTH_SHORT)
                        .show()
                    tvEmpty.isVisible = items.isEmpty()
                }
            }
        })
    }

    /** Summary arrives as one string; render a JSON array as one card per
     *  entry, otherwise one card per non-blank line. */
    private fun parseSummaryItems(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        runCatching {
            val arr = JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                arr.optString(i)?.takeIf { it.isNotBlank() }?.let { list.add(it) }
            }
            if (list.isNotEmpty()) return list
        }
        return raw.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun showEditor() {
        val input = EditText(this).apply {
            setText(rawSummary)
            setSelection(text.length)
            minLines = 3
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.ai_memory_summary)
            .setView(input)
            .setNegativeButton(R.string.ai_action_cancel, null)
            .setPositiveButton(R.string.ai_action_save) { _, _ ->
                agent.updateSummary(
                    bindRoleType, roleId, input.text.toString(),
                    object : Cb<Boolean> {
                        override fun onOk(data: Boolean?) {
                            loadSummary()
                        }

                        override fun onErr(code: Int, msg: String?) {
                            runOnUiThread {
                                Toast.makeText(
                                    this@SummaryActivity,
                                    "Save failed: $msg",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )
            }
            .show()
    }

    private inner class SummaryAdapter : RecyclerView.Adapter<SummaryVH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryVH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.ai_item_summary, parent, false)
            return SummaryVH(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: SummaryVH, position: Int) {
            holder.text.text = items[position]
        }
    }

    private inner class SummaryVH(v: View) : RecyclerView.ViewHolder(v) {
        val text: TextView = v.findViewById(R.id.tv_summary)
    }
}
