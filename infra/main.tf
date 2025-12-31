terraform {
  required_providers {
    google-beta = {
      source  = "hashicorp/google-beta"
      version = "~> 6.0"
    }
  }
}

provider "google-beta" {
  user_project_override = true
  billing_project       = var.project_id
}

module "project" {
  source                 = "./modules/project"
  project_id             = var.project_id
  billing_account        = var.billing_account
  app_sha256_fingerprint = var.app_sha256_fingerprint
}

module "auth" {
  source     = "./modules/auth"
  project_id = module.project.project_id
}

module "database" {
  source     = "./modules/database"
  project_id = module.project.project_id
}

module "storage" {
  source     = "./modules/storage"
  project_id = module.project.project_id
}

module "api_keys" {
  source               = "./modules/api_keys"
  project_id           = module.project.project_id
  app_package_name     = module.project.app_package_name
  app_sha1_fingerprint = var.app_sha1_fingerprint
}

module "functions" {
  source                      = "./modules/functions"
  project_id                  = module.project.project_id
  admin_service_account_email = module.iam.auth_service_account_email
}

module "iam" {
  source     = "./modules/iam"
  project_id = module.project.project_id
}

module "github_actions" {
  source      = "./modules/github_actions"
  project_id  = module.project.project_id
  github_repo = var.github_repo
}

output "android_maps_api_key_string" {
  value     = module.api_keys.android_maps_api_key_string
  sensitive = true
}

output "workload_identity_provider_name" {
  value = module.github_actions.workload_identity_provider_name
}

output "github_actions_service_account_email" {
  value = module.github_actions.github_actions_service_account_email
}
