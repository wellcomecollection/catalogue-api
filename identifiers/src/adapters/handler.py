"""API Gateway Lambda-proxy handler: HTTP <-> core.

A pure function from a proxy event to a proxy response dict, matching the
folio-api prototype's idiom. It performs no business logic itself — it routes by
``resource``, calls the service, maps domain errors to status codes, and applies
the conditional-GET (ETag / If-None-Match -> 304) behaviour.

Error bodies follow this API's own contract (`{error, message}`), which differs
from folio-api's `{message}` shape — see the README.
"""

import json
import os
from datetime import UTC, datetime
from typing import Any

from adapters.rds_data_repo import RdsDataRepository
from adapters.sqlite_repo import SqliteRepository, build_seeded_connection
from core.repository import Repository
from core.service import BadRequest, IdentifiersService, LookupResult, NotFound

# Backend selection (prototype default: the seeded SQLite store). Set
# IDENTIFIERS_BACKEND=rds to read the real Aurora ID Registry over the RDS Data
# API instead — see adapters/rds_data_repo.py and the README. Either backend
# satisfies the same Repository protocol; nothing in `core` changes.
BACKEND = os.environ.get("IDENTIFIERS_BACKEND", "sqlite").lower()


def _build_repo() -> Repository:
    if BACKEND == "rds":
        return RdsDataRepository.build_from_env()
    return SqliteRepository(build_seeded_connection())


# Built once per warm container (here, per process), mirroring how a Lambda
# binds a module-level client.
_repo = _build_repo()
_service = IdentifiersService(_repo)

_FORWARD = "/identifiers/v1/{canonicalId}"
_REVERSE = "/identifiers/v1/by-source/{sourceSystem}/{value}"
_MANIFEST = "/management/manifest"

# Baked into the image by CI, since a running container has no repository to ask.
# The Dockerfile always defines it, so an image built without the build argument
# carries it empty rather than absent. "unknown" matches ManifestRoute.scala and
# the concepts controller, so the same string means the same thing everywhere.
BUILD_COMMIT = os.environ.get("BUILD_COMMIT") or "unknown"
# Module import is close enough to process start, and it is what a warm
# container reports for its whole life, which is the useful reading.
STARTED_AT = datetime.now(UTC).isoformat().replace("+00:00", "Z")


def handler(event: dict, context: Any = None) -> dict:
    resource = event.get("resource")
    path_params = event.get("pathParameters") or {}
    query = event.get("queryStringParameters") or {}

    if resource == _MANIFEST:
        # Ahead of the try: it reads two module constants and can raise nothing
        # the error mapping below would have anything useful to say about. It
        # does still depend on the repository built at import, so a Lambda that
        # fails to initialise serves no manifest either.
        return _manifest()

    try:
        if resource == _FORWARD:
            result = _service.resolve_canonical(path_params.get("canonicalId", ""))
        elif resource == _REVERSE:
            result = _service.resolve_source(
                source_system=path_params.get("sourceSystem", ""),
                value=path_params.get("value", ""),
                type_=query.get("type"),
                include=query.get("include"),
            )
        else:
            return _error(404, "notFound", "no mapping found")
    except BadRequest as exc:
        return _error(400, exc.code, exc.message)
    except NotFound as exc:
        return _error(404, exc.code, exc.message)
    except Exception:
        return _error(500, "internalServerError", "the request could not be completed")

    return _ok(event, result)


def _ok(event: dict, result: LookupResult) -> dict:
    headers = {"Cache-Control": result.cache_control}
    if result.etag is not None:
        headers["ETag"] = result.etag
        if _if_none_match(event, result.etag):
            # Conditional GET: unchanged since the supplied ETag.
            return {"statusCode": 304, "headers": headers, "body": ""}

    headers["Content-Type"] = "application/json"
    return {
        "statusCode": 200,
        "headers": headers,
        "body": json.dumps(result.body.to_dict()),
    }


def _if_none_match(event: dict, etag: str) -> bool:
    """True if the client's If-None-Match lists our ETag.

    Prototype simplification: exact token match (the client echoes the ETag we
    issued). A production gateway would apply full weak-comparison semantics.
    """
    header = _header(event, "If-None-Match")
    if not header:
        return False
    return etag in [token.strip() for token in header.split(",")]


def _header(event: dict, name: str) -> str | None:
    """Case-insensitive lookup over proxy-event headers."""
    headers: dict[str, str] = event.get("headers") or {}
    lowered = name.lower()
    for key, value in headers.items():
        if key.lower() == lowered:
            return value
    return None


def _manifest() -> dict:
    """The commit this image is running, for the deploy tracker.

    No-store because a cached response would report the previous deployment.
    """
    return {
        "statusCode": 200,
        "headers": {"Content-Type": "application/json", "Cache-Control": "no-store"},
        "body": json.dumps({"commit": BUILD_COMMIT, "startedAt": STARTED_AT}),
    }


def _error(status_code: int, code: str, message: str) -> dict:
    return {
        "statusCode": status_code,
        "headers": {"Content-Type": "application/json"},
        "body": json.dumps({"error": code, "message": message}),
    }
