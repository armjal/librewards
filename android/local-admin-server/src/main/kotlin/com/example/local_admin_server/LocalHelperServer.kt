package com.example.local_admin_server

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import java.io.FileInputStream

fun main() {
    val env = Dotenv.configure().ignoreIfMissing().load()

    runCatching {
        val creds = GoogleCredentials.getApplicationDefault()

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(FirebaseOptions.builder()
                .setCredentials(creds)
                .setProjectId(env["GOOGLE_CLOUD_PROJECT"])
                .setServiceAccountId(env["SERVICE_ACCOUNT_EMAIL"])
                .build())
        }
    }.onFailure { println("Firebase init failed: ${it.message}") }

    embeddedServer(Netty, 8080) {
        install(ContentNegotiation) { json() }
        routing {
            post("/generate-token-for-admin") {
                val email = call.request.queryParameters["email"] ?: return@post call.respond(mapOf("error" to "Missing email"))
                runCatching {
                    val auth = FirebaseAuth.getInstance()
                    val uid = auth.getUserByEmail(email).uid
                    auth.setCustomUserClaims(uid, mapOf("admin" to true))
                    call.respond(mapOf("customToken" to auth.createCustomToken(uid, mapOf("admin" to true))))
                    print("Successfully generated token for $email")
                }.onFailure { call.respond(mapOf("error" to it.message)) }
            }
        }
    }.start(wait = true)
}
