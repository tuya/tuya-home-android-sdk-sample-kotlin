package com.tuya.appbizsdk.sample

import android.app.Application
import android.content.Context
import com.facebook.drawee.backends.pipeline.Fresco
import com.tuya.appbizsdk.sample.language.LocaleHelper
import com.thingclips.smart.home.sdk.BuildConfig
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.uuzuche.lib_zxing.activity.ZXingLibrary

class TuyaSmartApp:Application() {
    override fun attachBaseContext(base: Context) {
        // Inject the persisted in-app language as early as possible so it survives app restarts
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        Fresco.initialize(this)

        ThingHomeSdk.setDebugMode(true)
        ThingHomeSdk.init(this)
        ThingOptimusSdk.init(this)
        ZXingLibrary.initDisplayOpinion(this)
    }

}