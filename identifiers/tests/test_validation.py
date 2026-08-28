"""Edge validation: the 400 cases and the error body shape."""

from conftest import body

FORWARD = "/v1/identifiers/{canonicalId}"
REVERSE = "/v1/identifiers/by-source/{sourceSystem}/{value}"


def test_error_body_shape(invoke):
    # This API's contract uses {error, message} (not folio-api's {message}).
    result = invoke(FORWARD, {"canonicalId": "zzz"})
    payload = body(result)
    assert set(payload) == {"error", "message"}
    assert payload["error"] == "badRequest"


def test_too_short_canonical_id_is_400(invoke):
    assert invoke(FORWARD, {"canonicalId": "abcdef"})["statusCode"] == 400


def test_excluded_letters_rejected(invoke):
    # 'o', 'i', 'l' and '1' are excluded from the alphabet.
    assert invoke(FORWARD, {"canonicalId": "aoilabcd"})["statusCode"] == 400


def test_leading_digit_rejected(invoke):
    # First character must be a letter.
    assert invoke(FORWARD, {"canonicalId": "2345bcde"})["statusCode"] == 400


def test_bad_type_enum_is_400(invoke):
    result = invoke(
        REVERSE,
        {"sourceSystem": "sierra-system-number", "value": "b1161044x"},
        {"type": "Nonsense"},
    )
    assert result["statusCode"] == 400


def test_bad_include_value_is_400(invoke):
    result = invoke(
        REVERSE,
        {"sourceSystem": "sierra-system-number", "value": "b1161044x"},
        {"type": "Work", "include": "cousins"},
    )
    assert result["statusCode"] == 400
