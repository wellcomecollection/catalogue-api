-- Seed data for the Identifiers API prototype.
--
-- Chosen to exercise every interesting case the contract has to handle:
--   a2345bcd  Work  : Sierra original + Axiell alias (one-to-many, isAlias)
--   ka345678  Item  : Sierra item original + FOLIO item alias (the requesting case)
--   mn23pqrs  Image : single source (no alias)
--   ze789fgh  mixed : a Work row + an Image row share one canonical id (cross-type)
--   cn234567  Concept : Concept rows only (a type outside the contract enum, so 404)
--   cp234567  mixed : Concept original + Work alias (original out of enum, so 404)
--   cw234567  mixed : Work original + Work alias + Concept alias (Concept row omitted)
--   ms234567  Image : a METS source id containing `/`, which arrives as `%2F`

INSERT INTO identifiers (ontology_type, source_system, source_id, canonical_id, created_at) VALUES
  ('Work',  'sierra-system-number',  'b1161044x', 'a2345bcd', '2019-03-04T10:14:22Z'),
  ('Work',  'axiell-collections-id', '12345',     'a2345bcd', '2026-02-10T12:00:00Z'),
  ('Item',  'sierra-item-number',    'i17777',    'ka345678', '2020-01-01T00:00:00Z'),
  ('Item',  'folio-item',            '3f2a...uuid','ka345678','2026-02-10T12:00:00Z'),
  ('Image', 'miro-image-number',     'V0012345',  'mn23pqrs', '2018-06-01T00:00:00Z'),
  ('Work',  'axiell-collections-id', '67890',     'ze789fgh', '2026-02-10T12:00:00Z'),
  ('Image', 'miro-image-number',     'V0067890',  'ze789fgh', '2019-09-09T00:00:00Z'),
  ('Concept', 'lc-subjects',         'sh85083064', 'cn234567', '2021-05-01T00:00:00Z'),
  ('Concept', 'lc-subjects',         'sh00000001', 'cp234567', '2019-01-01T00:00:00Z'),
  ('Work',  'sierra-system-number',  'b20000001', 'cp234567', '2026-02-10T12:00:00Z'),
  ('Work',  'sierra-system-number',  'b30000001', 'cw234567', '2019-01-01T00:00:00Z'),
  ('Work',  'axiell-collections-id', '30001',     'cw234567', '2026-02-10T12:00:00Z'),
  ('Concept', 'lc-subjects',         'sh00000002', 'cw234567', '2026-03-01T00:00:00Z'),
  ('Image', 'mets-image',            'b33012246/FILE_0170_OBJECTS', 'ms234567', '2026-03-01T00:00:00Z');
