package com.bikcode.nilopartner.presentation.listeners

import com.bikcode.nilopartner.data.model.Message

interface OnChatListener {
    fun deleteMessage(message: Message)
}