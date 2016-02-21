-- =======================================
-- 15_11_04

ALTER TABLE Post RENAME COLUMN text_norm TO normalized;
ALTER TABLE Post
ALTER COLUMN normalized SET DATA TYPE BOOLEAN USING (normalized::BOOLEAN),
ALTER COLUMN normalized SET DEFAULT FALSE;
UPDATE Post SET normalized = FALSE;
ALTER TABLE Post
ALTER COLUMN normalized SET NOT NULL;

ALTER TABLE UnigramToPost RENAME COLUMN unigram_id TO ngram_id;
ALTER TABLE DigramToPost RENAME COLUMN digram_id TO ngram_id;
ALTER TABLE TrigramToPost RENAME COLUMN trigram_id TO ngram_id;

ALTER TABLE UnigramToPost
ADD COLUMN uses_str   TEXT NULL,
ADD COLUMN uses_cnt   INT  NULL;
ALTER TABLE DigramToPost
ADD COLUMN uses_str   TEXT NULL,
ADD COLUMN uses_cnt   INT  NULL;
ALTER TABLE TrigramToPost
ADD COLUMN uses_str   TEXT NULL,
ADD COLUMN uses_cnt   INT  NULL;


CREATE VIEW PostLength AS (
  SELECT post_id, sum(uses_cnt) length
  FROM UnigramToPost
  GROUP BY post_id
);

CREATE VIEW PostUniqueWordCount AS (
  SELECT post_id, count(*) count
  FROM UnigramToPost
  GROUP BY post_id
);

CREATE VIEW AllNGramTexts AS (
  SELECT text FROM Unigram
  UNION ALL
  SELECT text FROM Digram
  UNION ALL
  SELECT text FROM Trigram
);

CREATE VIEW AllNGramTextPost AS (
  SELECT u.text, up.post_id
  FROM Unigram u JOIN UnigramToPost up ON u.id = up.ngram_id
  UNION ALL
  SELECT d.text, dp.post_id
  FROM Digram d JOIN DigramToPost dp ON d.id = dp.ngram_id
  UNION ALL
  SELECT t.text, tp.post_id
  FROM Trigram t JOIN TrigramToPost tp ON t.id = tp.ngram_id
);

CREATE VIEW TagNameToPost AS (
  SELECT t.text, tp.post_id
  FROM Tag t
    JOIN TagToPost tp ON t.id = tp.tag_id
);

-- =======================================
-- 15_11_13

CREATE TABLE Normalizer (
  id   SERIAL PRIMARY KEY,
  name TEXT UNIQUE
);

ALTER TABLE Post ALTER COLUMN url SET DATA TYPE BIGINT USING (url::BIGINT);