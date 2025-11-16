output "app_package_name" {
  description = "The package name of the Firebase Android app."
  value       = google_firebase_android_app.app.package_name
}

output "project_id" {
  description = "The project ID."
  value       = google_project.lib_rewards_project.project_id
}
