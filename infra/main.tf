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
  project_id = "librewards-2ea18"
  name       = "LibRewards"

  labels = {
    "firebase" = "enabled"
  }
}

resource "google_firebase_project" "firebase_project" {
  provider = google-beta
  project  = google_project.lib_rewards_project.project_id
}

resource "google_firebase_android_app" "app" {
  provider = google-beta

  project      = google_project.lib_rewards_project.project_id
  display_name = "LibRewards"
  package_name = "android.librewards"
}