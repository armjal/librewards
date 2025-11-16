resource "google_storage_bucket" "default" {
  provider = google-beta

  name                        = "${var.project_id}-gcf-source"
  project                     = var.project_id
  location                    = "EU"
  uniform_bucket_level_access = true
}

data "archive_file" "default" {
  type        = "zip"
  output_path = "../set-admin-auth.zip"
  source_dir  = "../functions/set-admin-auth"
}

resource "google_storage_bucket_object" "object" {
  provider = google-beta

  name   = "set-admin-auth.zip"
  bucket = google_storage_bucket.default.name
  source = data.archive_file.default.output_path
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
    source {
      storage_source {
        bucket = google_storage_bucket.default.name
        object = google_storage_bucket_object.object.name
      }
    }
  }

  service_config {
    max_instance_count = 1
    available_memory   = "256M"
    timeout_seconds    = 60
  }
}