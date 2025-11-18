from http import HTTPStatus
import json
from typing import TypedDict
from werkzeug.wrappers import Request


class RequestData(TypedDict):
    user_id: str


def handler(request: Request):
    """Process the request to set admin authentication claim.
    Args:
        request: Request object containing the user_id assign the admin authentication claim to
    Returns:
        A JSON string with status and user_id
    """
    request_data: RequestData = request.get_json()
    return json.dumps({"status": HTTPStatus.OK, "data": request_data["user_id"]})
