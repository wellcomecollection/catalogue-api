"""Forward lookup responses validated against the spec."""

from adapters import handler

FORWARD = "/v1/identifiers/{canonicalId}"


def test_forward_200_matches_identifier_set(invoke, assert_contract):
    result = invoke(FORWARD, {"canonicalId": "a2345bcd"})
    assert_contract(result, "GET", FORWARD, 200)


def test_forward_single_source_200(invoke, assert_contract):
    result = invoke(FORWARD, {"canonicalId": "mn23pqrs"})
    assert_contract(result, "GET", FORWARD, 200)


def test_forward_404_matches_error(invoke, assert_contract):
    result = invoke(FORWARD, {"canonicalId": "abcdefgh"})
    assert_contract(result, "GET", FORWARD, 404)


def test_forward_400_matches_error(invoke, assert_contract):
    result = invoke(FORWARD, {"canonicalId": "zzz"})
    assert_contract(result, "GET", FORWARD, 400)


def test_forward_500_matches_error(invoke, assert_contract, monkeypatch):
    def unavailable(_):
        raise TimeoutError("RDS Data API timed out")

    monkeypatch.setattr(handler._repo, "get_by_canonical", unavailable)
    result = invoke(FORWARD, {"canonicalId": "a2345bcd"})
    assert_contract(result, "GET", FORWARD, 500)
