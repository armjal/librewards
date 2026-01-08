package com.example.librewards.utils

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ServerResponse(
    val status: Int,
    val message: String,
)

object LocalServerUtils {
    const val BASE_URL = "http://10.0.2.2:8080"

    fun post(path: String, requestBody: JSONObject? = null): ServerResponse {
        val url = URL("$BASE_URL$path")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        if (requestBody != null) connection.outputStream.use { it.write(requestBody.toString().toByteArray()) }
        val responseCode = connection.responseCode

        return ServerResponse(responseCode, connection.inputStream.bufferedReader().readText())
    }

    fun delete(path: String): ServerResponse {
        val url = URL("$BASE_URL$path")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "DELETE"
        return ServerResponse(connection.responseCode, connection.responseMessage)
    }
}
