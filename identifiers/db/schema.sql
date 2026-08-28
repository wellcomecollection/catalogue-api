-- Identifiers API prototype schema.
--
-- A portable (SQLite/Postgres) rendering of the `identifiers` table from the
-- RFC 083 "ID Registry". The registry also has a `canonical_ids` table holding
-- the pre-generated id pool, which this API never reads, so it is not modelled
-- here. The production store is Aurora MySQL. This API only ever READS this
-- table — all writes belong to the ID Minter (RFC 083).

CREATE TABLE identifiers (
  ontology_type  TEXT NOT NULL,
  source_system  TEXT NOT NULL,
  source_id      TEXT NOT NULL,
  canonical_id   VARCHAR(8) NOT NULL,
  created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- Reverse lookup is a point read on this three-part key.
  PRIMARY KEY (ontology_type, source_system, source_id)
);

-- Forward lookup (canonical_id -> N source rows) rides this index.
CREATE INDEX idx_canonical ON identifiers (canonical_id);
