"""API Gateway Lambda-proxy handler for the items v3 service.

A skeleton: it routes the two paths in spec/openapi.yaml and answers the
manifest. The items route reads nothing yet, so it has no items to return.
"""

import json
import os
import re
import time
from datetime import UTC, datetime
from typing import Any

import structlog

from logger import setup_logging

setup_logging()
logger = structlog.get_logger(__name__)

# The gateway resources, under the stage's /catalogue base path.
_ITEMS = "/v3/works/{workId}/items"
_MANIFEST = "/management/items-v3/manifest"

# As the WorkId parameter in the spec.
_WORK_ID = re.compile(r"[a-hjkmnp-z][a-hjkmnp-z2-9]{7}")

# Baked into the image by CI. "unknown" matches the other services' manifests.
BUILD_COMMIT = os.environ.get("BUILD_COMMIT") or "unknown"
STARTED_AT = datetime.now(UTC).isoformat().replace("+00:00", "Z")


def handler(event: dict, context: Any = None) -> dict:
    request_context = event.get("requestContext") or {}
    work_id = (event.get("pathParameters") or {}).get("workId")
    structlog.contextvars.clear_contextvars()
    structlog.contextvars.bind_contextvars(
        gateway_request_id=request_context.get("requestId"),
        resource=event.get("resource"),
        work_id=work_id,
    )
    start_time = time.perf_counter()
    response = _route(event.get("resource"), work_id)
    logger.info(
        "Request completed",
        status=response.get("statusCode"),
        duration_ms=round((time.perf_counter() - start_time) * 1000, 2),
    )
    return response


def _route(resource: str | None, work_id: str | None) -> dict:
    if resource == _MANIFEST:
        return _manifest()
    if resource != _ITEMS:
        return _error(404, "Not Found", "Page not found")
    return _items(work_id or "")


def _items(work_id: str) -> dict:
    if not _WORK_ID.fullmatch(work_id):
        return _error(404, "Not Found", f"Work not found for identifier {work_id}")
    # A 500, not an empty list, so no caller takes the skeleton for a work
    # with no items.
    return _error(500, "Internal Server Error", "Items are not served yet")


def _manifest() -> dict:
    """The commit this image is running. No-store, or a cache reports the last deploy."""
    return {
        "statusCode": 200,
        "headers": {"Content-Type": "application/json", "Cache-Control": "no-store"},
        "body": json.dumps({"commit": BUILD_COMMIT, "startedAt": STARTED_AT}),
    }


def _error(status: int, label: str, description: str) -> dict:
    """The catalogue API's error shape, as v2 returns."""
    return {
        "statusCode": status,
        "headers": {"Content-Type": "application/json"},
        "body": json.dumps(
            {
                "errorType": "http",
                "httpStatus": status,
                "label": label,
                "description": description,
                "type": "Error",
            }
        ),
    }
