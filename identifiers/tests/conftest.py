"""Contract-test fixtures: the Lambda handler validated against the spec.

The handler is invoked with synthesized API Gateway proxy events against the
seeded in-memory SQLite DB (no network, no external stub — the seed *is* the
fixture), and responses are validated against ``spec/openapi.yaml`` with
openapi-core, so the spec is what a response has to satisfy.
"""

import json
from pathlib import Path

import pytest
from openapi_core import OpenAPI
from openapi_core.testing import MockRequest, MockResponse

from adapters.handler import handler

SPEC_PATH = Path(__file__).resolve().parent.parent / "spec" / "openapi.yaml"

# Must match the first `servers` entry in the spec. The contract declares a
# production edge with no stage base path, so the base path is empty.
SERVER_HOST = "https://api.wellcomecollection.org"
SERVER_BASE_PATH = ""


def make_event(
    resource: str,
    path_params: dict | None = None,
    query: dict | None = None,
    headers: dict | None = None,
) -> dict:
    """Synthesize an API Gateway Lambda-proxy event for a GET."""
    path = resource
    for key, value in (path_params or {}).items():
        path = path.replace(f"{{{key}}}", value)
    return {
        "resource": resource,
        "path": path,
        "httpMethod": "GET",
        "pathParameters": path_params or None,
        "queryStringParameters": query or None,
        "headers": headers or None,
    }


@pytest.fixture
def invoke():
    """Call the handler with a synthesized event; return the response dict."""

    def call(resource, path_params=None, query=None, headers=None):
        return handler(make_event(resource, path_params, query, headers))

    return call


def body(result: dict) -> dict:
    """Parse a response's JSON body."""
    return json.loads(result["body"])


@pytest.fixture(scope="session")
def openapi() -> OpenAPI:
    return OpenAPI.from_file_path(str(SPEC_PATH))


@pytest.fixture
def assert_contract(openapi):
    """Validate a handler's response against the spec for (method, resource)."""

    def check(
        result: dict,
        method: str,
        resource: str,
        expected_status: int,
        query: dict | None = None,
    ):
        assert result["statusCode"] == expected_status, (
            f"expected {expected_status}, got {result['statusCode']}: "
            f"{result.get('body')!r}"
        )
        headers = result.get("headers") or {}
        content_type = headers.get("Content-Type", "text/plain")
        request = MockRequest(
            host_url=SERVER_HOST,
            method=method.lower(),
            path=SERVER_BASE_PATH + resource,
            args=query,
        )
        response = MockResponse(
            data=(result.get("body") or "").encode(),
            status_code=result["statusCode"],
            content_type=content_type,
        )
        openapi.validate_response(request, response)

    return check
