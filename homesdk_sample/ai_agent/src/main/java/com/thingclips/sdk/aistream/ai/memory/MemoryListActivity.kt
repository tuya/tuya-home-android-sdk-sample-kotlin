package com.thingclips.sdk.aistream.ai.memory

import com.thingclips.sdk.aistream.ai.data.*

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.thingclips.sdk.aistream.R

/**
 * Long-term memory list mirroring the official app's 格式记忆 / 数据表记忆
 * pages. Both render listMemory results; MODE_FORMAT shows non-shared
 * key/value entries, MODE_DATASHEET shows entries shared across roles
 * (per the official copy "不同角色会共享对应的记忆数据"). Tap a datasheet row
 * to view its content; long-press any row to delete it.
 */
class MemoryListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_FORMAT = 0
        const val MODE_DATASHEET = 1
    }

    private lateinit var agent: AiAgentManager
    private lateinit var roleId: String
    private var bindRoleType: Int = BindRoleType.DEFAULT
    private var mode: Int = MODE_FORMAT

    private val items = mutableListOf<MemoryItem>()
    private lateinit var adapter: MemAdapter
    private lateinit var tvEmpty: TextView
    private lateinit var tvSection: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ai_activity_memory_list)
        val devId = intent.getStringExtra("devId") ?: ""
        roleId = intent.getStringExtra("roleId") ?: ""
        bindRoleType = intent.getIntExtra("bindRoleType", BindRoleType.DEFAULT)
        mode = intent.getIntExtra(EXTRA_MODE, MODE_FORMAT)
        if (devId.isEmpty() || roleId.isEmpty()) {
            Toast.makeText(this, "devId/roleId required", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        agent = AiAgentManager(devId)

        findViewById<ImageView>(R.id.iv_back).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tv_title).setText(
            if (mode == MODE_FORMAT) R.string.ai_memory_format else R.string.ai_memory_datasheet
        )
        findViewById<TextView>(R.id.tv_desc).setText(
            if (mode == MODE_FORMAT) R.string.ai_memory_format_desc
            else R.string.ai_memory_datasheet_desc
        )
        tvSection = findViewById(R.id.tv_section)
        tvEmpty = findViewById(R.id.tv_empty)

        val rv = findViewById<RecyclerView>(R.id.rv_items)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = MemAdapter()
        rv.adapter = adapter

        loadMemory()
    }

    private fun loadMemory() {
        agent.listMemory(bindRoleType, roleId, object : Cb<ArrayList<MemoryGroup>> {
            override fun onOk(data: ArrayList<MemoryGroup>?) {
                items.clear()
                var sectionName: String? = null
                data?.forEach { group ->
                    group.memoryList?.forEach { item ->
                        val isDatasheet = item.shareMemory
                        if ((mode == MODE_DATASHEET) == isDatasheet) {
                            items.add(item)
                            if (sectionName == null) sectionName = group.effectiveScopeName
                        }
                    }
                }
                runOnUiThread {
                    if (mode == MODE_FORMAT && items.isNotEmpty()) {
                        tvSection.text =
                            sectionName ?: getString(R.string.ai_memory_role_section)
                        tvSection.visibility = View.VISIBLE
                    } else {
                        tvSection.visibility = View.GONE
                    }
                    tvEmpty.isVisible = items.isEmpty()
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onErr(code: Int, msg: String?) {
                runOnUiThread {
                    Toast.makeText(this@MemoryListActivity, "Load failed: $msg", Toast.LENGTH_SHORT)
                        .show()
                    tvEmpty.isVisible = items.isEmpty()
                }
            }
        })
    }

    private fun confirmDelete(item: MemoryItem) {
        val key = item.memoryKey ?: return
        AlertDialog.Builder(this)
            .setMessage(R.string.ai_confirm_delete_memory)
            .setNegativeButton(R.string.ai_action_cancel, null)
            .setPositiveButton(R.string.ai_action_delete) { _, _ ->
                agent.deleteMemory(bindRoleType, roleId, false, key, object : Cb<Boolean> {
                    override fun onOk(data: Boolean?) {
                        loadMemory()
                    }

                    override fun onErr(code: Int, msg: String?) {
                        runOnUiThread {
                            Toast.makeText(
                                this@MemoryListActivity,
                                "Delete failed: $msg",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                })
            }
            .show()
    }

    private fun showDetail(item: MemoryItem) {
        AlertDialog.Builder(this)
            .setTitle(item.memoryName ?: item.memoryKey ?: "")
            .setMessage(item.memoryValue ?: "")
            .setPositiveButton(R.string.ai_action_confirm, null)
            .show()
    }

    private inner class MemAdapter : RecyclerView.Adapter<MemVH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemVH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.ai_item_memory_kv, parent, false)
            return MemVH(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: MemVH, position: Int) {
            val m = items[position]
            holder.key.text = m.memoryName ?: m.memoryKey ?: ""
            if (mode == MODE_FORMAT) {
                holder.value.text = m.memoryValue ?: ""
                holder.value.isVisible = true
                holder.arrow.isVisible = false
                holder.itemView.setOnClickListener(null)
            } else {
                holder.value.isVisible = false
                holder.arrow.isVisible = true
                holder.itemView.setOnClickListener { showDetail(m) }
            }
            holder.itemView.setOnLongClickListener {
                confirmDelete(m)
                true
            }
        }
    }

    private inner class MemVH(v: View) : RecyclerView.ViewHolder(v) {
        val key: TextView = v.findViewById(R.id.tv_key)
        val value: TextView = v.findViewById(R.id.tv_value)
        val arrow: ImageView = v.findViewById(R.id.iv_arrow)
    }
}
