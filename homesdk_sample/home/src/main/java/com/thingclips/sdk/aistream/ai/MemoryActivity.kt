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
import com.thingclips.smart.android.network.Business
import com.thingclips.smart.android.network.http.BusinessResponse
import com.tuya.appsdk.sample.user.R

/**
 * Lists agent role memory and supports per-item / full deletion. Requires
 * "devId", "roleId", "bindRoleType" extras.
 */
class MemoryActivity : AppCompatActivity() {

    private val business = AiAgentBusiness()
    private lateinit var devId: String
    private lateinit var roleId: String
    private var bindRoleType: Int = 2

    private val items = mutableListOf<MemoryItem>()
    private lateinit var adapter: MemAdapter
    private lateinit var tvSwitch: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ai_activity_memory)
        title = "Memory"
        devId = intent.getStringExtra("devId") ?: ""
        roleId = intent.getStringExtra("roleId") ?: ""
        bindRoleType = intent.getIntExtra("bindRoleType", 2)
        if (devId.isEmpty() || roleId.isEmpty()) {
            Toast.makeText(this, "devId/roleId required", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

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
        business.getMemorySwitch(devId, object : Business.ResultListener<MemorySwitch> {
            override fun onSuccess(r: BusinessResponse?, result: MemorySwitch?, api: String?) {
                tvSwitch.text = "memoryOpen=${result?.memoryOpen} summaryOpen=${result?.summaryOpen}"
            }

            override fun onFailure(r: BusinessResponse?, result: MemorySwitch?, api: String?) {
                tvSwitch.text = "Memory switch: failed (${r?.errorMsg})"
            }
        })
    }

    private fun loadMemory() {
        business.listMemory(devId, bindRoleType, roleId, object : Business.ResultListener<ArrayList<MemoryGroup>> {
            override fun onSuccess(r: BusinessResponse?, result: ArrayList<MemoryGroup>?, api: String?) {
                items.clear()
                result?.forEach { g -> g.memoryList?.let { items.addAll(it) } }
                adapter.notifyDataSetChanged()
            }

            override fun onFailure(r: BusinessResponse?, result: ArrayList<MemoryGroup>?, api: String?) {
                Toast.makeText(this@MemoryActivity, "list failed: ${r?.errorMsg}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun deleteOne(key: String) {
        business.deleteMemory(devId, bindRoleType, roleId, false, key, object : Business.ResultListener<Boolean> {
            override fun onSuccess(r: BusinessResponse?, result: Boolean?, api: String?) {
                loadMemory()
            }

            override fun onFailure(r: BusinessResponse?, result: Boolean?, api: String?) {
                Toast.makeText(this@MemoryActivity, "delete failed: ${r?.errorMsg}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun confirmClearAll() {
        AlertDialog.Builder(this)
            .setMessage("Clear all memory for this role?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Clear") { _, _ ->
                business.deleteMemory(devId, bindRoleType, roleId, true, null, object : Business.ResultListener<Boolean> {
                    override fun onSuccess(r: BusinessResponse?, result: Boolean?, api: String?) {
                        loadMemory()
                    }

                    override fun onFailure(r: BusinessResponse?, result: Boolean?, api: String?) {
                        Toast.makeText(this@MemoryActivity, "clear failed: ${r?.errorMsg}", Toast.LENGTH_SHORT).show()
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
