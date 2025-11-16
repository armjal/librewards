terraform {
  required_providers {
    google-beta = {
      source = "hashicorp/google-beta"
    }
  }
}

resource "google_apikeys_key" "android_maps_key" {
  provider     = google-beta
  name         = "android-maps-api-key"
  display_name = "Maps SDK for Android Key"
  project      = var.project_id

  restrictions {
    api_targets {
      service = "maps-android-backend.googleapis.com"
    }

    android_key_restrictions {
      allowed_applications {
        package_name     = var.app_package_name
        sha1_fingerprint = var.app_sha1_fingerprint
      }
    }
  }
}
