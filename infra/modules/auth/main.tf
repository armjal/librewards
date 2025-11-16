resource "google_identity_platform_config" "default" {
  provider = google-beta
  project  = var.project_id
  sign_in {
    email {
      enabled           = true
      password_required = true
    }
  }
}
