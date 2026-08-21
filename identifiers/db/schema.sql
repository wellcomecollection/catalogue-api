-- Identifiers API prototype schema.
--
-- A portable (SQLite/Postgres) rendering of the RFC 083 "ID Registry" two-table
-- model. The production store is Aurora MySQL; the column shapes here match the
-- design doc's prototype DDL. This API only ever READS these tables — all writes
-- belong to the ID Minter (RFC 083).

CREATE TABLE canonical_ids (
  canonical_id  VARCHAR(8) PRIMARY KEY,
  status        TEXT NOT NULL DEFAULT 'free'
                CHECK (status IN ('free', 'assigned')),
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE identifiers (
  ontology_type  TEXT NOT NULL,
  source_system  TEXT NOT NULL,
  source_id      TEXT NOT NULL,
  canonical_id   VARCHAR(8) NOT NULL REFERENCES canonical_ids(canonical_id),
  created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- Reverse lookup is a point read on this three-part key.
  PRIMARY KEY (ontology_type, source_system, source_id)
);

-- Forward lookup (canonical_id -> N source rows) rides this index.
CREATE INDEX idx_canonical ON identifiers (canonical_id);
