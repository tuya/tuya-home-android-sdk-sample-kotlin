package com.thingclips.sdk.aistream.ai

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tuya.appsdk.sample.user.R

class ChatAdapter(
    private val context: Context,
    private val messageList: List<ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        val message = messageList[position]
        return if (message.isSentByUser) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_SENT) {
            R.layout.item_chat_message_sent
        } else {
            R.layout.item_chat_message_received
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messageList[position])
    }

    override fun getItemCount(): Int = messageList.size

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Using a single set of properties, as their R.id names differ based on layout
        private val tvMessageText: TextView = itemView.findViewById(R.id.tv_message_text_sent) ?: itemView.findViewById(R.id.tv_message_text_received)
        private val ivMessageImage: ImageView = itemView.findViewById(R.id.iv_message_image_sent) ?: itemView.findViewById(R.id.iv_message_image_received)

        fun bind(message: ChatMessage) {
            // Hide both views initially and show them based on message type
            tvMessageText.isVisible = false
            ivMessageImage.isVisible = false

            when (message.messageType) {
                ChatMessage.MessageType.TEXT,
                ChatMessage.MessageType.VOICE_TO_TEXT,
                ChatMessage.MessageType.NLG_TEXT -> {
                    message.text?.takeIf { it.isNotEmpty() }?.let {
                        tvMessageText.text = it
                        tvMessageText.isVisible = true
                    }
                }
                ChatMessage.MessageType.IMAGE -> {
                    message.imageUri?.let {
                        Glide.with(context).load(it).into(ivMessageImage)
                        ivMessageImage.isVisible = true
                    }
                }
                ChatMessage.MessageType.NLG_IMAGE -> {
                    message.imageUrl?.takeIf { it.isNotEmpty() }?.let {
                        Glide.with(context).load(it).into(ivMessageImage)
                        ivMessageImage.isVisible = true
                    }
                }
            }
        }
    }
}