package com.example.local_admin_server

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

fun main() {
    println("Starting LocalHelperServer...")
    val env = Dotenv.configure().ignoreIfMissing().load()
    println("Environment loaded.")

    runCatching {
        val creds = GoogleCredentials.getApplicationDefault()

        if (FirebaseApp.getApps().isEmpty()) {
            println("Initializing Firebase App...")
            FirebaseApp.initializeApp(
                FirebaseOptions.builder()
                    .setCredentials(creds)
                    .setDatabaseUrl(env["REALTIME_DB_URL"])
                    .setProjectId(env["GOOGLE_CLOUD_PROJECT"])
                    .setServiceAccountId(env["SERVICE_ACCOUNT_EMAIL"])
                    .build(),
            )
            println("Firebase Initialized Successfully.")
        } else {
            println("Firebase App already initialized.")
        }
    }.onFailure {
        println("Firebase init failed: ${it.message}")
        it.printStackTrace()
    }

    embeddedServer(Netty, 8080) {
        install(ContentNegotiation) { json() }
        routing {
            get("/") {
                println("Received GET /")
                call.respondText("OK")
            }

            post("/generate-token-for-admin") {
                val email = call.request.queryParameters["email"]
                println("Received POST /generate-token-for-admin with email: $email")
                if (email == null) {
                    println("Error: Missing email")
                    return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing email"))
                }

                runCatching {
                    val auth = FirebaseAuth.getInstance()
                    val uid = auth.getUserByEmail(email).uid
                    auth.setCustomUserClaims(uid, mapOf("admin" to true))

                    val token = auth.createCustomToken(uid, mapOf("admin" to true))
                    call.respond(mapOf("customToken" to token))
                }.onFailure {
                    println("Error generating token: ${it.message}")
                    it.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to it.message))
                }
            }

            post("/{uid}/update-user-field") {
                val uid = call.parameters["uid"]
                println("Received POST /:uid/update-user-field with uid: $uid")
                if (uid == null) return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing uid"))

                val text = call.receiveText()

                val json = Json.parseToJsonElement(text).jsonObject
                val field = json["field"]?.toString()?.trim('"')
                val value = json["value"]?.toString()?.trim('"')

                if (field == null) return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing field"))

                runCatching {
                    val db = FirebaseDatabase.getInstance()
                    val ref = db.getReference("users").child(uid).child(field)
                    ref.setValueAsync(value).get()
                    call.respond(mapOf("status" to "success"))
                }.onFailure {
                    println("Error updating user field: ${it.message}")
                    it.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to it.message))
                }
            }

            delete("/{uid}/user") {
                val uid = call.parameters["uid"]
                println("Received DELETE /:uid/user with uid: $uid")
                if (uid == null) return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing uid"))

                runCatching {
                    val db = FirebaseDatabase.getInstance()
                    db.getReference("users").child(uid).removeValueAsync().get()
                    call.respond(mapOf("status" to "success"))
                }.onFailure {
                    println("Error deleting user DB entry: ${it.message}")
                    it.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to it.message))
                }
            }

            delete("/{email}/auth") {
                val email = call.parameters["email"]
                println("Received DELETE /:email/auth with email: $email")
                if (email == null) return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing email"))

                runCatching {
                    val auth = FirebaseAuth.getInstance()
                    val authUid = auth.getUserByEmail(email).uid
                    auth.deleteUser(authUid)
                    call.respond(mapOf("status" to "success"))
                }.onFailure {
                    println("Error deleting auth user: ${it.message}")
                    it.printStackTrace()
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to it.message))
                }
            }
        }
    }.start(wait = true)
}
