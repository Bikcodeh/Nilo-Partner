package com.bikcode.nilopartner.presentation.listeners

import com.bikcode.nilopartner.data.model.OrderDTO

interface OrderAux {
    fun getOrderSelected(): OrderDTO?
}