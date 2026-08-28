"""ETag emission, Cache-Control, and If-None-Match -> 304 behaviour."""

FORWARD = "/v1/identifiers/{canonicalId}"
REVERSE = "/v1/identifiers/by-source/{sourceSystem}/{value}"


def test_forward_emits_weak_etag_and_cache_control(invoke):
    result = invoke(FORWARD, {"canonicalId": "a2345bcd"})
    headers = result["headers"]
    # Weak validator from (row_count, max(createdAt)): 2 rows, latest 2026-02-10.
    assert headers["ETag"] == 'W/"2-2026-02-10T12:00:00Z"'
    assert headers["Cache-Control"] == "public, max-age=300"


def test_if_none_match_returns_304(invoke):
    first = invoke(FORWARD, {"canonicalId": "a2345bcd"})
    etag = first["headers"]["ETag"]
    second = invoke(
        FORWARD, {"canonicalId": "a2345bcd"}, headers={"If-None-Match": etag}
    )
    assert second["statusCode"] == 304
    assert second["headers"]["ETag"] == etag
    assert second["body"] == ""


def test_304_validates_against_spec(invoke, assert_contract):
    first = invoke(FORWARD, {"canonicalId": "a2345bcd"})
    etag = first["headers"]["ETag"]
    second = invoke(
        FORWARD, {"canonicalId": "a2345bcd"}, headers={"If-None-Match": etag}
    )
    assert_contract(second, "GET", FORWARD, 304)


def test_stale_etag_returns_200(invoke):
    result = invoke(
        FORWARD,
        {"canonicalId": "a2345bcd"},
        headers={"If-None-Match": 'W/"1-2000-01-01T00:00:00Z"'},
    )
    assert result["statusCode"] == 200


def test_siblings_carries_etag_bare_reverse_does_not(invoke):
    siblings = invoke(
        REVERSE,
        {"sourceSystem": "sierra-system-number", "value": "b1161044x"},
        {"type": "Work", "include": "siblings"},
    )
    assert "ETag" in siblings["headers"]

    bare = invoke(
        REVERSE,
        {"sourceSystem": "sierra-system-number", "value": "b1161044x"},
        {"type": "Work"},
    )
    # Bare reverse is immutable once minted -> long TTL, no ETag (prototype).
    assert "ETag" not in bare["headers"]
    assert bare["headers"]["Cache-Control"] == "public, max-age=86400"
