package com.tuya.appbizsdk.sample.language

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.core.os.ConfigurationCompat
import java.util.Locale

/**
 * In-app language switching helper.
 *
 * The core mechanism mirrors the Tuya SDK's internal
 * `com.thingclips.smart.base.utils.ThingLanguageUtils#switchLanguage(Locale)`:
 * it switches the language by updating the Application resources' Configuration
 * locale and then calling updateConfiguration to refresh the resources.
 *
 * On top of that, two additions make it work reliably with androidx.appcompat
 * 1.2.x and across app restarts:
 *  - Persistence (SharedPreferences): the choice survives app restarts.
 *  - [wrap]: called in attachBaseContext so every AppCompatActivity loads with
 *    the persisted language.
 */
object LocaleHelper {

    private const val SP_NAME = "locale_sp"
    private const val KEY_LANGUAGE = "key_language"

    /** Empty string means "follow the system". */
    const val LANG_SYSTEM = ""
    const val LANG_ENGLISH = "en"
    const val LANG_CHINESE = "zh"

    /**
     * Switch language: persists the choice and immediately updates app-level
     * resources (same as ThingLanguageUtils.switchLanguage).
     * The caller should then call [restartApp] so the new locale fully takes
     * effect across the whole app (the Tuya SDK itself restarts the app after
     * a language switch — see DebugPresenter / ApplicationUtil.relaunchApp).
     *
     * @param language one of [LANG_ENGLISH], [LANG_CHINESE], [LANG_SYSTEM].
     */
    @Suppress("DEPRECATION")
    fun switchLanguage(context: Context, language: String) {
        // 1. Persist the user's choice
        saveLanguage(context, language)

        // 2. Update Application resources immediately, exactly like
        //    ThingLanguageUtils.switchLanguage:
        //      config.locale = locale
        //      resources.updateConfiguration(config, null)
        val res = context.applicationContext.resources
        val config = Configuration(res.configuration)
        applyLocale(config, resolveLocale(context, language))
        res.updateConfiguration(config, res.displayMetrics)
    }

    /**
     * Relaunch the app so the switched language fully takes effect.
     *
     * Mirrors the Tuya SDK's `ApplicationUtil.relaunchApp(true)`: re-launch the
     * launcher activity with a clean task, then kill the current process so the
     * app restarts fresh and [com.tuya.appbizsdk.sample.TuyaSmartApp] re-applies
     * the persisted locale in attachBaseContext. Call this right after
     * [switchLanguage].
     */
    fun restartApp(context: Context) {
        val intent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?: return
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
        context.startActivity(intent)
        android.os.Process.killProcess(android.os.Process.myPid())
        System.exit(0)
    }

    /**
     * Wrap a Context with the persisted language, for use in the
     * attachBaseContext of [com.tuya.appbizsdk.sample.TuyaSmartApp] / [BaseActivity].
     */
    fun wrap(context: Context): Context {
        val language = getLanguage(context)
        if (language == LANG_SYSTEM) {
            return context // Follow system: do not override
        }
        val config = Configuration(context.resources.configuration)
        applyLocale(config, resolveLocale(context, language))
        return context.createConfigurationContext(config)
    }

    /** Currently persisted language ("" means follow the system). */
    fun getLanguage(context: Context): String =
        context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, LANG_SYSTEM) ?: LANG_SYSTEM

    private fun saveLanguage(context: Context, language: String) {
        context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language)
            .apply()
    }

    private fun resolveLocale(context: Context, language: String): Locale = when (language) {
        LANG_ENGLISH -> Locale.ENGLISH
        LANG_CHINESE -> Locale.SIMPLIFIED_CHINESE
        else -> systemLocale()
    }

    /** Real system locale, unaffected by the app's own override. */
    private fun systemLocale(): Locale =
        ConfigurationCompat.getLocales(Resources.getSystem().configuration).get(0)
            ?: Locale.getDefault()

    private fun applyLocale(config: Configuration, locale: Locale) {
        config.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        }
        config.setLayoutDirection(locale)
    }
}
