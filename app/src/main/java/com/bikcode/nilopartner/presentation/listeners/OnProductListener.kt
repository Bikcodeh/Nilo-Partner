package com.bikcode.nilopartner.presentation.listeners

import com.bikcode.nilopartner.data.model.ProductDTO

interface OnProductListener {
    fun onClick(product: ProductDTO)
    fun onLongClick(product: ProductDTO)
}