"""Request logging emitted by the handler."""

from collections.abc import Iterator
from typing import NoReturn

import pytest
import structlog
from conftest import Invoke, make_event
from structlog.typing import EventDict

from adapters import handler
from core.models import SourceRow

FORWARD = "/identifiers/v1/{canonicalId}"


@pytest.fixture
def logs() -> Iterator[list[EventDict]]:
    # capture_logs replaces the configured processors, so merge_contextvars is
    # passed back in or the fields bound in handler() would not appear.
    with structlog.testing.capture_logs(
        processors=[structlog.contextvars.merge_contextvars]
    ) as entries:
        yield entries


def events_named(logs: list[EventDict], name: str) -> list[EventDict]:
    return [entry for entry in logs if entry["event"] == name]


def keyed_event(canonical_id: str, api_key_id: str, api_key: str) -> dict:
    event = make_event(FORWARD, {"canonicalId": canonical_id})
    event["requestContext"] = {
        "requestId": "gateway-request-1",
        "identity": {"apiKeyId": api_key_id, "apiKey": api_key},
    }
    return event


@pytest.mark.parametrize(
    ("canonical_id", "status"),
    [("a2345bcd", 200), ("abcdefgh", 404), ("zzz", 400)],
)
def test_one_request_completed_line_per_request(
    invoke: Invoke, logs: list[EventDict], canonical_id: str, status: int
) -> None:
    invoke(FORWARD, {"canonicalId": canonical_id})

    [completed] = events_named(logs, "Request completed")
    assert completed["status"] == status
    assert completed["resource"] == FORWARD
    assert completed["duration_ms"] >= 0


def test_request_fields_come_from_the_event(logs: list[EventDict]) -> None:
    handler.handler(keyed_event("a2345bcd", "key-id-1", "secret"))

    [completed] = events_named(logs, "Request completed")
    assert completed["gateway_request_id"] == "gateway-request-1"
    assert completed["api_key_id"] == "key-id-1"
    assert completed["path"] == "/identifiers/v1/a2345bcd"
    assert completed["path_parameters"] == {"canonicalId": "a2345bcd"}


def test_path_parameters_are_logged_decoded(
    invoke: Invoke, logs: list[EventDict]
) -> None:
    invoke(
        "/identifiers/v1/by-source/{sourceSystem}/{value}",
        {"sourceSystem": "mets-image", "value": "b33012246%2FFILE_0170_OBJECTS"},
        {"type": "Image"},
    )

    [completed] = events_named(logs, "Request completed")
    assert completed["path_parameters"] == {
        "sourceSystem": "mets-image",
        "value": "b33012246/FILE_0170_OBJECTS",
    }


def test_only_named_query_parameters_are_logged(
    invoke: Invoke, logs: list[EventDict]
) -> None:
    invoke(
        "/identifiers/v1/by-source/{sourceSystem}/{value}",
        {"sourceSystem": "sierra-system-number", "value": "b1161044x"},
        {"type": "Work", "include": "siblings", "api_key": "do-not-log-me"},
    )

    [completed] = events_named(logs, "Request completed")
    assert completed["query_type"] == "Work"
    assert completed["query_include"] == "siblings"
    assert "do-not-log-me" not in str(completed)


def test_fields_bound_during_a_request_do_not_carry_over_to_the_next(
    invoke: Invoke, logs: list[EventDict], monkeypatch: pytest.MonkeyPatch
) -> None:
    lookup = handler._repo.get_by_canonical

    def lookup_that_binds(canonical_id: str) -> list[SourceRow]:
        structlog.contextvars.bind_contextvars(bound_mid_request=canonical_id)
        return lookup(canonical_id)

    monkeypatch.setattr(handler._repo, "get_by_canonical", lookup_that_binds)
    invoke(FORWARD, {"canonicalId": "a2345bcd"})
    monkeypatch.setattr(handler._repo, "get_by_canonical", lookup)
    invoke(FORWARD, {"canonicalId": "a2345bcd"})

    first, second = events_named(logs, "Request completed")
    assert first["bound_mid_request"] == "a2345bcd"
    assert "bound_mid_request" not in second


def test_the_api_key_value_is_never_logged(logs: list[EventDict]) -> None:
    handler.handler(keyed_event("a2345bcd", "key-id-1", "do-not-log-me"))

    assert logs
    assert all("do-not-log-me" not in str(entry) for entry in logs)


def test_an_unexpected_error_logs_its_traceback(
    invoke: Invoke, logs: list[EventDict], monkeypatch: pytest.MonkeyPatch
) -> None:
    def unavailable(_: str) -> NoReturn:
        raise TimeoutError("RDS Data API timed out")

    monkeypatch.setattr(handler._repo, "get_by_canonical", unavailable)
    invoke(FORWARD, {"canonicalId": "a2345bcd"})

    [failed] = events_named(logs, "Lookup failed")
    assert failed["log_level"] == "error"
    assert failed["exc_info"] is True
    [completed] = events_named(logs, "Request completed")
    assert completed["status"] == 500


@pytest.mark.parametrize("canonical_id", ["abcdefgh", "zzz"])
def test_client_errors_do_not_log_lookup_failed(
    invoke: Invoke, logs: list[EventDict], canonical_id: str
) -> None:
    invoke(FORWARD, {"canonicalId": canonical_id})

    assert events_named(logs, "Lookup failed") == []
