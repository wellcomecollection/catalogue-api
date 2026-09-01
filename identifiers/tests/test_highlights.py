"""The brief's "Expected highlights" as explicit assertions.

These are the cases the README transcripts demonstrate and the raw material for
the RFC's demonstration section.
"""

from conftest import body

FORWARD = "/v1/identifiers/{canonicalId}"
REVERSE = "/v1/identifiers/by-source/{sourceSystem}/{value}"


def test_forward_returns_ordered_set_with_isalias(invoke):
    result = invoke(FORWARD, {"canonicalId": "a2345bcd"})
    assert result["statusCode"] == 200
    payload = body(result)
    assert payload["canonicalId"] == "a2345bcd"
    # Top-level type is the original row's type (both rows are Work here).
    assert payload["type"] == "Work"
    rows = payload["sourceIdentifiers"]
    assert len(rows) == 2
    # Axiell alias first (most recent createdAt), isAlias true.
    assert rows[0]["sourceSystem"] == "axiell-collections-id"
    assert rows[0]["isAlias"] is True
    # Sierra original last (earliest createdAt), isAlias false.
    assert rows[1]["sourceSystem"] == "sierra-system-number"
    assert rows[1]["isAlias"] is False


def test_reverse_bare_returns_canonical_id(invoke):
    result = invoke(
        REVERSE,
        {"sourceSystem": "sierra-system-number", "value": "b1161044x"},
        {"type": "Work"},
    )
    assert result["statusCode"] == 200
    assert body(result) == {"canonicalId": "a2345bcd"}


def test_reverse_with_siblings_returns_full_set(invoke):
    result = invoke(
        REVERSE,
        {"sourceSystem": "sierra-system-number", "value": "b1161044x"},
        {"type": "Work", "include": "siblings"},
    )
    assert result["statusCode"] == 200
    payload = body(result)
    assert payload["canonicalId"] == "a2345bcd"
    assert payload["type"] == "Work"
    assert len(payload["sourceIdentifiers"]) == 2
    # The queried tuple is included in the returned set.
    assert any(
        s["sourceSystem"] == "sierra-system-number" and s["value"] == "b1161044x"
        for s in payload["sourceIdentifiers"]
    )


def test_reverse_folio_item_translation(invoke):
    """The requesting reverse-translation case (RFC 088): FOLIO item UUID ->
    canonical item id."""
    result = invoke(
        REVERSE,
        {"sourceSystem": "folio-item", "value": "3f2a...uuid"},
        {"type": "Item"},
    )
    assert result["statusCode"] == 200
    assert body(result) == {"canonicalId": "ka345678"}


def test_cross_type_canonical_carries_rows_of_differing_types(invoke):
    result = invoke(FORWARD, {"canonicalId": "ze789fgh"})
    assert result["statusCode"] == 200
    payload = body(result)
    rows = payload["sourceIdentifiers"]
    assert {r["type"] for r in rows} == {"Work", "Image"}
    # Work (2026) comes first, but Image (2019) is the original.
    assert rows[0]["type"] == "Work"
    assert rows[0]["isAlias"] is True
    assert rows[1]["type"] == "Image"
    assert rows[1]["isAlias"] is False
    # Top-level type follows the original, so it is Image, not the first row's Work.
    assert payload["type"] == "Image"


def test_valid_format_but_unknown_is_404(invoke):
    result = invoke(FORWARD, {"canonicalId": "abcdefgh"})
    assert result["statusCode"] == 404


def test_malformed_canonical_id_is_400(invoke):
    result = invoke(FORWARD, {"canonicalId": "zzz"})
    assert result["statusCode"] == 400
