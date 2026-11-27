"""The SSM credentials provider and the per-container client."""

import json
from typing import Any

import pytest

import runtime


class FakeSsm:
    def __init__(self, value: dict[str, str]):
        self.value = value
        self.requests: list[dict[str, Any]] = []

    def get_parameter(self, **kwargs: Any) -> dict[str, Any]:
        self.requests.append(kwargs)
        return {"Parameter": {"Value": json.dumps(self.value)}}


def test_ssm_credentials_reads_the_decrypted_parameter() -> None:
    ssm = FakeSsm({"username": "svc", "password": "secret"})

    provide = runtime.ssm_credentials("/items/okapi_credentials", lambda: ssm)

    assert provide() == ("svc", "secret")
    assert ssm.requests == [
        {"Name": "/items/okapi_credentials", "WithDecryption": True}
    ]


def test_folio_client_is_one_per_container(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("OKAPI_URL", "https://okapi.test.invalid/")
    monkeypatch.setenv("OKAPI_TENANT", "test")
    monkeypatch.setenv("OKAPI_SECRET_PARAM", "/items/okapi_credentials")
    runtime.folio_client.cache_clear()

    client = runtime.folio_client()

    assert runtime.folio_client() is client
    assert client.base_url == "https://okapi.test.invalid"
    assert client.timeout == runtime.FOLIO_TIMEOUT_SECONDS
    runtime.folio_client.cache_clear()
