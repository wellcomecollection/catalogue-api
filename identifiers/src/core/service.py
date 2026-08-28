"""Lookup logic: ordering, isAlias derivation, ETag, cache policy, 404 rules.

Framework- and storage-agnostic. The service raises domain errors
(``BadRequest`` / ``NotFound``) that the handler maps to HTTP status codes; it
never imports an HTTP framework or a database driver.
"""

from dataclasses import dataclass

from core import validation
from core.models import CanonicalIdRef, IdentifierSet, SourceIdentifier, SourceRow
from core.repository import Repository

# --- Prototype defaults (NOT contract decisions — see README) ---------------
# Bounded TTL while the alias set can still grow during the migration window.
# A real value is an open question in the design doc; 300s is a placeholder.
FORWARD_MAX_AGE = 300
# The bare reverse lookup (source -> canonicalId) is immutable once minted, so
# it is cached hard. Placeholder; the design doc relaxes this post-switchover.
REVERSE_BARE_MAX_AGE = 86400
# ----------------------------------------------------------------------------


class IdentifiersError(Exception):
    """Base for domain errors carrying a stable code + human message."""

    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code
        self.message = message


class BadRequest(IdentifiersError):
    """Maps to 400 — gateway-level validation failure."""


class NotFound(IdentifiersError):
    """Maps to 404 — no mapping for the supplied id/tuple."""


@dataclass(frozen=True)
class LookupResult:
    """A response body plus the caching headers the handler should emit."""

    body: IdentifierSet | CanonicalIdRef
    cache_control: str
    etag: str | None = None


class IdentifiersService:
    def __init__(self, repo: Repository):
        self._repo = repo

    # -- Forward: canonicalId -> full IdentifierSet --------------------------

    def resolve_canonical(self, canonical_id: str) -> LookupResult:
        if not validation.is_valid_canonical_id(canonical_id):
            raise BadRequest(
                "badRequest", "canonicalId does not match the required format"
            )
        rows = self._repo.get_by_canonical(canonical_id)
        if not rows:
            # Covers an unknown id AND a pre-generated-but-unassigned canonical
            # id (exists in canonical_ids, no identifiers rows yet).
            raise NotFound("notFound", "no mapping found")
        return self._set_result(canonical_id, rows)

    # -- Reverse: source tuple -> canonicalId (optionally + siblings) --------

    def resolve_source(
        self,
        source_system: str,
        value: str,
        type_: str | None,
        include: str | None,
    ) -> LookupResult:
        ontology_type = type_ or validation.DEFAULT_TYPE
        if not validation.is_valid_type(ontology_type):
            raise BadRequest("badRequest", "unsupported type")
        if include is not None and not validation.is_valid_include(include):
            raise BadRequest("badRequest", "unsupported include value")
        # sourceSystem/value are heterogeneous and not pattern-validated: an
        # unknown tuple resolves to 404, never 400.
        canonical_id = self._repo.get_by_source(ontology_type, source_system, value)
        if canonical_id is None:
            raise NotFound("notFound", "no mapping found")

        if include == "siblings":
            rows = self._repo.get_by_canonical(canonical_id)
            if not rows:
                raise NotFound("notFound", "no mapping found")
            return self._set_result(canonical_id, rows)

        # Bare reverse lookup is immutable once minted -> long TTL, no ETag.
        return LookupResult(
            body=CanonicalIdRef(canonical_id=canonical_id),
            cache_control=f"public, max-age={REVERSE_BARE_MAX_AGE}",
        )

    # -- Shared set construction (forward and include=siblings) --------------

    def _set_result(self, canonical_id: str, rows: list[SourceRow]) -> LookupResult:
        ordered = _order_rows(rows)
        # Derived from createdAt, not from position: the response is ordered
        # newest first, so the original is last rather than first.
        original = _original_row(ordered)
        identifiers = [
            SourceIdentifier(
                type=row.ontology_type,
                source_system=row.source_system,
                value=row.source_id,
                is_alias=(row is not original),
                created_at=row.created_at,
            )
            for row in ordered
        ]
        # The original's type, so this is unambiguous even for a mixed-type set.
        top_level_type = original.ontology_type
        return LookupResult(
            body=IdentifierSet(
                canonical_id=canonical_id,
                type=top_level_type,
                source_identifiers=identifiers,
            ),
            cache_control=f"public, max-age={FORWARD_MAX_AGE}",
            etag=_etag(ordered),
        )


def _order_rows(rows: list[SourceRow]) -> list[SourceRow]:
    """Descending by createdAt, most recent first, then source_system.

    ISO-8601 UTC timestamps in a fixed format sort correctly as strings.
    """
    return sorted(rows, key=lambda r: (r.created_at, r.source_system), reverse=True)


def _original_row(rows: list[SourceRow]) -> SourceRow:
    """The earliest row: the original, from which every later row is inherited."""
    return min(rows, key=lambda r: (r.created_at, r.source_system))


def _etag(ordered_rows: list[SourceRow]) -> str:
    """Weak validator from (row_count, max(createdAt)).

    Changes exactly when an alias is added, so revalidation is a cheap 304 until
    the set actually grows. e.g. W/"2-2026-02-10T12:00:00Z".
    """
    row_count = len(ordered_rows)
    max_created_at = max(r.created_at for r in ordered_rows)
    return f'W/"{row_count}-{max_created_at}"'
