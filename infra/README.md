# Infrastructure

Terraform (IaC) configuration to provision cloud resources used by the project.

## Quick Start
- Initialize and plan from the `infra` directory:
```
  cd infra
  terraform init
  terraform plan
  terraform validate
  terraform apply
```

### Modules
- `api_keys`: Creates and stores API keys and secrets needed by services.
- `auth`: Configures Firebase / identity resources and authentication settings.
- `database`: Provisions the Realtime Database used by the app.
- `functions`: Deploys Cloud Functions used for utilities and serverless tasks.
- `github_actions`: Service account and IAM bindings used by CI (GitHub Actions).
- `iam`: Centralized IAM roles and service accounts for the project.
- `project`: Top-level project settings, billing and project-level resources.
- `storage`: Cloud Storage buckets for product images and other assets.
