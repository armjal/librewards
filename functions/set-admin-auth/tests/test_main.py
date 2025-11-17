from src.main import handler

def test_main():
    event = {
        "specversion": "1.0",
        "type": "com.example.someevent",
        "source": "/mycontext",
        "id": "A234-1234-1234",
        "time": "2020-08-23T09:00:00Z",
        "datacontenttype": "application/json",
        "data": {"message": "Hello World!"},
    }
    handler(event)
    assert True