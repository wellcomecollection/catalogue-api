"""SQLite implementation of the Repository protocol.

The only place in the prototype that knows SQL or a DB driver. The production
target is a second Repository over Aurora via the RDS Data API; `core` is
unaffected by the swap.
"""

import sqlite3
from pathlib import Path

from core.models import SourceRow

_DB_DIR = Path(__file__).parent / "db"
_SCHEMA = _DB_DIR / "schema.sql"
_SEED = _DB_DIR / "seed.sql"


def build_seeded_connection(db_path: str = ":memory:") -> sqlite3.Connection:
    """A fresh connection with schema + seed applied.

    Defaults to an in-memory DB so every run/test starts from the same known
    fixtures (reproducibility over persistence — a prototype default).
    ``check_same_thread=False`` lets a threaded local server share one handle.
    """
    conn = sqlite3.connect(db_path, check_same_thread=False)
    conn.executescript(_SCHEMA.read_text())
    conn.executescript(_SEED.read_text())
    conn.commit()
    return conn


class SqliteRepository:
    """Performs only the two read queries the API needs; returns raw rows.

    Ordering and isAlias derivation are the service's job, not the store's.
    """

    def __init__(self, connection: sqlite3.Connection):
        self._conn = connection

    def get_by_canonical(self, canonical_id: str) -> list[SourceRow]:
        cursor = self._conn.execute(
            "SELECT ontology_type, source_system, source_id, created_at "
            "FROM identifiers WHERE canonical_id = ?",
            (canonical_id,),
        )
        return [SourceRow(*row) for row in cursor.fetchall()]

    def get_by_source(
        self, ontology_type: str, source_system: str, source_id: str
    ) -> str | None:
        cursor = self._conn.execute(
            "SELECT canonical_id FROM identifiers "
            "WHERE ontology_type = ? AND source_system = ? AND source_id = ?",
            (ontology_type, source_system, source_id),
        )
        row = cursor.fetchone()
        return row[0] if row else None
