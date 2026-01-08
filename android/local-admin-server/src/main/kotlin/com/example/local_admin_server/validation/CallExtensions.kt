package com.example.local_admin_server.validation

import io.ktor.server.application.ApplicationCall

object CallExtensions {
    fun ApplicationCall.requireStringParameter(
        name: String,
    ): String {
        val value = parameters[name]?.takeIf { it.isNotEmpty() } ?: throw IllegalArgumentException("Parameter '$name' is invalid or empty")
        return value
    }

    fun ApplicationCall.requireStringQueryParameter(
        name: String,
    ): String {
        val value =
            request.queryParameters[name]?.takeIf { it.isNotEmpty() }
                ?: throw IllegalArgumentException("Parameter '$name' is invalid or empty")
        return value
    }
}
