output "workload_identity_provider_name" {
  description = "The Workload Identity Provider resource name"
  value       = google_iam_workload_identity_pool_provider.github_provider.name
}

output "github_actions_service_account_email" {
  description = "The email of the Service Account created for GitHub Actions"
  value       = google_service_account.github_actions_sa.email
}
