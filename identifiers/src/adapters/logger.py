"""
Contextual logging implementation using structlog.

Adapted from catalogue-pipeline/catalogue_graph/src/utils/logger.py
"""

import logging
import os
import sys
from typing import Any

import structlog

log_level = os.environ.get(
    "LOG_LEVEL", "INFO"
)  # we can adjust the log level in tf. Defaults to INFO in local

# Third-party loggers that are excessively chatty at DEBUG level. Each is
# clamped to its mapped level regardless of the root LOG_LEVEL so that running
# the app at DEBUG doesn't drown our own logs in library noise.
NOISY_LIBRARY_LOGGERS: dict[str, str] = {
    "botocore": "INFO",
    "urllib3": "INFO",
}


def setup_structlog() -> None:
    """
    Configure structlog for structured contextual logging.
    """

    # Configure processors
    processors: list[structlog.types.Processor] = [
        # Add log level to event dict
        structlog.stdlib.add_log_level,
        # Add logger name
        structlog.stdlib.add_logger_name,
        # Add timestamp
        structlog.processors.TimeStamper(fmt="iso"),
        # Merge in bound contextvars
        structlog.contextvars.merge_contextvars,
        # Filter out None values
        lambda _, __, event_dict: {
            k: v for k, v in event_dict.items() if v is not None
        },
        # Choose renderer based on environment
        *_get_renderers(),
    ]

    # Configure structlog
    structlog.configure(
        processors=processors,
        wrapper_class=SingleLineExceptionLogger,
        logger_factory=structlog.stdlib.LoggerFactory(),
        cache_logger_on_first_use=os.environ.get("LOG_CACHE_LOGGERS") != "false",
    )

    # Configure standard library logging to output to stderr
    logging.basicConfig(
        format="%(message)s",
        level=getattr(logging, log_level.upper()),
        stream=sys.stderr,
    )


class SingleLineExceptionLogger(structlog.stdlib.BoundLogger):
    """
    Logs exceptions through error(), as stdlib's Logger.exception would print
    the traceback after the JSON line, where the log forwarder cannot parse it.
    """

    def exception(self, event: str | None = None, *args: Any, **kw: Any) -> Any:
        kw.setdefault("exc_info", True)
        return self.error(event, *args, **kw)


def _get_renderers() -> list[structlog.types.Processor]:
    """Get appropriate renderers based on environment."""

    if hasattr(sys.stderr, "isatty") and sys.stderr.isatty():
        # Colored console output for local development
        return [structlog.dev.ConsoleRenderer(colors=True)]
    else:
        # JSON output for production/containers
        return [
            structlog.processors.format_exc_info,
            structlog.processors.JSONRenderer(),
        ]


def setup_logging() -> None:
    """
    Set up structlog.
    """
    setup_structlog()
    # Force the root logger to desired level to override any AWS Lambda defaults
    logging.getLogger().setLevel(log_level)

    # Clamp noisy third-party loggers so application DEBUG runs are still
    # readable.
    root_level = logging.getLogger().getEffectiveLevel()
    for logger_name, level in NOISY_LIBRARY_LOGGERS.items():
        clamped = max(root_level, logging.getLevelNamesMapping()[level])
        logging.getLogger(logger_name).setLevel(clamped)
