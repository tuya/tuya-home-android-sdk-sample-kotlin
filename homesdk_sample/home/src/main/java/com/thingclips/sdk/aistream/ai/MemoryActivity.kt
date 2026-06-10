package com.thingclips.sdk.aistream.ai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tuya.appsdk.sample.user.R

/**
 * Lists agent role memory and supports per-item / full deletion. Requires
 * "devId", "roleId", "bindRoleType" extras.
 */
class MemoryActivity : AppCompatActivity() {

    private lateinit var agent: AiAgentManager
    private lateinit var devId: String
    private lateinit var roleId: String
    private var bindRoleType: Int = BindRoleType.DEFAULT

    private val items = mutableListOf<MemoryItem>()
    private lateinit var adapter: MemAdapter
    private lateinit var tvSwitch: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ai_activity_memory)
        title = "Memory"
        devId = intent.getStringExtra("devId") ?: ""
        roleId = intent.getStringExtra("roleId") ?: ""
        bindRoleType = intent.getIntExtra("bindRoleType", BindRoleType.DEFAULT)
        if (devId.isEmpty() || roleId.isEmpty()) {
            Toast.makeText(this, "devId/roleId required", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        agent = AiAgentManager(devId)

        tvSwitch = findViewById(R.id.tv_memory_switch)
        val rv = findViewById<RecyclerView>(R.id.rv_memory)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = MemAdapter()
        rv.adapter = adapter

        findViewById<Button>(R.id.btn_clear_all_memory).setOnClickListener { confirmClearAll() }

        loadSwitch()
        loadMemory()
    }

    private fun loadSwitch() {
        agent.getMemorySwitch(object : Cb<MemorySwitch> {
            override fun onOk(data: MemorySwitch?) {
                tvSwitch.text = "memoryOpen=${data?.memoryOpen} summaryOpen=${data?.summaryOpen}"
            }

            override fun onErr(code: Int, msg: String?) {
                tvSwitch.text = "Memory switch: failed ($msg)"
            }
        })
    }

    private fun loadMemory() {
        agent.listMemory(bindRoleType, roleId, object : Cb<ArrayList<MemoryGroup>> {
            override fun onOk(data: ArrayList<MemoryGroup>?) {
                items.clear()
                data?.forEach { g -> g.memoryList?.let { items.addAll(it) } }
                adapter.notifyDataSetChanged()
            }

            override fun onErr(code: Int, msg: String?) {
                Toast.makeText(this@MemoryActivity, "list failed: $msg", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteOne(key: String) {
        agent.deleteMemory(bindRoleType, roleId, false, key, object : Cb<Boolean> {
            override fun onOk(data: Boolean?) {
                loadMemory()
            }

            override fun onErr(code: Int, msg: String?) {
                Toast.makeText(this@MemoryActivity, "delete failed: $msg", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun confirmClearAll() {
        AlertDialog.Builder(this)
            .setMessage("Clear all memory for this role?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Clear") { _, _ ->
                agent.deleteMemory(bindRoleType, roleId, true, null, object : Cb<Boolean> {
                    override fun onOk(data: Boolean?) {
                        loadMemory()
                    }

                    override fun onErr(code: Int, msg: String?) {
                        Toast.makeText(this@MemoryActivity, "clear failed: $msg", Toast.LENGTH_SHORT).show()
                    }
                })
            }
            .show()
    }

    private inner class MemAdapter : RecyclerView.Adapter<MemVH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemVH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.ai_item_memory, parent, false)
            return MemVH(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: MemVH, position: Int) {
            val m = items[position]
            holder.name.text = m.memoryName ?: m.memoryKey ?: ""
            holder.value.text = m.memoryValue ?: ""
            holder.delete.setOnClickListener {
                m.memoryKey?.let { deleteOne(it) }
            }
        }
    }

    private inner class MemVH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.tv_memory_name)
        val value: TextView = v.findViewById(R.id.tv_memory_value)
        val delete: Button = v.findViewById(R.id.btn_delete_memory)
    }
}
