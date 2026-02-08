#!/bin/bash
# Usage: ./set-admin.sh <email>

EMAIL=$1

gcloud functions call set-admin-auth \
  --region=europe-west2 \
  --data='{
    "email": "'$EMAIL'"
  }' \
  --gen2
