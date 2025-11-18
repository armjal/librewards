resource "google_storage_bucket" "default" {
  provider = google-beta

  name                        = "${var.project_id}-gcf-source"
  project                     = var.project_id
  location                    = "EU"
  uniform_bucket_level_access = true
}

resource "google_storage_bucket_object" "object" {
  provider = google-beta

  name   = "set-admin-auth.zip"
  bucket = google_storage_bucket.default.name
  source = "../functions/set-admin-auth/function.zip"
}

resource "google_cloudfunctions2_function" "default" {
  provider = google-beta

  name        = "set-admin-auth"
  location    = "europe-west2"
  description = "A function to set admin auth claims for a user in LibRewards."
  project     = var.project_id

  build_config {
    runtime     = "python313"
    entry_point = "handler"

    environment_variables = {
      GOOGLE_PYTHON_PACKAGE_MANAGER = "uv"
    }
    source {
      storage_source {
        bucket = google_storage_bucket.default.name
        object = google_storage_bucket_object.object.name
        generation = google_storage_bucket_object.object.generation
      }
    }
  }

  service_config {
    max_instance_count = 1
    available_memory   = "256M"
    timeout_seconds    = 60
  }
}