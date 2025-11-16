output "android_maps_api_key_string" {
  description = "The string value of the Android Maps API key."
  value       = google_apikeys_key.android_maps_key.key_string
  sensitive   = true
}
