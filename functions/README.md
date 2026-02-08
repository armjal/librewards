# Utility Functions

A set of utility functions to assist with the functions of the application. Each function lives in a subdirectory (for example `set-admin-auth`).

## Quick Start (creating a function)
This project uses a Terraform module located at [`infra/modules/functions`](infra\modules\functions) to provision Cloud Functions. When adding a new function you will:

1. Add a new subdirectory under `functions/` (for example `functions/my-function`).
2. Implement the function source code and tests in that directory.
3. Pass the function path to the Terraform resource and include configuration values.

### Files to include inside a function directory

A minimal Python function layout looks like:

- `pyproject.toml` for dependency manifest and project metadata (this repo uses `pyproject.toml` in existing functions).
- `src/main.py`: the function entrypoint.
- `tests/test_main.py`: unit tests for the function logic.
- `scripts/` or `Makefile`: convenience scripts for local testing and packaging.
- `README.md`: short notes on what the function does and how to run it locally.
