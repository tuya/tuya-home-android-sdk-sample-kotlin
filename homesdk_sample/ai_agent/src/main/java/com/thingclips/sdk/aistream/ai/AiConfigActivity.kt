package com.thingclips.sdk.aistream.ai

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.thingclips.sdk.aistream.R

/**
 * Key management for AI identities. Values typed here are stored as
 * device-local SharedPreferences overrides on top of the gitignored
 * defaults (assets/ai_identity.properties, local.properties); blanking a
 * field and saving falls back to those defaults. Keys never enter git.
 */
class AiConfigActivity : AppCompatActivity() {

    private lateinit var etAppSolution: EditText
    private lateinit var etAppMini: EditText
    private lateinit var etDeviceSolution: EditText
    private lateinit var etDeviceMini: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ai_activity_config)

        findViewById<ImageView>(R.id.iv_back).setOnClickListener { finish() }
        etAppSolution = findViewById(R.id.et_app_solution)
        etAppMini = findViewById(R.id.et_app_mini)
        etDeviceSolution = findViewById(R.id.et_device_solution)
        etDeviceMini = findViewById(R.id.et_device_mini)

        prefill(AiIdentityConfig.IDENTITY_APP, etAppSolution, etAppMini)
        prefill(AiIdentityConfig.IDENTITY_DEVICE, etDeviceSolution, etDeviceMini)

        findViewById<TextView>(R.id.btn_save).setOnClickListener { save() }
    }

    /** Override values fill the field; layered defaults show as hints. */
    private fun prefill(identity: Int, etSolution: EditText, etMini: EditText) {
        val (overrideSolution, overrideMini) = AiIdentityConfig.overrideOf(this, identity)
        val (defSolution, defMini) = AiIdentityConfig.defaultsOf(this, identity)
        etSolution.setText(overrideSolution)
        etMini.setText(overrideMini)
        if (defSolution.isNotEmpty()) etSolution.hint = defSolution
        if (defMini.isNotEmpty()) etMini.hint = defMini
    }

    private fun save() {
        AiIdentityConfig.saveOverride(
            this, AiIdentityConfig.IDENTITY_APP,
            etAppSolution.text.toString(), etAppMini.text.toString()
        )
        AiIdentityConfig.saveOverride(
            this, AiIdentityConfig.IDENTITY_DEVICE,
            etDeviceSolution.text.toString(), etDeviceMini.text.toString()
        )
        Toast.makeText(this, R.string.ai_config_saved, Toast.LENGTH_SHORT).show()
        finish()
    }
}
