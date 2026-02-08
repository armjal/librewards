# Local Admin Server

Lightweight Ktor (Kotlin) local admin server for setting up/tearing down resources for integration testing. 

## Quick Start
From the `local-admin-server` directory:

### Running the server
```
./gradlew :local-admin-server:run
```

### Linting
```
./gradlew :local-admin-server:ktlint
```

## Endpoints
- `GET /` : Health/status endpoint that returns a simple success object.
- `POST /{university}/product` : Uploads a product (image + metadata) for the specified university and returns the new productId.
- `DELETE /{university}/products` : Deletes all product images and product records for the given university.
- `POST /{uid}/update-user-field` : Updates a single user field for the user with the specified user ID.
- `DELETE /{uid}/user` : Removes the user record for the specified user ID from the database.
- `POST /generate-token-for-admin?email=<email>` : Grants admin claims to the user with the given email and returns a custom token.
- `DELETE /{email}/auth` : Deletes the Firebase auth user with the specified email.

## Config
- Configuration is located at `src/main/resources/application.conf`.

### Required environment variables
- `REALTIME_DB_URL` : Firebase Realtime Database URL used by the server.
- `GOOGLE_CLOUD_PROJECT` : GCP project id for Firebase integration.
- `SERVICE_ACCOUNT_EMAIL` : Service account email that has correct Firebase permissions used to initialize Firebase credentials.
- `PRODUCT_IMAGE_BUCKET_NAME` : Cloud Storage bucket name for uploaded product images.
