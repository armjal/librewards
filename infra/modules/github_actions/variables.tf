variable "project_id" {
  description = "The ID of the Google Cloud project"
  type        = string
}

variable "github_repo" {
  description = "The GitHub repository (format: owner/repo)"
  type        = string
}

variable "service_account_email" {
  description = "The existing Service Account email to attach policies to"
  type        = string
}
