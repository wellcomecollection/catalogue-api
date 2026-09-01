"""The storage seam.

`core` depends only on this Protocol, never on a concrete store. The prototype
ships one implementation (``adapters/sqlite_repo.py``); the production target is
a second implementation over Aurora via the RDS Data API. Swapping stores
touches nothing in `core`.

A Repository performs only the two read queries the API needs and returns raw
rows — it does not order, derive ``isAlias``, or know about HTTP. All lookup
semantics live in ``core/service.py``.
"""

from typing import Protocol

from core.models import SourceRow


class Repository(Protocol):
    def get_by_canonical(self, canonical_id: str) -> list[SourceRow]:
        """All source rows for a canonical id (unordered). Empty if none."""
        ...

    def get_by_source(
        self, ontology_type: str, source_system: str, source_id: str
    ) -> str | None:
        """The canonical id for a (type, system, value) tuple, or None.

        A point read on the three-part primary key.
        """
        ...
