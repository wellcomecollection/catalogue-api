"""Local HTTP invoker — the one-command demo surface.

Translates real HTTP requests into API-Gateway proxy events, calls the same
``handler`` a Lambda would, and writes the proxy response back as HTTP. This
keeps the prototype faithful to the production shape (no web framework) while
still being exercisable with curl for the README transcripts.

Run:  uv run python src/adapters/run_local.py   (defaults to port 8000)
"""

import os
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any
from urllib.parse import parse_qs, urlsplit

from adapters.handler import BACKEND, handler

_FORWARD = "/identifiers/v1/{canonicalId}"
_SOURCE_SYSTEMS = "/identifiers/v1/source-systems"
_REVERSE = "/identifiers/v1/by-source/{sourceSystem}/{value}"
_REVERSE_PREFIX = "/identifiers/v1/by-source/"
_FORWARD_PREFIX = "/identifiers/v1/"


def _route(path: str) -> tuple[str | None, dict]:
    """Map a URL path to (resource, pathParameters).

    Segments are left percent-encoded, as API Gateway leaves them; the handler
    decodes them.
    """
    # Literal route wins over {canonicalId}, as in API Gateway.
    if path == _SOURCE_SYSTEMS:
        return _SOURCE_SYSTEMS, {}
    if path.startswith(_REVERSE_PREFIX):
        rest = path[len(_REVERSE_PREFIX) :]
        segments = rest.split("/")
        if len(segments) == 2 and all(segments):
            return _REVERSE, {
                "sourceSystem": segments[0],
                "value": segments[1],
            }
        return None, {}
    if path.startswith(_FORWARD_PREFIX):
        rest = path[len(_FORWARD_PREFIX) :]
        if rest and "/" not in rest:
            return _FORWARD, {"canonicalId": rest}
    return None, {}


class _Handler(BaseHTTPRequestHandler):
    def do_GET(self) -> None:
        split = urlsplit(self.path)
        resource, path_params = _route(split.path)
        query = {k: v[0] for k, v in parse_qs(split.query).items()}
        event = {
            "resource": resource,
            "path": split.path,
            "httpMethod": "GET",
            "pathParameters": path_params or None,
            "queryStringParameters": query or None,
            "headers": dict(self.headers),
        }
        response = handler(event)

        body = (response.get("body") or "").encode()
        self.send_response(response["statusCode"])
        for key, value in (response.get("headers") or {}).items():
            self.send_header(key, value)
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, fmt: str, *args: Any) -> None:  # quieter console
        sys.stderr.write(f"{self.address_string()} - {fmt % args}\n")


def main() -> None:
    port = int(os.environ.get("PORT", "8000"))
    server = ThreadingHTTPServer(("127.0.0.1", port), _Handler)
    print(
        f"Identifiers API (prototype) on http://127.0.0.1:{port}  [backend: {BACKEND}]"
    )
    print("  GET /identifiers/v1/{canonicalId}")
    print("  GET /identifiers/v1/source-systems")
    print(
        "  GET /identifiers/v1/by-source/{sourceSystem}/{value}?type=Work[&include=siblings]"
    )
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        server.shutdown()


if __name__ == "__main__":
    main()
