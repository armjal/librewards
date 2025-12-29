package com.example.librewards.utils

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ServerResponse(
    val status: Int,
    val message: String,
)

object LocalServerUtils {
    fun post(url: URL, requestBody: JSONObject? = null): ServerResponse {
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        if (requestBody != null) connection.outputStream.use { it.write(requestBody.toString().toByteArray()) }
        val responseCode = connection.responseCode

        return ServerResponse(responseCode, connection.inputStream.bufferedReader().readText())
    }

    fun delete(url: URL): ServerResponse {
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "DELETE"
        return ServerResponse(connection.responseCode, connection.responseMessage)
    }
}
