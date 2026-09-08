"""Reverse lookup responses validated against the spec.

Covers both arms of the 200 `oneOf`: the bare CanonicalIdRef and the
include=siblings IdentifierSet.
"""

import pytest
from conftest import AssertContract, Invoke

from adapters import handler

REVERSE = "/v1/identifiers/by-source/{sourceSystem}/{value}"


def test_reverse_bare_200_matches_canonical_ref(
    invoke: Invoke, assert_contract: AssertContract
) -> None:
    result = invoke(
        REVERSE,
        {"sourceSystem": "sierra-system-number", "value": "b1161044x"},
        {"type": "Work"},
    )
    assert_contract(result, "GET", REVERSE, 200, query={"type": "Work"})


def test_reverse_siblings_200_matches_identifier_set(
    invoke: Invoke, assert_contract: AssertContract
) -> None:
    result = invoke(
        REVERSE,
        {"sourceSystem": "sierra-system-number", "value": "b1161044x"},
        {"type": "Work", "include": "siblings"},
    )
    assert_contract(
        result, "GET", REVERSE, 200, query={"type": "Work", "include": "siblings"}
    )


def test_reverse_default_type_is_work(invoke: Invoke) -> None:
    # No `type` query param -> defaults to Work; Work tuple resolves.
    result = invoke(
        REVERSE, {"sourceSystem": "sierra-system-number", "value": "b1161044x"}
    )
    assert result["statusCode"] == 200


def test_reverse_unknown_tuple_404(
    invoke: Invoke, assert_contract: AssertContract
) -> None:
    result = invoke(
        REVERSE,
        {"sourceSystem": "sierra-system-number", "value": "does-not-exist"},
        {"type": "Work"},
    )
    assert_contract(result, "GET", REVERSE, 404, query={"type": "Work"})


def test_reverse_wrong_type_is_404_not_400(invoke: Invoke) -> None:
    # The tuple exists as Work, not Image -> no mapping for (Image, ...) -> 404.
    result = invoke(
        REVERSE,
        {"sourceSystem": "sierra-system-number", "value": "b1161044x"},
        {"type": "Image"},
    )
    assert result["statusCode"] == 404


def test_reverse_bad_type_enum_is_400(
    invoke: Invoke, assert_contract: AssertContract
) -> None:
    result = invoke(
        REVERSE,
        {"sourceSystem": "sierra-system-number", "value": "b1161044x"},
        {"type": "Banana"},
    )
    assert_contract(result, "GET", REVERSE, 400, query={"type": "Banana"})


def test_reverse_siblings_with_no_rows_is_404(
    invoke: Invoke, monkeypatch: pytest.MonkeyPatch
) -> None:
    # The seeded store cannot produce this: its two reads always agree.
    monkeypatch.setattr(handler._repo, "get_by_canonical", lambda _: [])
    result = invoke(
        REVERSE,
        {"sourceSystem": "sierra-system-number", "value": "b1161044x"},
        {"type": "Work", "include": "siblings"},
    )
    assert result["statusCode"] == 404
