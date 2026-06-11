package com.thingclips.sdk.aistream.ai

import com.tuya.appsdk.sample.user.R

/**
 * App-identity chat: a plain conversation surface. There is no role concept
 * for this identity (role/memory ATOP APIs are devId-scoped), so its layout
 * has no role card or overflow menu, and history persists under an
 * "app:<solutionCode>" surrogate key.
 */
class AiAppChatActivity : BaseAiChatActivity() {

    override val identity: Int = AiIdentityConfig.IDENTITY_APP
    override val layoutResId: Int = R.layout.ai_activity_chat_app

    override fun initIdentity(): Boolean {
        mDevId = "app:$mAiSolutionCode" // local history key only
        return true
    }
}
