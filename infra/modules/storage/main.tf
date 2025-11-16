resource "google_storage_bucket" "rewards_bucket" {
  provider                    = google-beta
  project                     = var.project_id
  name                        = "rewards_bucket-${var.project_id}"
  location                    = "EU"
  uniform_bucket_level_access = true
}

resource "google_firebase_storage_bucket" "rewards_bucket" {
  provider  = google-beta
  project   = var.project_id
  bucket_id = google_storage_bucket.rewards_bucket.name
}
