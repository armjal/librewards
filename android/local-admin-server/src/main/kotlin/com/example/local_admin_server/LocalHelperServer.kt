package com.example.local_admin_server

import com.example.local_admin_server.validation.CallExtensions.requireStringParameter
import com.example.local_admin_server.validation.CallExtensions.requireStringQueryParameter
import com.example.local_admin_server.validation.CreateProductRequest
import com.example.local_admin_server.validation.UpdateUserFieldRequest
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Bucket
import com.google.cloud.storage.Storage
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.cloud.StorageClient
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import io.github.cdimascio.dotenv.Dotenv
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.net.URLEncoder
import java.util.UUID

private lateinit var userDbRef: DatabaseReference
private lateinit var productDbRef: DatabaseReference
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
        defaultRoute()
        authRoutes()
        userRoutes()
        productRoutes()
    }
}

private fun Route.defaultRoute() {
    get("/") {
        println("Received GET /")
        call.respond(mapOf("status" to "success"))
    }
}

private fun Route.productRoutes() {
    post("/{university}/product") {
        println("Received POST /product")

        val createProductRequest = CreateProductRequest(call.parameters, call.receiveText())

        val objectName = "${createProductRequest.university}/images/${createProductRequest.name}"
        val token = UUID.randomUUID().toString()

        val blobInfo = BlobInfo.newBuilder(storageBucket.name, objectName)
            .setContentType("image/jpeg")
            .setMetadata(mapOf("firebaseStorageDownloadTokens" to token))
            .build()

        val blob = storageBucket.storage.create(
            blobInfo,
            createProductRequest.productImageBytes,
        )

        val encodedPath = URLEncoder.encode(blob?.name, "UTF-8")
        val downloadUrl =
            "https://firebasestorage.googleapis.com/v0/b/${StorageClient.getInstance().bucket().name}/o/$encodedPath?alt=media&token=$token"

        val productUniDbRef = productDbRef.child(createProductRequest.university)?.push()
        val productData = mapOf(
            "productName" to createProductRequest.name,
            "productCost" to createProductRequest.cost,
            "productImageUrl" to downloadUrl,
        )
        productUniDbRef?.setValueAsync(productData)?.get()
        call.respond(mapOf("status" to "success", "productId" to productUniDbRef?.key))
    }

    delete("/{university}/products") {
        val university = call.requireStringParameter("university")
        println("Received DELETE /:university/products with university: $university")

        val blobs = storageBucket.list(
            Storage.BlobListOption.prefix("$university/images/"),
        ).iterateAll()

        for (blob in blobs) {
            println("Deleting: ${blob.name}")
            blob.delete()
        }
        productDbRef.child(university).removeValueAsync().get()
        call.respond(mapOf("status" to "success"))
    }
}

private fun Route.userRoutes() {
    post("/{uid}/update-user-field") {
        val request = UpdateUserFieldRequest(call.parameters, call.receiveText())
        println("Received POST /:uid/update-user-field with uid: ${request.uid}")

        val usersDbRef = userDbRef.child(request.uid).child(request.field)
        usersDbRef.setValueAsync(request.value).get()
        call.respond(mapOf("status" to "success"))
    }

    delete("/{uid}/user") {
        val uid = call.requireStringParameter("uid")
        println("Received DELETE /:uid/user with uid: $uid")

        userDbRef.child(uid).removeValueAsync().get()
        call.respond(mapOf("status" to "success"))
    }
}

private fun Route.authRoutes() {
    post("/generate-token-for-admin") {
        val email = call.requireStringQueryParameter("email")
        println("Received POST /generate-token-for-admin with email: $email")

        val uid = authInstance.getUserByEmail(email).uid
        authInstance.setCustomUserClaims(uid, mapOf("admin" to true))

        val token = authInstance.createCustomToken(uid, mapOf("admin" to true))
        call.respond(mapOf("customToken" to token))
    }

    delete("/{email}/auth") {
        val email = call.requireStringParameter("email")
        println("Received DELETE /:email/auth with email: $email")

        val authUid = authInstance.getUserByEmail(email).uid
        authInstance.deleteUser(authUid)
        call.respond(mapOf("status" to "success"))
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

        val dbInstance = FirebaseDatabase.getInstance()
        userDbRef = dbInstance.getReference("users")
        productDbRef = dbInstance.getReference("products")
        storageBucket = StorageClient.getInstance().bucket()
        authInstance = FirebaseAuth.getInstance()
    }.onFailure {
        println("Firebase init failed: ${it.message}")
        it.printStackTrace()
    }
}
