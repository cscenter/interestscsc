-- =======================================
-- 15_11_04

-- noinspection SqlResolve
ALTER TABLE Post RENAME COLUMN text_norm TO normalized;
ALTER TABLE Post
ALTER COLUMN normalized SET DATA TYPE BOOLEAN USING (normalized::BOOLEAN),
ALTER COLUMN normalized SET DEFAULT FALSE;
UPDATE Post SET normalized = FALSE;
ALTER TABLE Post
ALTER COLUMN normalized SET NOT NULL;

-- noinspection SqlResolve
ALTER TABLE UnigramToPost RENAME COLUMN unigram_id TO ngram_id;
-- noinspection SqlResolve
ALTER TABLE DigramToPost RENAME COLUMN digram_id TO ngram_id;
-- noinspection SqlResolve
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

-- =======================================
-- 16_02_26

CREATE INDEX unigram_to_post ON unigramtopost(post_id);
CREATE INDEX digram_to_post ON digramtopost(post_id);
CREATE INDEX trigram_to_post ON trigramtopost(post_id);
CREATE INDEX tag_to_post ON tagtopost(post_id);
CREATE INDEX tf_idf_test_tf_idf ON tf_idf_test(tf_idf);

CREATE MATERIALIZED VIEW tf_idf_test AS (
  WITH allNGrams AS (
    SELECT 1 as type, up.ngram_id, up.post_id, up.uses_cnt, ttp.tag_id
    FROM unigramtopost up JOIN tagtopost ttp USING(post_id)
    --     JOIN unigram u ON up.ngram_id = u.id
    UNION ALL
    SELECT 2 as type, dp.ngram_id, dp.post_id, dp.uses_cnt, ttp.tag_id
    FROM digramtopost dp JOIN tagtopost ttp USING(post_id)
    --     JOIN digram d ON dp.ngram_id = d.id
    UNION ALL
    SELECT 3 as type, tp.ngram_id, tp.post_id, tp.uses_cnt, ttp.tag_id
    FROM trigramtopost tp JOIN tagtopost ttp USING(post_id)
    --     JOIN trigram t ON tp.ngram_id = t.id
  ),
      totalWordCountByTag AS (
        SELECT tag_id, sum(uses_cnt) as val
        FROM allNGrams
        GROUP BY tag_id
    ),
      wordCountByTag AS (
        SELECT tag_id, type, ngram_id, sum(uses_cnt) as val
        FROM allNGrams
        GROUP BY tag_id, type, ngram_id
    ),
    -- Сейчас учитывается число только тех тегов,
    -- которые имеют нормализованные посты
      totalTagCount AS (
        SELECT count(DISTINCT tag_id) as val
        FROM allNGrams
    ),
      tagCountByWord AS (
        SELECT type, ngram_id, count(*) as val
        FROM allNGrams
        GROUP BY type, ngram_id
    )
  SELECT
    wCBT.tag_id,
    wCBT.type,
    wCBT.ngram_id,
    wCBT.val :: FLOAT / tWCBT.val AS tf,
    -- Сейчас логарифм десятичный
    log((SELECT val FROM totalTagCount)::FLOAT / tCBW.val) AS idf,
    (wCBT.val :: FLOAT / tWCBT.val) * (log((SELECT val FROM totalTagCount)::FLOAT / tCBW.val)) as tf_idf
  FROM wordCountByTag wCBT
    JOIN totalWordCountByTag tWCBT USING(tag_id)
    JOIN tagCountByWord tCBW USING(type, ngram_id)
);

-- REFRESH MATERIALIZED VIEW tf_idf_test;
