from http import HTTPStatus
import json
from typing import TypedDict
from werkzeug.wrappers import Request
from firebase_admin.auth import set_custom_user_claims, get_user_by_email, UserRecord
import logging


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
