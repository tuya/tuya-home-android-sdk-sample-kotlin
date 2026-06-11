package com.thingclips.sdk.aistream.ai.chat

import com.thingclips.sdk.aistream.ai.AiIdentityConfig
import com.thingclips.sdk.aistream.ai.call.AiCallActivity
import com.thingclips.sdk.aistream.ai.data.*
import com.thingclips.sdk.aistream.ai.memory.MemoryActivity
import com.thingclips.sdk.aistream.ai.role.RoleEditActivity
import com.thingclips.sdk.aistream.ai.role.RoleListActivity

import android.net.Uri

data class ChatMessage(
    var text: String? = null,
    val imageUri: Uri? = null, // For local images to be sent
    val imageUrl: String? = null, // For received images (URL)
    val isSentByUser: Boolean,
    val messageType: MessageType,
    var bizId: String? = null // For NLG messages, to handle append
) {
    val timestamp: Long = System.currentTimeMillis()

    enum class MessageType {
        TEXT, IMAGE, VOICE_TO_TEXT, NLG_TEXT, NLG_IMAGE
    }
}