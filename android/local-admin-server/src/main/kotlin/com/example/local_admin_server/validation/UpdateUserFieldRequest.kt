package com.example.local_admin_server.validation

import io.ktor.http.Parameters
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

data class UpdateUserFieldRequest(
    val requestParameters: Parameters,
    val requestBody: String,
) {
    val uid = requestParameters["uid"]
    var field: String
    var value: String

    init {
        val jsonRequestBody = Json.parseToJsonElement(requestBody).jsonObject
        field = jsonRequestBody["field"].toString().trim('"')
        value = jsonRequestBody["value"].toString().trim('"')

        require(!(uid.isNullOrBlank() || field.isBlank() || value.isBlank())) { "All fields must be non-empty" }
    }
}
