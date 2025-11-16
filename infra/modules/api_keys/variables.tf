variable "project_id" {
  description = "The ID of the Google Cloud project."
  type        = string
}

variable "app_package_name" {
  description = "The package name of the Android app."
  type        = string
}

variable "app_sha1_fingerprint" {
  description = "The SHA1 fingerprint of the Android app."
  type        = string
}
