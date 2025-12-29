package com.example.local_admin_server

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receiveText
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

fun main() {
    val env = Dotenv.configure().ignoreIfMissing().load()

    runCatching {
        val creds = GoogleCredentials.getApplicationDefault()

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(
                FirebaseOptions.builder()
                    .setCredentials(creds)
                    .setDatabaseUrl(env["REALTIME_DB_URL"]) // Ensure this is in .env or hardcoded if missing
                    .setProjectId(env["GOOGLE_CLOUD_PROJECT"])
                    .setServiceAccountId(env["SERVICE_ACCOUNT_EMAIL"])
                    .build()
            )
        }
    }.onFailure { println("Firebase init failed: ${it.message}") }

    embeddedServer(Netty, 8080) {
        install(ContentNegotiation) { json() }
        routing {
            get("/") {
                call.respondText("OK")
            }

            post("/generate-token-for-admin") {
                val email = call.request.queryParameters["email"] ?: return@post call.respond(mapOf("error" to "Missing email"))
                runCatching {
                    val auth = FirebaseAuth.getInstance()
                    val uid = auth.getUserByEmail(email).uid
                    auth.setCustomUserClaims(uid, mapOf("admin" to true))
                    call.respond(mapOf("customToken" to auth.createCustomToken(uid, mapOf("admin" to true))))
                }.onFailure { call.respond(mapOf("error" to it.message)) }
            }

            delete("/{uid}/user") {
                val uid = call.parameters["uid"] ?: return@delete call.respond(mapOf("error" to "Missing uid"))
                runCatching {
                    val auth = FirebaseAuth.getInstance()
                    val db = FirebaseDatabase.getInstance()
                    db.getReference("users").child(uid).removeValueAsync().get()


                    call.respond(mapOf("status" to "success"))
                }.onFailure {
                    it.printStackTrace()
                    call.respond(mapOf("error" to it.message))
                }
            }

            delete("/{email}/auth") {
                val email = call.parameters["email"] ?: return@delete call.respond(mapOf("error" to "Missing email"))
                runCatching {
                    val auth = FirebaseAuth.getInstance()
                    val authUid = auth.getUserByEmail(email).uid
                    auth.deleteUser(authUid)
                    call.respond(mapOf("status" to "success"))
                }.onFailure {
                    it.printStackTrace()
                    call.respond(mapOf("error" to it.message))
                }
            }
        }
    }.start(wait = true)
}
