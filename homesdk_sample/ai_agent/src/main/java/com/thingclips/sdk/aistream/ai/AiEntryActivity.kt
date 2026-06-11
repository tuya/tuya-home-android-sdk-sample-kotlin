package com.thingclips.sdk.aistream.ai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.home.sdk.bean.HomeBean
import com.thingclips.smart.home.sdk.callback.IThingHomeResultCallback
import com.thingclips.sdk.aistream.R

/**
 * AI assistant entry: choose between App identity chat (account-level, no
 * device required) and device identity chat (requires picking a device whose
 * PID the aiSolutionCode is published to). The gear opens AiConfigActivity
 * for key management. Requires "homeId" extra.
 */
class AiEntryActivity : AppCompatActivity() {

    private var homeId: Long = 0L
    private lateinit var tvAppState: TextView
    private lateinit var tvDeviceState: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ai_activity_entry)
        homeId = intent.getLongExtra("homeId", 0L)
        if (homeId == 0L) {
            Toast.makeText(this, "homeId required", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<ImageView>(R.id.iv_back).setOnClickListener { finish() }
        findViewById<ImageView>(R.id.iv_settings).setOnClickListener {
            startActivity(Intent(this, AiConfigActivity::class.java))
        }
        tvAppState = findViewById(R.id.tv_app_state)
        tvDeviceState = findViewById(R.id.tv_device_state)

        findViewById<View>(R.id.card_app_identity).setOnClickListener { launchAppChat() }
        findViewById<View>(R.id.card_device_identity).setOnClickListener { launchDeviceChat() }
    }

    override fun onResume() {
        super.onResume()
        refreshStates()
    }

    private fun refreshStates() {
        tvAppState.setText(
            if (AiIdentityConfig.isConfigured(this, AiIdentityConfig.IDENTITY_APP))
                R.string.ai_entry_configured else R.string.ai_entry_not_configured
        )
        tvDeviceState.setText(
            if (AiIdentityConfig.isConfigured(this, AiIdentityConfig.IDENTITY_DEVICE))
                R.string.ai_entry_configured else R.string.ai_entry_not_configured
        )
    }

    private fun requireKeys(identity: Int): AiIdentityConfig.Keys? {
        val keys = AiIdentityConfig.resolve(this, identity)
        if (keys == null) {
            Toast.makeText(this, R.string.ai_config_keys_missing, Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, AiConfigActivity::class.java))
        }
        return keys
    }

    private fun launchAppChat() {
        val keys = requireKeys(AiIdentityConfig.IDENTITY_APP) ?: return
        startActivity(
            Intent(this, AiAppChatActivity::class.java)
                .putExtra("ownerId", homeId.toString())
                .putExtra("aiSolutionCode", keys.aiSolutionCode)
                .putExtra("miniProgramId", keys.miniProgramId)
        )
    }

    private fun launchDeviceChat() {
        val keys = requireKeys(AiIdentityConfig.IDENTITY_DEVICE) ?: return
        ThingHomeSdk.newHomeInstance(homeId).getHomeDetail(object : IThingHomeResultCallback {
            override fun onSuccess(bean: HomeBean?) {
                val devices = bean?.deviceList ?: emptyList()
                if (devices.isEmpty()) {
                    Toast.makeText(
                        this@AiEntryActivity,
                        "No devices in this home",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                val names = devices.map { it.name ?: it.devId }.toTypedArray()
                AlertDialog.Builder(this@AiEntryActivity)
                    .setTitle(R.string.ai_select_device)
                    .setItems(names) { _, which ->
                        startActivity(
                            Intent(this@AiEntryActivity, AiDeviceChatActivity::class.java)
                                .putExtra("ownerId", homeId.toString())
                                .putExtra("aiSolutionCode", keys.aiSolutionCode)
                                .putExtra("miniProgramId", keys.miniProgramId)
                                .putExtra("devId", devices[which].devId)
                        )
                    }
                    .show()
            }

            override fun onError(errorCode: String?, errorMsg: String?) {
                Toast.makeText(
                    this@AiEntryActivity,
                    "Load devices failed: $errorMsg",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}
