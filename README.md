# LibRewards

An Android application to reward students for their time spent at their university's library through a QR system. Students are able to have their QR code scanned at a library by a staff member to either start or stop their timer, or redeem a reward.

This project is a successor of the [librewards-offline](https://github.com/armjal/librewards-offline) project.

## How it works
### Admin account
1. A university will receive an invitation to join the application
2. A library staff member registers as normal through the application
3. An member of the Librewards team will be able to use the [set-admin-auth](functions/set-admin-auth) function to set the library staff member's user account to admin status
4. The staff member will be able to login as normal through the application and be navigated to the admin interface
5. In this interface, the admin will be able to:
   - Manage university library rewards
   - Scan a student's QR code to start/stop their timer
   - Scan a student's QR code to redeem a reward

### Student account
1. A student of a university that is using Librewards will be able to register through the application as normal
2. Once navigate to the main interface, the student has options to:
   - Start/stop their timer by having their QR code scanned by a library staff member
   - Redeem a reward from their university library by having their QR code scanned by a library staff member

### Points calculation
Points are earned based on the duration a student spends at the library. When a student stops their timer, the elapsed time is retrieved and points are calculated base on that. The following **arbitrary** values are what currently exists in code:

| Time Spent | Points Earned |
|------------|---------------|
| 0 – 10 seconds | 0 |
| 10 – 30 seconds | 10 |
| 30 – 60 seconds | 50 |
| 1 – 2 minutes (60 – 120 seconds) | 75 |
| 2 – 3 minutes (120 – 180 seconds) | 125 |
| 3 – 4.3 minutes (180 – 260 seconds) | 225 |
| 4.3 – 6.6 minutes (260 – 400 seconds) | 400 |
| 6.6+ minutes (400+ seconds) | 700 |

Should the timer exceed 24 hours, the timer resets and no points are earned.

### Distance from library calculation
To encourage time-spent at the library, upon having their QR code scanned, a circle appears on the student's map which is:
- Blue - the student is within a radius of 40* metres from the scanned location. This means they are within a valid studying zone. 
- Red - the student is between 40* and 60* metres from the scanned location. The student should go back to the valid studying zone.
If the student goes beyond a 60* metre radius, the circle is removed, timer reset, and no points earned - indicating they have left the library.

\***Metres used are arbitrary values and can be changed in code**

### Managing rewards

Within the admin interface, library staff can manage reward products for their university. The product management features are located in the **Rewards** tab of the admin app and include:

**Adding a new product:**
1. Click the `Add a Product` button
2. Select a product image from the device's gallery
3. Enter the product name and point cost (the cost in points for students to redeem)
4. Click `Upload` - the image is sent to Cloud Storage and the product details are saved to the database

**Viewing products:**
- A list of all products for the university is displayed as a scrollable list
- Each product shows its image, name, and cost

**Updating a product:**
1. Click on any product in the list to open the manage dialog
2. Edit the product name and/or cost as desired
3. Click `Update` to save changes to the database

**Deleting a product:**
1. Click on any product in the list to open the manage dialog
2. Click `Delete` to remove the product from the database

Products are scoped to each university, so admins and students can only see products within their own institution.

**Technical details:**
- Product metadata (name, cost, image URL) is stored in Firebase Realtime Database under `products/{university}/{productId}`
- Product images are stored in Cloud Storage under `{university}/images/{productName}`
- When a product is uploaded, the image is assigned a unique download token for public access via URL

## Project contents

- `android/` - Android app module (primary mobile client).
  - `app/` - The Android application module containing source code, resources, and Gradle configuration.
  - `local-admin-server/` - Local server to facilitate Firebase operations for setup/teardown of integration tests
- `functions/` - Small utility functions for Librewards
- `infra/` - Terraform modules and configuration used to provision Firebase, and GCP resources.

## Technical stack & Architecture

Librewards is built on a modern, cloud-native stack that enables real-time synchronization, scalable authentication, and sophisticated location-based features. This represents a significant evolution from its predecessor.

### Current tech stack

**Mobile (Android):**
- **Kotlin** - Primary language for the Android app, replacing Java from the predecessor. 
- **Google Maps API** - Provides interactive maps with real-time location visualization.
- **Fused Location Client** - Google Play Services API for efficient, accurate location updates (high-accuracy, 2-second polling, 20m minimum distance).
- **Google Barcode Scanner** - Built-in ML Kit integration for scanning QR codes.
- **ZXing Library** - QR code generation for student IDs, enabling contactless scanning workflows.

**Backend & Infrastructure:**
- **GCP (Google Cloud Platform)** - Underlying infrastructure:
  - Firebase Realtime Database
  - Firebase Authentication
  - Firebase Storage
  - Cloud Functions
  - Identity & Access Management
- **Firebase Authentication** - Cloud-based user authentication with email/password and custom token support for admins.
- **Firebase Realtime Database** - Allows for real-time synchronisation of user and product data across all users within the same university. This is a key service to ensure there are no delays when a student wants to toggle their timer, or to redeem a reward.
- **Firebase Storage** - Stores reward product images with public download URLs.
- **Cloud Functions** - Allows for utility functions to be created to support the functionality of the app.
- **Terraform (Infrastructure as Code)** -  Provisions and manages all GCP resources

### Evolution from predecessor

The predecessor, [librewards-offline](https://github.com/armjal/librewards-offline), was a standalone offline-first application with fundamental limitations:

| Aspect | Predecessor (librewards-offline) | LibRewards (Current) |
|--------|----------------------------------|----------------------|
| **Language** | Java | Kotlin 
| **Data storage** | SQLite (local database) | Firebase Realtime Database |
| **User management** | None; name stored in local DB | Firebase Authentication |
| **Timer management** | Manual code entry (hardcoded start/stop codes) | QR code scanning |
| **Rewards management** | Hardcoded reward codes | Cloud Storage + dynamic product management |
| **Rewards redemption** | Manual code entry (hardcoded reward codes) | QR code scanning 
| **Location tracking** | None | Fused Location Client |
| **Map visualization** | None | Google Maps API with real-time circles and markers |
| **Access control** | None | Firebase custom claims + IAM service accounts |
| **Infrastructure** | Android only | GCP & Infrastructure-as-Code (Terraform) |
| **Testing** | Unit & Instrumented | Unit & Instrumented integration (with backend server) |

## Tests

- Unit and instrumented integration tests live under `android/app/src/test` and `android/app/src/androidTest` respectively. Run them with Gradle tasks:
  - Unit tests: `cd android; .\gradlew test`
  - Instrumentation tests:
      ```
      cd android
      .\gradlew :local-admin-server:run
      # Wait for server to start on http://localhost:8080/
      .\gradlew connectedAndroidTest
      ```
### Continuous Integration Pipeline

The CI pipeline (GitHub Actions) runs both unit and integration tests automatically with the following workflow:

1. Build the local admin server
2. Start it in the background on `localhost:8080`
3. Wait for server readiness (polling `GET /` endpoint)
4. Launch the Android emulator
5. Run `./gradlew connectedCheck` to execute all instrumented tests

GitHub Actions has its own dedicated service account as shown in the [github-actions module](infra/modules/github_actions). With the permissions defined in IaC, this allows the CI pipeline to manage resources in database, storage, and auth services while performing integration tests.