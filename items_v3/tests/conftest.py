"""A fake OKAPI server on localhost, so tests go through the client's real transport."""

import json
import threading
import urllib.parse
from collections.abc import Callable, Iterator
from dataclasses import dataclass, field
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any

import pytest

TENANT = "test-tenant"

# (method, path, query, token, body) -> (status, json body, extra headers)
Route = Callable[[str, str, str, str | None, Any], tuple[int, Any, dict[str, str]]]


@dataclass
class Call:
    method: str
    path: str
    query: str
    token: str | None
    tenant: str | None
    body: Any = None


@dataclass
class FakeOkapi:
    url: str
    routes: dict[str, Route] = field(default_factory=dict)
    calls: list[Call] = field(default_factory=list)
    # Tokens the server accepts; a login issues the next one.
    valid_tokens: set[str] = field(default_factory=set)
    logins: int = 0
    login_status: int = 201

    def calls_to(self, path: str) -> list[Call]:
        return [c for c in self.calls if c.path == path]


@pytest.fixture
def okapi() -> Iterator[FakeOkapi]:
    fake = FakeOkapi(url="")

    class Handler(BaseHTTPRequestHandler):
        def log_message(self, *args: Any) -> None:
            pass

        def _reply(self, status: int, body: Any, headers: dict[str, str]) -> None:
            # None sends an empty body, as items-and-holdings does for no match.
            raw = b"" if body is None else json.dumps(body).encode()
            self.send_response(status)
            self.send_header("content-type", "application/json")
            self.send_header("content-length", str(len(raw)))
            for k, v in headers.items():
                self.send_header(k, v)
            self.end_headers()
            self.wfile.write(raw)

        def _handle(self) -> None:
            parsed = urllib.parse.urlsplit(self.path)
            query = urllib.parse.parse_qs(parsed.query).get("query", [""])[0]
            token = self.headers.get("x-okapi-token")
            length = int(self.headers.get("content-length") or 0)
            body = json.loads(self.rfile.read(length)) if length else None
            fake.calls.append(
                Call(
                    self.command,
                    parsed.path,
                    query,
                    token,
                    self.headers.get("x-okapi-tenant"),
                    body,
                )
            )

            if parsed.path == "/authn/login":
                fake.logins += 1
                if fake.login_status not in (200, 201):
                    self._reply(fake.login_status, {"errors": []}, {})
                    return
                new = f"token-{fake.logins}"
                fake.valid_tokens.add(new)
                self._reply(fake.login_status, {}, {"x-okapi-token": new})
                return
            if token not in fake.valid_tokens:
                self._reply(401, "Invalid token", {})
                return
            route = fake.routes.get(parsed.path)
            if route is None:
                self._reply(404, {"errors": []}, {})
                return
            self._reply(*route(self.command, parsed.path, query, token, body))

        do_GET = _handle
        do_POST = _handle

    server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
    fake.url = f"http://127.0.0.1:{server.server_address[1]}"
    thread = threading.Thread(target=server.serve_forever, args=(0.05,), daemon=True)
    thread.start()
    try:
        yield fake
    finally:
        server.shutdown()
        server.server_close()
