# Terraform configuration to set up providers by version.
terraform {
  required_providers {
    google-beta = {
      source  = "hashicorp/google-beta"
      version = "~> 6.0"
    }
  }
}

# Configures the provider to use the resource block's specified project for quota checks.
provider "google-beta" {
  user_project_override = true
}

resource "google_project" "lib_rewards_project" {
  provider        = google-beta
  project_id      = "librewards-2ea18"
  name            = "LibRewards"
  billing_account = "0147E8-8D8F4A-3B7036"

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
  ])
  service = each.key
}

resource "google_firebase_project" "firebase_project" {
  provider = google-beta
  project  = google_project.lib_rewards_project.project_id
}

resource "google_firebase_android_app" "app" {
  provider     = google-beta
  project      = google_project.lib_rewards_project.project_id
  display_name = "LibRewards"
  package_name = "android.librewards"
}

#Sets up Authentication
resource "google_identity_platform_config" "default" {
  provider = google-beta
  project  = google_project.lib_rewards_project.project_id
  sign_in {
    email {
      enabled           = true
      password_required = true
    }
  }
}

# Provisions the default Realtime Database default instance.
resource "google_firebase_database_instance" "database" {
  provider    = google-beta
  project     = google_firebase_project.firebase_project.project
  region      = "europe-west1"
  instance_id = "${google_project.lib_rewards_project.project_id}-default-rtdb"
  type        = "DEFAULT_DATABASE"
}