"""The log level setting."""

import importlib
import logging
from collections.abc import Iterator

import pytest
import structlog

import logger


@pytest.fixture
def restore_logging() -> Iterator[None]:
    saved = structlog.get_config()
    root_level = logging.getLogger().level
    yield
    importlib.reload(logger)
    structlog.configure(**saved)
    logging.getLogger().setLevel(root_level)


def test_a_lowercase_log_level_is_accepted(
    monkeypatch: pytest.MonkeyPatch, restore_logging: None
) -> None:
    """logging rejects "debug", which would fail the Lambda at import."""
    monkeypatch.setenv("LOG_LEVEL", "debug")
    reloaded = importlib.reload(logger)

    reloaded.setup_logging()

    assert logging.getLogger().level == logging.DEBUG
    monkeypatch.undo()
