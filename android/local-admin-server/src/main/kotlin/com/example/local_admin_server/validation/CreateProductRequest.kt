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
    var imageBase64: String

    init {
        val jsonRequestBody = Json.parseToJsonElement(requestBody).jsonObject
        university = requestParameters["university"].toString()
        name = jsonRequestBody["productName"].toString().trim('"')
        cost = jsonRequestBody["productCost"].toString().trim('"')
        imageBase64 = jsonRequestBody["productImageBase64"].toString().trim('"')

        require(university.isNotBlank() || name.isNotBlank() || cost.isNotBlank() || imageBase64.isNotBlank()) {
            "All fields must be non-empty"
        }
    }
}
