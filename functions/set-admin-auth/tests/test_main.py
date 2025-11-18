from unittest.mock import Mock, patch

from main import RequestData, handler
from werkzeug.wrappers import Request
from firebase_admin.exceptions import FirebaseError


@patch("main.set_custom_user_claims")
def test__handler__given_user_id__successfully_sets_admin_auth_claim(
    mock_set_custom_user_claims: Mock,
):
    request_data: RequestData = {"user_id": "test-id"}
    request: Request = Request.from_values(json=request_data)

    response = handler(request)

    mock_set_custom_user_claims.assert_called_once_with("test-id", {"admin": True})
    assert response == '{"status": 200, "data": "test-id"}'


@patch("main.set_custom_user_claims")
def test__handler__given_set_claims_failure__raises_exception(
    mock_set_custom_user_claims: Mock,
):
    request_data: RequestData = {"user_id": "test-id"}
    request: Request = Request.from_values(json=request_data)
    mock_set_custom_user_claims.side_effect = FirebaseError(
        code="error-code", message="Failed to set claims"
    )

    response = handler(request)

    mock_set_custom_user_claims.assert_called_once_with("test-id", {"admin": True})
    assert response == '{"status": 500, "error": "Failed to set claims"}'
