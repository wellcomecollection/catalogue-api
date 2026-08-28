"""Domain models for the Identifiers API.

Each model knows how to render itself to the contract's JSON shape (camelCase
keys) via ``to_dict``. The models are storage- and framework-agnostic; a
Repository yields ``SourceRow``s, the service derives ``isAlias`` and ordering,
and the handler serialises the result.
"""

from dataclasses import dataclass


@dataclass(frozen=True)
class SourceRow:
    """A raw mapping row as stored, before any lookup-time derivation.

    ``is_alias`` is intentionally absent: it is not stored, it is derived by the
    service from ``created_at`` ordering across the set.
    """

    ontology_type: str
    source_system: str
    source_id: str
    created_at: str  # ISO-8601 UTC, e.g. "2019-03-04T10:14:22Z"


@dataclass(frozen=True)
class SourceIdentifier:
    """One element of an IdentifierSet (identical shape on both endpoints)."""

    type: str
    source_system: str
    value: str
    is_alias: bool
    created_at: str

    def to_dict(self) -> dict:
        return {
            "type": self.type,
            "sourceSystem": self.source_system,
            "value": self.value,
            "isAlias": self.is_alias,
            "createdAt": self.created_at,
        }


@dataclass(frozen=True)
class IdentifierSet:
    """A canonical id and the full set of source identifiers sharing it.

    ``type`` is a convenience copy of the original row's ontology type (the
    single ``isAlias=false`` row), so a caller can read the canonical id's type
    without scanning ``source_identifiers``. With cross-type predecessors it
    reflects the original and may differ from a later alias's per-row ``type``.
    """

    canonical_id: str
    type: str
    source_identifiers: list[SourceIdentifier]

    def to_dict(self) -> dict:
        return {
            "canonicalId": self.canonical_id,
            "type": self.type,
            "sourceIdentifiers": [s.to_dict() for s in self.source_identifiers],
        }


@dataclass(frozen=True)
class CanonicalIdRef:
    """Bare canonical id reference (default reverse-lookup response)."""

    canonical_id: str

    def to_dict(self) -> dict:
        return {"canonicalId": self.canonical_id}
