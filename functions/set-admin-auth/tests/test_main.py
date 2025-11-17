from main import Request, handler


def test_handler():
    request: Request = {"user_id": "test-id"}

    response = handler(request)

    assert response == "test-id"