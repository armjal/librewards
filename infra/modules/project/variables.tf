variable "project_id" {
  description = "The ID of the Google Cloud project."
  type        = string
}

variable "billing_account" {
  description = "The billing account to associate with the project."
  type        = string
}

variable "app_sha256_fingerprint" {
  description = "The SHA256 fingerprint of the Android app for App Check."
  type        = string
}
