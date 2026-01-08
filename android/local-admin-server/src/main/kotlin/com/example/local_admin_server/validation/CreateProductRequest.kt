package com.example.local_admin_server.validation

import io.ktor.http.Parameters
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

data class CreateProductRequest(
    val requestParameters: Parameters,
    val requestBody: String,
) {
    var university: String
    var name: String
    var cost: String
    var productImageBytes: ByteArray

    init {
        val jsonRequestBody = Json.parseToJsonElement(requestBody).jsonObject
        university = requestParameters["university"].toString()
        name = jsonRequestBody["productName"].toString().trim('"')
        cost = jsonRequestBody["productCost"].toString().trim('"')
        val imageBase64 = jsonRequestBody["productImageBase64"].toString().trim('"')
        productImageBytes = java.util.Base64.getDecoder().decode(imageBase64)

        require(university.isNotBlank() || name.isNotBlank() || cost.isNotBlank() || imageBase64.isNotBlank()) {
            "All fields must be non-empty"
        }
    }
}
