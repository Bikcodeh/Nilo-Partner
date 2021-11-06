package com.bikcode.nilopartner.presentation.listeners

import com.bikcode.nilopartner.data.model.OrderDTO

interface OnOrderListener {
    fun onStartChat(order: OrderDTO)
    fun onStatusChange(order: OrderDTO)
}