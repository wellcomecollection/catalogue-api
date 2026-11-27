"""The FOLIO client against a fake OKAPI: token reuse, refresh, login failure and
the two calls the availability path makes."""

import logging
from typing import Any

import pytest
from conftest import TENANT, FakeOkapi

from folio_client import FolioClient, FolioError

INSTANCE = "5bf370e0-8cca-4d9c-82e4-5170ab2a0a39"
HOLDINGS = [
    "11111111-1111-4111-8111-111111111111",
    "22222222-2222-4222-8222-222222222222",
]
ITEMS = ["aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa", "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"]


def client_for(okapi: FakeOkapi) -> FolioClient:
    return FolioClient(okapi.url, TENANT, username="svc", password="secret")


def ok(body: Any) -> Any:
    return lambda *_: (200, body, {})


IAH = "/inventory-hierarchy/items-and-holdings"
RECORD = {
    "instanceId": INSTANCE,
    "holdings": [{"id": h} for h in HOLDINGS],
    "items": [
        {"id": ITEMS[0], "holdingsRecordId": HOLDINGS[0], "status": "Available"},
        {
            "id": ITEMS[1],
            "holdingsRecordId": HOLDINGS[1],
            "status": "Available",
            "suppressFromDiscovery": True,
        },
    ],
}


def test_reuses_the_token_across_calls(okapi: FakeOkapi) -> None:
    okapi.routes[IAH] = ok(RECORD)
    client = client_for(okapi)

    client.items_and_holdings(INSTANCE)
    client.items_and_holdings(INSTANCE)

    assert okapi.logins == 1
    assert [c.token for c in okapi.calls_to(IAH)] == ["token-1", "token-1"]
    assert all(c.tenant == TENANT for c in okapi.calls)


def test_refreshes_once_on_401_and_retries(okapi: FakeOkapi) -> None:
    okapi.routes[IAH] = ok(RECORD)
    client = client_for(okapi)
    client.items_and_holdings(INSTANCE)

    okapi.valid_tokens.clear()  # the cached token expires
    assert client.items_and_holdings(INSTANCE) == RECORD

    assert okapi.logins == 2
    tokens = [c.token for c in okapi.calls_to(IAH)]
    assert tokens == ["token-1", "token-1", "token-2"]


def test_does_not_retry_a_second_401(okapi: FakeOkapi) -> None:
    # Every token is rejected, as if the account lacked the permission.
    def reject(*_: Any) -> tuple[int, Any, dict[str, str]]:
        return 401, "Invalid token", {}

    okapi.routes[IAH] = reject
    client = client_for(okapi)

    with pytest.raises(FolioError) as e:
        client.items_and_holdings(INSTANCE)

    assert e.value.status == 401
    assert okapi.logins == 2
    assert len(okapi.calls_to(IAH)) == 2


def test_failed_login_raises_without_calling_folio(okapi: FakeOkapi) -> None:
    okapi.login_status = 422
    client = client_for(okapi)

    with pytest.raises(FolioError) as e:
        client.items_and_holdings(INSTANCE)

    assert e.value.status == 422
    assert [c.path for c in okapi.calls] == ["/authn/login"]


def test_reads_credentials_from_the_provider_once(okapi: FakeOkapi) -> None:
    okapi.routes[IAH] = ok(RECORD)
    reads: list[int] = []

    def provider() -> tuple[str, str]:
        reads.append(1)
        return "svc", "secret"

    client = FolioClient(okapi.url, TENANT, credentials_provider=provider)
    client.items_and_holdings(INSTANCE)
    okapi.valid_tokens.clear()
    client.items_and_holdings(INSTANCE)

    assert okapi.logins == 2
    assert len(reads) == 1


def test_items_and_holdings_is_one_call_including_suppressed(
    okapi: FakeOkapi,
) -> None:
    okapi.routes[IAH] = ok(RECORD)

    record = client_for(okapi).items_and_holdings(INSTANCE)

    assert record == RECORD
    [call] = [c for c in okapi.calls if c.path != "/authn/login"]
    assert (call.method, call.path) == ("POST", IAH)
    assert call.body == {
        "instanceIds": [INSTANCE],
        "skipSuppressedFromDiscoveryRecords": False,
    }


def test_items_and_holdings_is_none_for_an_unknown_instance(
    okapi: FakeOkapi,
) -> None:
    okapi.routes[IAH] = ok(None)

    assert client_for(okapi).items_and_holdings(INSTANCE) is None


def test_items_and_holdings_ignores_a_record_for_another_instance(
    okapi: FakeOkapi,
) -> None:
    other = "99999999-9999-4999-8999-999999999999"
    okapi.routes[IAH] = ok([{"instanceId": other, "items": []}, RECORD])

    assert client_for(okapi).items_and_holdings(INSTANCE) == RECORD


def test_items_and_holdings_raises_on_an_error(okapi: FakeOkapi) -> None:
    okapi.routes[IAH] = lambda *_: (500, {"errors": []}, {})

    with pytest.raises(FolioError) as e:
        client_for(okapi).items_and_holdings(INSTANCE)
    assert e.value.status == 500


def test_open_request_item_ids_returns_the_held_items(okapi: FakeOkapi) -> None:
    okapi.routes["/circulation/requests"] = ok(
        {
            "requests": [
                {"id": "r1", "itemId": ITEMS[1], "status": "Open - Not yet filled"}
            ]
        }
    )

    held = client_for(okapi).open_request_item_ids(ITEMS)

    assert held == {ITEMS[1]}
    [call] = okapi.calls_to("/circulation/requests")
    assert call.query == (f'(itemId==("{ITEMS[0]}" or "{ITEMS[1]}") and status==Open*)')


def test_open_request_item_ids_with_no_items_calls_nothing(okapi: FakeOkapi) -> None:
    assert client_for(okapi).open_request_item_ids([]) == set()
    assert okapi.calls == []


def test_open_request_item_ids_raises_when_folio_truncates(okapi: FakeOkapi) -> None:
    # Queued requests on one item can fill the page and hide holds on others.
    okapi.routes["/circulation/requests"] = ok(
        {"requests": [{"itemId": ITEMS[0]}], "totalRecords": 1001}
    )

    with pytest.raises(FolioError, match="truncated"):
        client_for(okapi).open_request_item_ids(ITEMS)


def test_logs_paths_without_query_strings(
    okapi: FakeOkapi, caplog: pytest.LogCaptureFixture
) -> None:
    okapi.routes["/circulation/requests"] = ok({"requests": [], "totalRecords": 0})
    client = client_for(okapi)
    client.open_request_item_ids(ITEMS)
    okapi.valid_tokens.clear()

    with caplog.at_level(logging.INFO, logger="folio_client.client"):
        client.open_request_item_ids(ITEMS)

    messages = [r.getMessage() for r in caplog.records if "OKAPI GET" in r.getMessage()]
    assert messages == [
        "OKAPI GET /circulation/requests",
        "OKAPI GET /circulation/requests returned 401, re-authenticating",
        "OKAPI GET /circulation/requests",
    ]
    assert not any(ITEMS[0] in r.getMessage() for r in caplog.records)
