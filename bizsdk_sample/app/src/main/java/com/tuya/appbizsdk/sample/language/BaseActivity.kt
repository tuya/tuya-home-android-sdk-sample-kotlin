package com.tuya.appbizsdk.sample.language

import android.content.Context
import androidx.appcompat.app.AppCompatActivity

/**
 * Base activity that injects the persisted language.
 *
 * Subclass it (instead of [AppCompatActivity]) to make the switched language
 * take effect on the screen. Because androidx.appcompat 1.2.x wraps an
 * activity's base context, only injecting in attachBaseContext reliably
 * overrides the language.
 */
abstract class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }
}
