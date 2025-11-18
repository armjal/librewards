from main import RequestData, handler
from werkzeug.wrappers import Request


def test_handler():
    request_data: RequestData = {"user_id": "test-id"}
    request: Request = Request.from_values(json=request_data)

    response = handler(request)

    assert response == '{"status": 200, "data": "test-id"}'
