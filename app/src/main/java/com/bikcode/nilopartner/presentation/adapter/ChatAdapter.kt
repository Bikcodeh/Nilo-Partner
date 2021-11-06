package com.bikcode.nilopartner.presentation.adapter

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bikcode.nilopartner.R
import com.bikcode.nilopartner.data.model.Message
import com.bikcode.nilopartner.databinding.ItemChatBinding
import com.bikcode.nilopartner.presentation.listeners.OnChatListener

class ChatAdapter(
    private val messages: MutableList<Message>,
    private val listener: OnChatListener,
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private lateinit var context: Context

    fun add(message: Message) {
        if (messages.contains(message).not()) {
            messages.add(message)
            notifyItemInserted(messages.count() - 1)
        }
    }

    fun update(message: Message) {
        messages.indexOf(message).also {
            if (it != -1) {
                messages[it] = message
                notifyItemChanged(it)
            }
        }
    }

    fun delete(message: Message) {
        messages.indexOf(message).also {
            if (it != -1) {
                messages.removeAt(it)
                notifyItemRemoved(it)
            }
        }
    }

    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = ItemChatBinding.bind(view)

        var gravity = Gravity.END
        var background = ContextCompat.getDrawable(context, R.drawable.background_chat_client)
        var textColor = ContextCompat.getColor(context, R.color.colorOnSecondary)

        private val marginHorizontal =
            context.resources.getDimensionPixelSize(R.dimen.chat_margin_horizontal)
        private val params = binding.tvMessage.layoutParams as ViewGroup.MarginLayoutParams

        fun bind(message: Message) {
            params.marginStart = marginHorizontal
            params.marginEnd = 0
            params.topMargin = 0

            if (message.isSendByClient().not()) {
                gravity = Gravity.START
                background = ContextCompat.getDrawable(context, R.drawable.background_chat_support)
                textColor = ContextCompat.getColor(context, R.color.colorOnPrimary)
                params.marginStart = 0
                params.marginEnd = marginHorizontal
            }

            if (adapterPosition > 0 && message.isSendByClient() != messages[adapterPosition - 1].isSendByClient()) {
                params.topMargin =
                    context.resources.getDimensionPixelSize(R.dimen.common_padding_min)
            }

            binding.root.gravity = gravity
            binding.tvMessage.layoutParams = params
            binding.tvMessage.background = (background)
            binding.tvMessage.setTextColor(textColor)
            binding.tvMessage.text = message.message
        }

        fun setListener(message: Message) {
            binding.tvMessage.setOnLongClickListener {
                listener.deleteMessage(message)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
        holder.setListener(message)
    }

    override fun getItemCount(): Int = messages.count()
}