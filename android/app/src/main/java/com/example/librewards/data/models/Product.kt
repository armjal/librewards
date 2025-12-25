package com.example.librewards.data.models

data class ProductEntry(
    var id: String = "",
    var product: Product = Product(),
)

data class Product(
    var productName: String = "",
    var productCost: String = "",
    var productImageUrl: String = "",
) {
    fun toMap(): Map<String, String> = mapOf(
        "productName" to productName,
        "productCost" to productCost,
        "productImageUrl" to productImageUrl,
    )
}
