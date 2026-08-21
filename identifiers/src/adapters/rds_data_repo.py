"""Aurora (RDS Data API) implementation of the Repository protocol.

This is the production-shaped backend: it reads the real ID Registry over the
RDS Data API (HTTP, no persistent connections), mirroring the folio-api
SSM/boto3 convention. Selecting it changes nothing in `core` — it satisfies the
same `Repository` protocol as the SQLite prototype store.

READ-ONLY BY CONTRACT AND BY GUARD
==================================
The Identifiers API is a read-only projection; all writes belong to the ID
Minter (RFC 083). This repository issues **only SELECT statements** and refuses
anything else via ``_assert_read_only``. It must NEVER write to or seed the
database. Do not add INSERT/UPDATE/DELETE/DDL/transaction methods here.

Schema note: the live Aurora MySQL tables are ``canonical_ids`` / ``identifiers``
(snake_case table names) but their COLUMNS are PascalCase per RFC 083
(``CanonicalId``, ``OntologyType``, ``SourceSystem``, ``SourceId``,
``CreatedAt``) — hence the explicit column list below. ``CreatedAt`` comes back
as MySQL ``"YYYY-MM-DD HH:MM:SS"`` and is normalised to the contract's ISO-8601.
"""

import os

from core.models import SourceRow


def _assert_read_only(sql: str) -> None:
    """Defence-in-depth: refuse any statement that is not a plain SELECT."""
    if not sql.lstrip().upper().startswith("SELECT"):
        raise RuntimeError(
            "RdsDataRepository is read-only; refusing non-SELECT statement"
        )


def _field(cell: dict) -> str | None:
    """Extract a string from a Data API field, or None when SQL NULL."""
    if cell.get("isNull"):
        return None
    return cell.get("stringValue")


def _to_iso(value: str | None) -> str:
    """MySQL ``"YYYY-MM-DD HH:MM:SS"`` (UTC) -> contract ISO-8601 ``...TZ``.

    The store keeps timestamps in UTC; the Data API returns them space-separated
    and tz-naive. We normalise so ordering, isAlias, and the ETag derivation in
    `core` see the same shape the SQLite store produces.
    """
    if not value:
        return ""
    iso = value.replace(" ", "T")
    if not iso.endswith("Z"):
        iso += "Z"
    return iso


class RdsDataRepository:
    """Read-only Repository over Aurora via the RDS Data API."""

    def __init__(self, client, resource_arn: str, secret_arn: str, database: str):
        self._client = client
        self._resource_arn = resource_arn
        self._secret_arn = secret_arn
        self._database = database

    @classmethod
    def build_from_env(cls) -> "RdsDataRepository":
        """Construct from env vars, mirroring folio-api's runtime config.

        RDS_RESOURCE_ARN  — the Aurora cluster ARN
        RDS_SECRET_ARN    — Secrets Manager ARN holding the DB credentials
        RDS_DATABASE      — database name (defaults to "identifiers")
        Region comes from the standard boto3/AWS resolution (AWS_REGION etc.).
        """
        import boto3  # imported lazily so the SQLite path needs no boto3

        return cls(
            client=boto3.client("rds-data"),
            resource_arn=os.environ["RDS_RESOURCE_ARN"],
            secret_arn=os.environ["RDS_SECRET_ARN"],
            database=os.environ.get("RDS_DATABASE", "identifiers"),
        )

    # -- Repository protocol -------------------------------------------------

    def get_by_canonical(self, canonical_id: str) -> list[SourceRow]:
        # Rides idx_canonical; ordering/isAlias are derived in core.
        sql = (
            "SELECT OntologyType, SourceSystem, SourceId, CreatedAt "
            "FROM identifiers WHERE CanonicalId = :canonical_id"
        )
        params = [_param("canonical_id", canonical_id)]
        records = self._execute(sql, params).get("records", [])
        return [
            SourceRow(
                ontology_type=_field(rec[0]),
                source_system=_field(rec[1]),
                source_id=_field(rec[2]),
                created_at=_to_iso(_field(rec[3])),
            )
            for rec in records
        ]

    def get_by_source(
        self, ontology_type: str, source_system: str, source_id: str
    ) -> str | None:
        # Point read on the three-part primary key.
        sql = (
            "SELECT CanonicalId FROM identifiers "
            "WHERE OntologyType = :ontology_type "
            "AND SourceSystem = :source_system "
            "AND SourceId = :source_id LIMIT 1"
        )
        params = [
            _param("ontology_type", ontology_type),
            _param("source_system", source_system),
            _param("source_id", source_id),
        ]
        records = self._execute(sql, params).get("records", [])
        if not records:
            return None
        return _field(records[0][0])

    # -- internals -----------------------------------------------------------

    def _execute(self, sql: str, parameters: list[dict]) -> dict:
        _assert_read_only(sql)
        return self._client.execute_statement(
            resourceArn=self._resource_arn,
            secretArn=self._secret_arn,
            database=self._database,
            sql=sql,
            parameters=parameters,
        )


def _param(name: str, value: str) -> dict:
    """A Data API named parameter (values are bound, never interpolated)."""
    return {"name": name, "value": {"stringValue": value}}
