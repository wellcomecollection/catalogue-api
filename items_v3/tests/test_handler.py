"""The handler against the contract in spec/openapi.yaml."""

import importlib
import json
from collections.abc import Iterator
from pathlib import Path
from typing import Any

import pytest
from openapi_core import OpenAPI
from openapi_core.testing import MockRequest, MockResponse

import handler

SPEC_PATH = Path(__file__).resolve().parent.parent / "spec" / "openapi.yaml"
SERVER = "https://api.wellcomecollection.org"

ITEMS = "/v3/works/{workId}/items"
MANIFEST = "/management/items-v3/manifest"

WORK_ID = "a2222222"
SHA = "0123456789abcdef0123456789abcdef01234567"


def invoke(resource: str, work_id: str | None = None) -> dict[str, Any]:
    """Call the handler with a proxy event."""
    event = {
        "resource": resource,
        "httpMethod": "GET",
        "pathParameters": {"workId": work_id} if work_id is not None else None,
    }
    return handler.handler(event)


@pytest.fixture(scope="session")
def openapi() -> OpenAPI:
    return OpenAPI.from_file_path(str(SPEC_PATH))


def assert_contract(openapi: OpenAPI, result: dict[str, Any], path: str) -> None:
    """The response is one the spec allows for a GET of this path."""
    openapi.validate_response(
        MockRequest(host_url=SERVER, method="get", path=f"/catalogue{path}"),
        MockResponse(
            data=result["body"].encode(),
            status_code=result["statusCode"],
            content_type=result["headers"]["Content-Type"],
            headers=result["headers"],
        ),
    )


def items_path(work_id: str) -> str:
    return ITEMS.replace("{workId}", work_id)


def test_items_are_not_served_yet(openapi: OpenAPI) -> None:
    result = invoke(ITEMS, WORK_ID)

    assert result["statusCode"] == 500
    assert_contract(openapi, result, items_path(WORK_ID))


@pytest.mark.parametrize(
    "work_id", ["", "A2222222", "a222222", "a2222222x", "a222222l"]
)
def test_a_malformed_id_is_404(work_id: str) -> None:
    assert invoke(ITEMS, work_id)["statusCode"] == 404


def test_an_unrouted_resource_is_404() -> None:
    assert invoke("/v3/works/{workId}")["statusCode"] == 404


def test_manifest_matches_the_spec_and_is_never_cached(openapi: OpenAPI) -> None:
    result = invoke(MANIFEST)

    assert result["statusCode"] == 200
    assert result["headers"]["Cache-Control"] == "no-store"
    assert json.loads(result["body"])["commit"] == "unknown"
    assert_contract(openapi, result, MANIFEST)


@pytest.fixture
def with_commit(monkeypatch: pytest.MonkeyPatch) -> Iterator[Any]:
    """The handler as CI builds it. BUILD_COMMIT is read once, at import."""
    monkeypatch.setenv("BUILD_COMMIT", SHA)
    yield importlib.reload(handler)
    monkeypatch.undo()
    importlib.reload(handler)


def test_manifest_reports_the_commit_baked_into_the_image(with_commit: Any) -> None:
    body = json.loads(with_commit.handler({"resource": MANIFEST})["body"])

    assert body == {"commit": SHA, "startedAt": with_commit.STARTED_AT}
