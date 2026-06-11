package com.thingclips.sdk.aistream.ai

import android.content.Context
import android.content.pm.PackageManager
import java.util.Properties

/**
 * Resolves the aiSolutionCode / miniProgramId pair for each chat identity.
 *
 * The keys are secrets and must never be committed, so resolution is layered:
 *  1. In-app override saved from AiConfigActivity (SharedPreferences).
 *  2. assets/ai_identity.properties — gitignored; see the committed
 *     ai_identity.properties.example template.
 *  3. Manifest meta-data AI_SOLUTION_CODE / MINI_PROGRAM_ID, injected from
 *     local.properties (legacy path, device identity only).
 *
 * Device identity codes are published to the device PID; app identity codes
 * come from a solution created on the platform and published to this app.
 */
object AiIdentityConfig {

    const val IDENTITY_DEVICE = 0
    const val IDENTITY_APP = 1

    const val EXTRA_IDENTITY = "identity"

    private const val PREFS = "ai_identity_keys"
    private const val ASSET_FILE = "ai_identity.properties"

    data class Keys(val aiSolutionCode: String, val miniProgramId: String)

    fun resolve(context: Context, identity: Int): Keys? {
        val solution = firstNonEmpty(
            prefs(context).getString(prefKey(identity, "solution"), null),
            assetProps(context).getProperty(assetKey(identity, "aiSolutionCode")),
            if (identity == IDENTITY_DEVICE) metaData(context, "AI_SOLUTION_CODE") else null
        )
        val mini = firstNonEmpty(
            prefs(context).getString(prefKey(identity, "mini"), null),
            assetProps(context).getProperty(assetKey(identity, "miniProgramId")),
            if (identity == IDENTITY_DEVICE) metaData(context, "MINI_PROGRAM_ID") else null
        )
        if (solution.isNullOrEmpty() || mini.isNullOrEmpty()) return null
        return Keys(solution, mini)
    }

    fun isConfigured(context: Context, identity: Int): Boolean =
        resolve(context, identity) != null

    /** Blank values clear the override so defaults apply again. */
    fun saveOverride(context: Context, identity: Int, solution: String?, mini: String?) {
        prefs(context).edit().apply {
            putOrRemove(prefKey(identity, "solution"), solution)
            putOrRemove(prefKey(identity, "mini"), mini)
        }.apply()
    }

    /** Current override values (empty when none), for prefilling the editor. */
    fun overrideOf(context: Context, identity: Int): Pair<String, String> = Pair(
        prefs(context).getString(prefKey(identity, "solution"), "") ?: "",
        prefs(context).getString(prefKey(identity, "mini"), "") ?: ""
    )

    /** Defaults below the override layer, shown as hints in the editor. */
    fun defaultsOf(context: Context, identity: Int): Pair<String, String> {
        val props = assetProps(context)
        val solution = firstNonEmpty(
            props.getProperty(assetKey(identity, "aiSolutionCode")),
            if (identity == IDENTITY_DEVICE) metaData(context, "AI_SOLUTION_CODE") else null
        ) ?: ""
        val mini = firstNonEmpty(
            props.getProperty(assetKey(identity, "miniProgramId")),
            if (identity == IDENTITY_DEVICE) metaData(context, "MINI_PROGRAM_ID") else null
        ) ?: ""
        return Pair(solution, mini)
    }

    private fun android.content.SharedPreferences.Editor.putOrRemove(k: String, v: String?) {
        if (v.isNullOrBlank()) remove(k) else putString(k, v.trim())
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun prefKey(identity: Int, field: String) =
        "${if (identity == IDENTITY_APP) "app" else "device"}.$field"

    private fun assetKey(identity: Int, field: String) =
        "${if (identity == IDENTITY_APP) "app" else "device"}.$field"

    private fun assetProps(context: Context): Properties {
        val props = Properties()
        runCatching {
            context.assets.open(ASSET_FILE).use { props.load(it) }
        }
        return props
    }

    private fun metaData(context: Context, key: String): String? = try {
        context.packageManager
            .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            .metaData?.getString(key)
    } catch (e: Exception) {
        null
    }

    private fun firstNonEmpty(vararg values: String?): String? =
        values.firstOrNull { !it.isNullOrBlank() }?.trim()
}
