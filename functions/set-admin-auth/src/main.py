from http import HTTPStatus
import json
from typing import TypedDict
from werkzeug.wrappers import Request
from firebase_admin.auth import set_custom_user_claims
import logging


class RequestData(TypedDict):
    user_id: str


class CustomAuthClaims(TypedDict):
    admin: bool


def handler(request: Request):
    """Process the request to set admin authentication claim.
    Args:
        request: Request object containing the user_id assign the admin authentication claim to
    Returns:
        A JSON string with status and user_id
    """
    request_data: RequestData = request.get_json()
    claims = CustomAuthClaims(admin=True)

    try:
        set_custom_user_claims(request_data["user_id"], claims)
        logging.debug(f"Set admin claims for user_id: {request_data['user_id']}")

        return json.dumps({"status": HTTPStatus.OK, "data": request_data["user_id"]})

    except Exception as e:
        logging.error(
            f"Error setting admin claims for user_id: {request_data['user_id']}: {e}"
        )
        return json.dumps({"status": HTTPStatus.INTERNAL_SERVER_ERROR, "error": "Failed to set claims"})
