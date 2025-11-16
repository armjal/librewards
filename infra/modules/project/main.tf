resource "google_project" "lib_rewards_project" {
  provider        = google-beta
  project_id      = var.project_id
  name            = "LibRewards"
  billing_account = var.billing_account

  labels = {
    "firebase" = "enabled"
  }
}

resource "google_project_service" "default" {
  provider = google-beta
  project  = google_project.lib_rewards_project.project_id
  for_each = toset([
    "serviceusage.googleapis.com",
    "cloudresourcemanager.googleapis.com",
    "identitytoolkit.googleapis.com",
    "firebasedatabase.googleapis.com",
    "firebasestorage.googleapis.com",
    "storage.googleapis.com",
    "firebaserules.googleapis.com",
    "maps-android-backend.googleapis.com",
    "apikeys.googleapis.com"
  ])
  service = each.key
}

resource "google_firebase_project" "firebase_project" {
  provider = google-beta
  project  = var.project_id
}

resource "google_firebase_android_app" "app" {
  provider     = google-beta
  project      = google_firebase_project.firebase_project.project
  display_name  = "LibRewards"
  package_name  = "android.librewards"
}
