package com.example.librewards.models

data class Product(
    var productName: String = "",
    var productCost: String = "",
    var productImageUrl: String = ""
) {
    fun toMap(): Map<String, String> {
        return mapOf(
            "productName" to productName,
            "productCost" to productCost,
            "productImageUrl" to productImageUrl)
    }
}


