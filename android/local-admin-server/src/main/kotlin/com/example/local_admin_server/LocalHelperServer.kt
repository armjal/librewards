package com.example.local_admin_server

import com.example.local_admin_server.models.CreateProductRequest
import com.example.local_admin_server.models.UpdateUserFieldRequest
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.Bucket
import com.google.cloud.storage.Storage
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.cloud.StorageClient
import com.google.firebase.database.FirebaseDatabase
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages // Ensure this import exists
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.net.URLEncoder
import java.util.UUID

private lateinit var dbInstance: FirebaseDatabase
private lateinit var storageBucket: Bucket
private lateinit var authInstance: FirebaseAuth

fun Application.module() {
    println("Starting LocalHelperServer...")
    install(ContentNegotiation) { json() }
    install(IgnoreTrailingSlash)
    initialiseFirebase()
    setupRouting()
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            println("IllegalArgumentException caught: ${cause.message}")
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to (cause.message ?: "Invalid request")))
        }
        exception<Throwable> { call, cause ->
            println("Error caught: ${cause.message}")
            cause.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to cause.message))
        }
    }
}

private fun Application.setupRouting() {
    routing {
        get("/") {
            println("Received GET /")
            call.respondText("OK")
        }

        post("/{university}/product") {
            println("Received POST /product")

            val createProductRequest = CreateProductRequest(call.parameters, call.receiveText())

            val productImageBytes = java.util.Base64.getDecoder().decode(createProductRequest.imageBase64)
            val objectName = "${createProductRequest.university}/images/${createProductRequest.name}"

            val blob = storageBucket.create(
                objectName,
                productImageBytes,
                "image/jpeg",
            )

            val token = UUID.randomUUID().toString()
            blob.toBuilder()
                .setMetadata(mapOf("firebaseStorageDownloadTokens" to token))
                .build()
                .update()

            val encodedPath = URLEncoder.encode(blob?.name, "UTF-8")
            val downloadUrl =
                "https://firebasestorage.googleapis.com/v0/b/${
                    StorageClient.getInstance().bucket().name
                }/o/$encodedPath?alt=media&token=$token"

            val productDbRef = dbInstance.getReference("products")?.child(createProductRequest.university)?.push()
            val productData = mapOf(
                "productName" to createProductRequest.name,
                "productCost" to createProductRequest.cost,
                "productImageUrl" to downloadUrl,
            )
            productDbRef?.setValueAsync(productData)?.get()
            call.respond(mapOf("status" to "success", "productId" to productDbRef?.key))
        }

        delete("/{university}/products") {
            val university = call.parameters["university"]
            println("Received DELETE /:university/products with university: $university")

            if (university == null) return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing university"))

            val blobs = storageBucket.list(
                Storage.BlobListOption.prefix("$university/images/"),
            ).iterateAll()

            for (blob in blobs) {
                println("Deleting: ${blob.name}")
                blob.delete()
            }
            dbInstance.getReference("products").child(university).removeValueAsync().get()
            call.respond(mapOf("status" to "success"))
        }

        post("/generate-token-for-admin") {
            val email = call.request.queryParameters["email"]
            println("Received POST /generate-token-for-admin with email: $email")
            if (email == null) {
                println("Error: Missing email")
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing email"))
            }

            val uid = authInstance.getUserByEmail(email).uid
            authInstance.setCustomUserClaims(uid, mapOf("admin" to true))

            val token = authInstance.createCustomToken(uid, mapOf("admin" to true))
            call.respond(mapOf("customToken" to token))
        }

        post("/{uid}/update-user-field") {
            val request = UpdateUserFieldRequest(call.parameters, call.receiveText())
            println("Received POST /:uid/update-user-field with uid: ${request.uid}")

            val usersDbRef = dbInstance.getReference("users").child(request.uid).child(request.field)
            usersDbRef.setValueAsync(request.value).get()
            call.respond(mapOf("status" to "success"))
        }

        delete("/{uid}/user") {
            val uid = call.parameters["uid"]
            println("Received DELETE /:uid/user with uid: $uid")
            if (uid == null) return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing uid"))

            dbInstance.getReference("users").child(uid).removeValueAsync().get()
            call.respond(mapOf("status" to "success"))
        }

        delete("/{email}/auth") {
            val email = call.parameters["email"]
            println("Received DELETE /:email/auth with email: $email")
            if (email == null) return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing email"))

            val authUid = authInstance.getUserByEmail(email).uid
            authInstance.deleteUser(authUid)
            call.respond(mapOf("status" to "success"))
        }
    }
}

private fun initialiseFirebase() {
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
                    .setStorageBucket(env["PRODUCT_IMAGE_BUCKET_NAME"])
                    .build(),
            )
            println("Firebase Initialized Successfully.")
        } else {
            println("Firebase App already initialized.")
        }

        dbInstance = FirebaseDatabase.getInstance()
        storageBucket = StorageClient.getInstance().bucket()
        authInstance = FirebaseAuth.getInstance()
    }.onFailure {
        println("Firebase init failed: ${it.message}")
        it.printStackTrace()
    }
}
