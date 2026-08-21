"""RdsDataRepository unit tests — hermetic, no network, no boto3.

A fake Data API client returns canned responses, so these tests exercise the
SQL, parameter binding, column mapping, CreatedAt normalisation, and the
read-only guard without touching AWS. The repository must never write.
"""

import pytest

from adapters.rds_data_repo import (
    RdsDataRepository,
    _assert_read_only,
    _to_iso,
)


class FakeDataClient:
    """Records calls and returns a pre-set Data API response."""

    def __init__(self, response: dict):
        self._response = response
        self.calls: list[dict] = []

    def execute_statement(self, **kwargs):
        self.calls.append(kwargs)
        return self._response


def _str(value):
    return {"stringValue": value}


def make_repo(response):
    return RdsDataRepository(
        client=FakeDataClient(response),
        resource_arn="arn:aws:rds:eu-west-1:0:cluster:test",
        secret_arn="arn:aws:secretsmanager:eu-west-1:0:secret:test",
        database="identifiers",
    )


def test_get_by_canonical_maps_columns_and_normalises_timestamp():
    response = {
        "records": [
            [
                _str("Work"),
                _str("sierra-system-number"),
                _str("b1161044x"),
                _str("2019-03-04 10:14:22"),
            ],
            [
                _str("Work"),
                _str("axiell-collections-id"),
                _str("12345"),
                _str("2026-02-10 12:00:00"),
            ],
        ]
    }
    repo = make_repo(response)
    rows = repo.get_by_canonical("a2345bcd")

    assert [r.source_system for r in rows] == [
        "sierra-system-number",
        "axiell-collections-id",
    ]
    # MySQL "YYYY-MM-DD HH:MM:SS" -> contract ISO-8601 with T and Z.
    assert rows[0].created_at == "2019-03-04T10:14:22Z"
    assert rows[1].created_at == "2026-02-10T12:00:00Z"


def test_get_by_canonical_empty_returns_empty_list():
    assert make_repo({"records": []}).get_by_canonical("missing23") == []


def test_get_by_source_returns_canonical_id():
    repo = make_repo({"records": [[_str("ka345678")]]})
    assert repo.get_by_source("Item", "folio-item", "3f2a...uuid") == "ka345678"


def test_get_by_source_unknown_returns_none():
    assert make_repo({"records": []}).get_by_source("Work", "x", "y") is None


def test_queries_are_parameterised_not_interpolated():
    repo = make_repo({"records": [[_str("a2345bcd")]]})
    repo.get_by_source("Work", "sierra-system-number", "b1161044x")
    call = repo._client.calls[0]
    # The value rides as a bound parameter, never in the SQL string.
    assert "b1161044x" not in call["sql"]
    assert {"name": "source_id", "value": {"stringValue": "b1161044x"}} in call[
        "parameters"
    ]


def test_only_select_statements_are_issued():
    row = [_str("Work"), _str("s"), _str("v"), _str("2020-01-01 00:00:00")]
    repo = make_repo({"records": [row]})
    repo.get_by_canonical("a2345bcd")
    repo.get_by_source("Work", "s", "v")
    for call in repo._client.calls:
        assert call["sql"].lstrip().upper().startswith("SELECT")


@pytest.mark.parametrize(
    "sql",
    [
        "UPDATE identifiers SET x=1",
        "INSERT INTO t VALUES (1)",
        "DELETE FROM t",
        "DROP TABLE t",
        "  update t set a=1",
    ],
)
def test_read_only_guard_refuses_writes(sql):
    with pytest.raises(RuntimeError, match="read-only"):
        _assert_read_only(sql)


def test_to_iso_handles_missing():
    assert _to_iso(None) == ""
    assert _to_iso("") == ""
    assert _to_iso("2020-01-01 00:00:00") == "2020-01-01T00:00:00Z"
