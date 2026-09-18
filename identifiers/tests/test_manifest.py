"""The manifest endpoint, and the guard on what it is allowed to exempt."""

import importlib
import json
from collections.abc import Iterator
from types import ModuleType

import pytest
from conftest import AssertContract, Invoke
from openapi_core import OpenAPI

from adapters import handler

MANIFEST = "/management/manifest"
SCHEME = "ApiKeyAuth"
SHA = "7aacfdd974cfd315894c24ff831e3327aa3bb98b"


@pytest.fixture
def with_commit(monkeypatch: pytest.MonkeyPatch) -> Iterator[ModuleType]:
    """The handler as CI builds it, with a commit baked in.

    BUILD_COMMIT is read once at import, so the value has to be in place before
    the reload. The second reload restores the module for later tests, and runs
    inside the fixture so that monkeypatch has not yet undone the environment.
    """
    monkeypatch.setenv("BUILD_COMMIT", SHA)
    yield importlib.reload(handler)
    monkeypatch.undo()
    importlib.reload(handler)


def test_manifest_200_matches_the_spec(
    invoke: Invoke, assert_contract: AssertContract
) -> None:
    result = invoke(MANIFEST)
    assert_contract(result, "GET", MANIFEST, 200)


def test_manifest_reports_the_commit_baked_into_the_image(
    with_commit: ModuleType,
) -> None:
    """End to end from the environment variable CI sets to the response body."""
    body = json.loads(with_commit.handler({"resource": MANIFEST})["body"])
    assert body["commit"] == SHA
    assert body["startedAt"] == with_commit.STARTED_AT


def test_manifest_is_never_cached(invoke: Invoke) -> None:
    """A cache in front of this would keep reporting the previous deployment."""
    assert invoke(MANIFEST)["headers"]["Cache-Control"] == "no-store"


def test_commit_is_unknown_rather_than_absent_without_the_build_argument(
    invoke: Invoke,
) -> None:
    """A local build passes no BUILD_COMMIT, and the endpoint still answers.

    `unknown` rather than empty, matching ManifestRoute.scala and the concepts
    controller, so the same string means the same thing across the services.
    """
    body = json.loads(invoke(MANIFEST)["body"])
    assert body["commit"] == "unknown"


def test_only_the_manifest_is_reachable_without_an_api_key(
    openapi: OpenAPI,
) -> None:
    """The gateway is defined wholly by this spec, so this is the whole surface.

    Asserts the positive, that every operation but the manifest names ApiKeyAuth.
    The spec declares no default because API Gateway applies one to every method
    and does not honour an operation-level `security: []` as an override, which
    is how the manifest ended up needing a key. `security-defined` is off in
    redocly.yaml, so the lint will not catch a misspelled scheme.
    """
    spec = openapi.spec

    assert spec.get("security") is None, "the spec must declare no default security"

    # Defined as anything but a header API key, the gateway has no key to
    # enforce while every assertion below still passes. .get so a scheme with no
    # `in` or `name`, such as type: http, fails here rather than raising.
    scheme = spec["components"]["securitySchemes"][SCHEME]
    scheme_is_a_header_api_key = (
        scheme.get("type"),
        scheme.get("in"),
        scheme.get("name"),
    ) == ("apiKey", "header", "x-api-key")
    assert scheme_is_a_header_api_key, f"{SCHEME} must define a header API key"

    # Anything else under a path is an operation, including any-method routes.
    not_operations = {"parameters", "summary", "description", "servers", "$ref"}

    # .items(), because iterating a SchemaPath yields values rather than keys
    for path, operations in spec["paths"].items():
        for name, operation in operations.items():
            if name in not_operations:
                continue
            declared = operation.get("security")
            route = f"{str(name).upper()} {path}"

            if path == MANIFEST:
                # Per method, not per path: another method added here would
                # otherwise be exempted too, and be imported without a key.
                assert name == "get", f"{route} must require a key, only GET is open"
                assert not declared, f"{route} is the one route meant to be open"
            else:
                names = [list(requirement.keys()) for requirement in declared or []]
                requires_key = any(SCHEME in n for n in names)
                assert requires_key, f"{route} is readable without {SCHEME}"
