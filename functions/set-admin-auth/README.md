# Set user as admin

Utility to grant a user admin privileges by adding an `admin` claim to their auth token.

## Prerequisites
- GCP CLI installed with user privileges allowing for function execution

Running locally (script)

From `functions/set-admin-auth` run:

```bash
./scripts/set-admin.sh example-student@university.ac.uk
```

## Setup

Install dependencies using a virtualenv:

```bash
python -m venv venv
source venv/bin/activate
pip install -e .
```

## Testing

- Unit tests live in `functions/set-admin-auth/tests/` and can be run with `pytest`.

## Packaging

To package the function for deployment, run:

```bash
make zip
```

This creates a deployment-ready ZIP file of the function and will be used when at the `terraform apply` step.

