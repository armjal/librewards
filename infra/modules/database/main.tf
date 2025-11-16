resource "google_firebase_database_instance" "database" {
  provider    = google-beta
  project     = var.project_id
  region      = "europe-west1"
  instance_id = "${var.project_id}-default-rtdb"
  type        = "DEFAULT_DATABASE"
}