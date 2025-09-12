package com.thingclips.sdk.aistream.ai

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