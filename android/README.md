# Android App

Contains the Android application for Librewards (primary module is `app/`).

## Project structure and overview

### Directory layout

**Root level (`android/`):**
- `app/` - main Android module containing the Librewards application source code, resources, and Gradle configuration.
- `local-admin-server/` - local Ktor-based server to facilitate Firebase operations for integration testing.
- `gradle/` - Gradle wrapper and configuration.
- `build.gradle`, `settings.gradle` - Gradle build and project configuration.

### App module structure (`android/app/src/main`)

- `java/` - Kotlin/Java sources for activities, fragments, and view models (organized with MVVM architecture).
- `res/layout/` - XML layouts for activities and fragments (e.g., `activity_main.xml`, `activity_login.xml`, `fragment_timer.xml`).
- `res/drawable/` - Images and vector drawables used by the UI (e.g., gradients, icons, rounded shapes).
- `res/values/` - Colors, styles, dimensions, and string resources.
- `AndroidManifest.xml` - App manifest (permissions, activities, intent filters).

### Architecture and development notes

- **MVVM Pattern**: The modules are organized and implemented with a Model-View-ViewModel (Activities/Fragments + ViewModels) approach in mind.
- **Testing**: Unit tests are in `src/test`, instrumentation/integration tests are in `src/androidTest`.

## Testing

Comprehensive unit and instrumented integration tests have been implemented to validate core app functionality across both student and admin user flows.

### Overview

- **Unit Tests** (`android/app/src/test`): Test individual ViewModels, repositories, and utility functions in isolation.
- **Instrumented Integration Tests** (`android/app/src/androidTest`): Test app flows, communication between live components (Firebase, GCP), and core features (location updates, timer functionality, reward management, and product operations).

**Note**: The test suite was primarily developed with Gemini within Android Studio.

### Why a local admin server?

Integration tests needs dummy data in order to successfully test flows; especially admin paths where promotion of a user is done by a Librewards developer. To enable this, a **local admin server** was implemented in Kotlin using the Ktor framework. This server:

- Runs on `localhost:8080` and creates a local network bridge accessible to the Android emulator
- Provides endpoints to create/delete test users, upload products, and manage Firebase state
- Requires to be started before integration tests run and stopped afterward

**Note**: At the moment, there is only one live environment that integration tests use for setup/teardown. This can later be amended to isolate a production environment.

### Running tests

**Unit tests only:**
```
cd android
.\gradlew test
```

**Full integration test suite (including local admin server):**

From the repository root, run:

```
cd android
.\gradlew :local-admin-server:run
# Wait for server to start on http://localhost:8080/
.\gradlew connectedAndroidTest
```

See [root README](../README.md#continuous-integration-pipeline) for details on how the CI pipeline orchestrates these tests.

**Test coverage includes:**
- User authentication (login, registration)
- Timer start/stop with points calculation
- Location tracking and distance-based circle color changes (including leaving the study zone)
- Map interactions and marker updates
- Product management (add, update, delete) for admins
- Reward redemption flows
- 24-hour timer reset

### Linting
```
./gradlew ktlint
```