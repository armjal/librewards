import json
import logging
from http import HTTPStatus
from typing import TypedDict

import firebase_admin
from firebase_admin import initialize_app
from firebase_admin.auth import UserRecord, get_user_by_email, set_custom_user_claims
from werkzeug.wrappers import Request


class RequestData(TypedDict):
    email: str


class CustomAuthClaims(TypedDict):
    admin: bool


def handler(request: Request):
    """Process the request to set admin authentication claim.
    Args:
        request: Request object containing a user's email to assign the admin authentication claim to
    Returns:
        A JSON string with status and user_id
    """

    _initialize_firebase_app()
    request_data: RequestData = request.get_json()
    claims = CustomAuthClaims(admin=True)
    user: UserRecord = get_user_by_email(request_data["email"])

    try:
        set_custom_user_claims(user.uid, claims)
        logging.debug(f"Set admin claims for user: {user.email}")

        return json.dumps({"status": HTTPStatus.OK, "data": user.uid})

    except Exception as e:
        logging.error(f"Error setting admin claims for user: {user.email}: {e}")
        return json.dumps(
            {
                "status": HTTPStatus.INTERNAL_SERVER_ERROR,
                "error": "Failed to set claims",
            }
        )


def _initialize_firebase_app():
    try:
        firebase_admin.get_app()
    except ValueError:
        initialize_app()
