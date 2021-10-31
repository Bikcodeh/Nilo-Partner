package com.bikcode.nilopartner.domain.model

data class Product(
    val id: String,
    val name: String,
    val description: String,
    val imgUrl: String,
    val quantity: Int,
    val price: Double
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Product

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
