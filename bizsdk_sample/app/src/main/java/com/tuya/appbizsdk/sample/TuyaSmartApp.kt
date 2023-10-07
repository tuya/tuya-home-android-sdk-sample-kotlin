package com.tuya.appbizsdk.sample

import android.app.Application
import com.facebook.drawee.backends.pipeline.Fresco
import com.thingclips.smart.home.sdk.BuildConfig
import com.thingclips.smart.home.sdk.ThingHomeSdk
import com.thingclips.smart.optimus.sdk.ThingOptimusSdk
import com.uuzuche.lib_zxing.activity.ZXingLibrary

class TuyaSmartApp:Application() {
    override fun onCreate() {
        super.onCreate()
        Fresco.initialize(this)

        ThingHomeSdk.setDebugMode(true)
        ThingHomeSdk.init(this)
        ThingOptimusSdk.init(this)
        ZXingLibrary.initDisplayOpinion(this)
    }

}