"""Minimal Folio (OKAPI) client.

Carries only the read calls this service makes, to limit what it can reach in
FOLIO. Pure standard library and free of service config, so it can become a shared
module later (platform#6652). The login token is cached on the instance and
refreshed once on a 401, so one client is reused across warm Lambda invocations.
"""

from __future__ import annotations

import json
import logging
import ssl
import urllib.error
import urllib.parse
import urllib.request
from collections.abc import Callable
from typing import Any, cast

logger = logging.getLogger(__name__)

CredentialsProvider = Callable[[], "tuple[str, str]"]


def _cql_string(value: str) -> str:
    """``value`` as a quoted CQL string, matched literally rather than read as query
    syntax. Escapes what FOLIO's ``StringUtil.cqlEncode`` does; as identity's copy."""
    escaped = "".join(f"\\{c}" if c in '\\*?^"' else c for c in value)
    return f'"{escaped}"'


class FolioError(RuntimeError):
    """A non-2xx Folio response (or a transport/login failure)."""

    def __init__(self, message: str, status: int | None = None, body: Any = None):
        super().__init__(message)
        self.status = status
        self.body = body


class FolioClient:
    def __init__(
        self,
        base_url: str,
        tenant: str,
        *,
        username: str | None = None,
        password: str | None = None,
        credentials_provider: CredentialsProvider | None = None,
        ssl_context: ssl.SSLContext | None = None,
        timeout: int = 30,
    ):
        if not base_url or not tenant:
            raise ValueError("base_url and tenant are required")
        self.base_url = base_url.rstrip("/")
        self.tenant = tenant
        self._username = username
        self._password = password
        self._credentials_provider = credentials_provider
        self._ssl_context = ssl_context
        self.timeout = timeout
        self._token: str | None = None

    # --- authentication ---

    def _credentials(self) -> tuple[str, str]:
        if self._username is not None and self._password is not None:
            return self._username, self._password
        if self._credentials_provider is not None:
            self._username, self._password = self._credentials_provider()
            return self._username, self._password
        raise FolioError("No Folio credentials configured")

    def login(self) -> str:
        username, password = self._credentials()
        logger.info("OKAPI login as %s on tenant %s", username, self.tenant)
        status, data, headers = self._raw(
            "POST",
            "/authn/login",
            body={"username": username, "password": password},
            authed=False,
        )
        if status not in (200, 201):
            raise FolioError(f"OKAPI login failed ({status})", status, data)
        token = cast("str | None", headers.get("x-okapi-token"))
        if not token:
            raise FolioError("OKAPI login returned no x-okapi-token header", status)
        self._token = token
        logger.info("OKAPI login succeeded")
        return token

    # --- transport ---

    def _raw(
        self,
        method: str,
        path: str,
        *,
        body: dict | None = None,
        authed: bool = True,
        accept: str = "application/json",
    ) -> tuple[int, Any, Any]:
        data = json.dumps(body).encode() if body is not None else None
        headers: dict[str, str] = {"x-okapi-tenant": self.tenant, "accept": accept}
        if authed:
            headers["x-okapi-token"] = self._token or ""
        if data is not None:
            headers["content-type"] = "application/json"
        req = urllib.request.Request(
            f"{self.base_url}{path}", data=data, headers=headers, method=method
        )
        try:
            with urllib.request.urlopen(
                req, timeout=self.timeout, context=self._ssl_context
            ) as resp:
                raw = resp.read().decode()
                # Many OKAPI writes (e.g. PUT /users/{id}) return 204 with no body.
                parsed = json.loads(raw) if raw else {}
                return resp.status, parsed, resp.headers
        except urllib.error.HTTPError as e:
            detail = e.read().decode(errors="replace")
            try:
                parsed = json.loads(detail) if detail else {}
            except json.JSONDecodeError:
                parsed = detail
            return e.code, parsed, (e.headers or {})

    def request(
        self,
        method: str,
        path: str,
        body: dict | None = None,
        *,
        accept: str = "application/json",
        _retried: bool = False,
    ) -> tuple[int, Any]:
        """Authenticated OKAPI request with lazy login and one 401 re-auth."""
        if self._token is None:
            self.login()
        logger.info("OKAPI %s %s", method, path)
        status, data, _ = self._raw(method, path, body=body, accept=accept)
        if status == 401 and not _retried:
            logger.info("OKAPI %s %s returned 401, re-authenticating", method, path)
            self.login()
            return self.request(method, path, body, accept=accept, _retried=True)
        return status, data

    # --- mod-inventory-storage (items for an instance) ---

    def items_and_holdings(self, instance_id: str) -> dict | None:
        """An instance's ``{"instanceId", "holdings": [...], "items": [...]}`` record
        from ``inventory-hierarchy/items-and-holdings``, in one call and unpaged, as
        the catalogue pipeline reads it. None when FOLIO has no such instance.

        Suppressed records are included: the service has to see an item that was
        suppressed after the last indexing run.
        """
        status, data = self.request(
            "POST",
            "/inventory-hierarchy/items-and-holdings",
            {"instanceIds": [instance_id], "skipSuppressedFromDiscoveryRecords": False},
        )
        if status != 200:
            raise FolioError(f"items-and-holdings failed ({status})", status, data)
        # One record per instance, streamed as bare JSON objects; an unknown
        # instance gives an empty body, which _raw parses as {}.
        records = data if isinstance(data, list) else [data]
        for record in records:
            if isinstance(record, dict) and record.get("instanceId") == instance_id:
                return record
        return None

    # --- mod-circulation (open requests on items) ---

    def open_request_item_ids(self, item_ids: list[str]) -> set[str]:
        """Subset of ``item_ids`` that already have an open request (hold). We
        never queue holds, so any item with an open request is not requestable."""
        if not item_ids:
            return set()
        clause = " or ".join(_cql_string(i) for i in item_ids)
        query = urllib.parse.quote(f"(itemId==({clause}) and status==Open*)")
        status, data = self.request(
            "GET", f"/circulation/requests?query={query}&limit=1000"
        )
        if status != 200:
            raise FolioError(f"requests lookup failed ({status})", status, data)
        return {r["itemId"] for r in data.get("requests", []) if r.get("itemId")}
