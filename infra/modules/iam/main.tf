resource "random_id" "service_account" {
  byte_length = 4
}

resource "google_service_account" "auth" {
  account_id   = "auth-account-${random_id.service_account.hex}"
  description  = "Service account for Firebase authentication administration"
  display_name = "firebase-auth-admin"
  project = var.project_id
}

resource "google_project_iam_member" "service_account_roles" {
  for_each = toset([
    "roles/firebase.admin",
    "roles/serviceusage.serviceUsageConsumer"
  ])

  project = var.project_id
  role    = each.value
  member  = "serviceAccount:${google_service_account.service_account.email}"
}
