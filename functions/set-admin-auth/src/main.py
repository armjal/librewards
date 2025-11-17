
from typing import TypedDict


class Request(TypedDict):
    user_id: str

def handler(request: Request):
    """Process the request to set admin authentication claim.
    Args:
        request: Request object containing the user_id assign the admin authentication claim to
    """
    return request['user_id']
