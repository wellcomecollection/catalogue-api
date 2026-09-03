"""Forward lookup responses validated against the spec."""

from typing import NoReturn

import pytest
from conftest import AssertContract, Invoke

from adapters import handler

FORWARD = "/v1/identifiers/{canonicalId}"


def test_forward_200_matches_identifier_set(
    invoke: Invoke, assert_contract: AssertContract
) -> None:
    result = invoke(FORWARD, {"canonicalId": "a2345bcd"})
    assert_contract(result, "GET", FORWARD, 200)


def test_forward_single_source_200(
    invoke: Invoke, assert_contract: AssertContract
) -> None:
    result = invoke(FORWARD, {"canonicalId": "mn23pqrs"})
    assert_contract(result, "GET", FORWARD, 200)


def test_forward_404_matches_error(
    invoke: Invoke, assert_contract: AssertContract
) -> None:
    result = invoke(FORWARD, {"canonicalId": "abcdefgh"})
    assert_contract(result, "GET", FORWARD, 404)


def test_forward_400_matches_error(
    invoke: Invoke, assert_contract: AssertContract
) -> None:
    result = invoke(FORWARD, {"canonicalId": "zzz"})
    assert_contract(result, "GET", FORWARD, 400)


def test_forward_500_matches_error(
    invoke: Invoke, assert_contract: AssertContract, monkeypatch: pytest.MonkeyPatch
) -> None:
    def unavailable(_: str) -> NoReturn:
        raise TimeoutError("RDS Data API timed out")

    monkeypatch.setattr(handler._repo, "get_by_canonical", unavailable)
    result = invoke(FORWARD, {"canonicalId": "a2345bcd"})
    assert_contract(result, "GET", FORWARD, 500)
