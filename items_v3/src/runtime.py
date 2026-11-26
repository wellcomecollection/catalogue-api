"""Service wiring: the FOLIO client, built from the environment and SSM.

Follows the identity API's v2 FOLIO settings (``OKAPI_URL``, ``OKAPI_TENANT``,
``OKAPI_SECRET_PARAM``), where the parameter is a SecureString holding
``{"username": ..., "password": ...}``.
"""

import json
import os
from collections.abc import Callable
from functools import cache
from typing import Any

from folio_client import FolioClient
from folio_client.client import CredentialsProvider

# Provisional per-call timeout; the budget and retries are platform#6656.
FOLIO_TIMEOUT_SECONDS = 5


def ssm_credentials(
    param_name: str, ssm_client: Callable[[], Any] | None = None
) -> CredentialsProvider:
    """A credentials provider that reads the service account from SSM.

    FolioClient keeps what it returns, so SSM is read once per container.
    """

    def provide() -> tuple[str, str]:
        if ssm_client is None:
            import boto3

            client = boto3.client("ssm")
        else:
            client = ssm_client()
        value = client.get_parameter(Name=param_name, WithDecryption=True)
        creds = json.loads(value["Parameter"]["Value"])
        return creds["username"], creds["password"]

    return provide


@cache
def folio_client() -> FolioClient:
    """One client per container, so its token survives warm invocations."""
    return FolioClient(
        base_url=os.environ["OKAPI_URL"],
        tenant=os.environ["OKAPI_TENANT"],
        credentials_provider=ssm_credentials(os.environ["OKAPI_SECRET_PARAM"]),
        timeout=FOLIO_TIMEOUT_SECONDS,
    )
