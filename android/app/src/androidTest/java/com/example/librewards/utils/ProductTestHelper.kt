package com.example.librewards.utils

import android.util.Base64
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.example.librewards.data.models.Product
import org.json.JSONObject

object ProductTestHelper {
    fun createProduct(university: String, product: Product, productImageBase64Encoded: String) {
        val encodedUniversity = university.replace(" ", "%20")
        val createProductPath = "/$encodedUniversity/product"

        val requestBody = JSONObject().apply {
            put("productName", product.productName)
            put("productCost", product.productCost)
            put("productImageBase64", productImageBase64Encoded)
        }
        val serverResponse = LocalServerUtils.post(createProductPath, requestBody)
        if (serverResponse.status != 200) {
            throw RuntimeException("Product failed to create, returned code ${serverResponse.status}")
        }
    }

    fun createTestProducts(university: String = "University of Integration Tests", products: List<Product>) {
        val context = getInstrumentation().context

        for (product in products) {
            val productImageAssetName = product.productName.lowercase()

            val resId = context.resources.getIdentifier(productImageAssetName, "drawable", context.packageName)
            val inputStream = context.resources.openRawResource(resId)
            val productImageBytes = inputStream.readBytes()
            val productImageBase64Encoded = Base64.encodeToString(productImageBytes, Base64.NO_WRAP)

            createProduct(university, product, productImageBase64Encoded)
        }
    }

    fun deleteProducts(university: String = "University of Integration Tests") {
        val encodedUniversity = university.replace(" ", "%20")
        val deleteProductsPath = "/$encodedUniversity/products"

        val serverResponse = LocalServerUtils.delete(deleteProductsPath)
        if (serverResponse.status != 200) {
            throw RuntimeException("Products failed to delete for $university, returned code ${serverResponse.status}")
        }
    }
}
