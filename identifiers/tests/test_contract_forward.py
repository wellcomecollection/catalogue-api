"""Forward lookup responses validated against the spec."""

from typing import NoReturn

import pytest
from conftest import AssertContract, Invoke, body

from adapters import handler

FORWARD = "/identifiers/v1/{canonicalId}"


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


def test_forward_concept_only_is_404(
    invoke: Invoke, assert_contract: AssertContract
) -> None:
    result = invoke(FORWARD, {"canonicalId": "cn234567"})
    assert_contract(result, "GET", FORWARD, 404)


def test_forward_concept_original_is_404_despite_work_alias(
    invoke: Invoke, assert_contract: AssertContract
) -> None:
    result = invoke(FORWARD, {"canonicalId": "cp234567"})
    assert_contract(result, "GET", FORWARD, 404)


def test_forward_omits_concept_alias(
    invoke: Invoke, assert_contract: AssertContract
) -> None:
    result = invoke(FORWARD, {"canonicalId": "cw234567"})
    assert_contract(result, "GET", FORWARD, 200)
    payload = body(result)
    assert payload["type"] == "Work"
    assert [
        (r["type"], r["sourceSystem"], r["isAlias"])
        for r in payload["sourceIdentifiers"]
    ] == [
        ("Work", "axiell-collections-id", True),
        ("Work", "sierra-system-number", False),
    ]
    # Built from the returned rows only: 2 rows, latest 2026-02-10, not the
    # omitted Concept row's 2026-03-01.
    assert result["headers"]["ETag"] == 'W/"2-2026-02-10T12:00:00Z"'


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
