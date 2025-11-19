from unittest.mock import Mock, patch

from firebase_admin.exceptions import FirebaseError
from werkzeug.wrappers import Request

from main import RequestData, handler


@patch("main.set_custom_user_claims")
@patch("main.get_user_by_email")
def test__handler__given_user_email__successfully_sets_admin_auth_claim(
    mock_get_user_by_email: Mock,
    mock_set_custom_user_claims: Mock,
):
    mock_get_user_by_email.return_value = Mock(uid="test-id", email="test-id@test.com")
    request_data: RequestData = {"email": "test-id@test.com"}
    request: Request = Request.from_values(json=request_data)

    response = handler(request)

    mock_set_custom_user_claims.assert_called_once_with("test-id", {"admin": True})
    assert response == '{"status": 200, "data": "test-id"}'


@patch("main.set_custom_user_claims")
@patch("main.get_user_by_email")
def test__handler__given_set_claims_failure__raises_exception(
    mock_get_user_by_email: Mock,
    mock_set_custom_user_claims: Mock,
):
    mock_get_user_by_email.return_value = Mock(uid="test-id", email="test-id@test.com")
    request_data: RequestData = {"email": "test-id@test.com"}
    request: Request = Request.from_values(json=request_data)
    mock_set_custom_user_claims.side_effect = FirebaseError(code="error-code", message="Failed to set claims")

    response = handler(request)

    mock_set_custom_user_claims.assert_called_once_with("test-id", {"admin": True})
    assert response == '{"status": 500, "error": "Failed to set claims"}'
