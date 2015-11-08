DROP VIEW IF EXISTS TagNameToPost;
DROP VIEW IF EXISTS AllNGramTextPost;
DROP VIEW IF EXISTS AllNGramTexts;
DROP VIEW IF EXISTS PostUniqueWordCount;
DROP VIEW IF EXISTS PostLength;
DROP TABLE IF EXISTS TrigramToPost;
DROP TABLE IF EXISTS DigramToPost;
DROP TABLE IF EXISTS UnigramToPost;
DROP TABLE IF EXISTS TagToUserLJ;
DROP TABLE IF EXISTS TagToPost;
DROP TABLE IF EXISTS Trigram;
DROP TABLE IF EXISTS Digram;
DROP TABLE IF EXISTS Unigram;
DROP TABLE IF EXISTS Tag;
DROP TABLE IF EXISTS MasterTag;
DROP TABLE IF EXISTS Post;
DROP TABLE IF EXISTS RawUserLJ;
DROP TABLE IF EXISTS UserLJ;
DROP TABLE IF EXISTS Region;
DROP TABLE IF EXISTS Crawler;


-- ? TODO Продумать индексирование

CREATE TABLE Crawler (
  id   SERIAL PRIMARY KEY,
  name TEXT UNIQUE
);

CREATE TABLE Region (
  id   BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL UNIQUE
);

CREATE TABLE UserLJ (
  id        BIGSERIAL PRIMARY KEY,
  nick      TEXT      NOT NULL UNIQUE,
  region_id BIGINT    NULL REFERENCES Region, -- хотели NOT NULL
  created   TIMESTAMP NULL, -- хотели NOT NULL
  update    TIMESTAMP NULL, -- хотели NOT NULL
  fetched   TIMESTAMP NULL, -- NULL здесь показывает, что
                            -- значение может быть пустым
  birthday  DATE      NULL,
  interests TEXT      NULL
);

CREATE TABLE RawUserLJ (-- Здесь сохраняем пользователей
  nick       TEXT PRIMARY KEY, -- при первом столкновении
  user_id    BIGINT UNIQUE NULL REFERENCES UserLJ,
  crawler_id INT           NULL REFERENCES Crawler
);

CREATE TABLE Post (
  id         BIGSERIAL PRIMARY KEY,
  url        INT       NOT NULL, -- номер из ссылки <user>.lj.com/<number>
  user_id    BIGINT    NOT NULL REFERENCES UserLJ,
  date       TIMESTAMP NOT NULL,
  title      TEXT      NOT NULL,
  text       TEXT      NOT NULL,
  normalized BOOLEAN   NOT NULL DEFAULT FALSE,
  comments   INT       NULL,
  UNIQUE (user_id, url)
);

CREATE TABLE MasterTag (
  id   BIGSERIAL PRIMARY KEY,
  text TEXT NOT NULL UNIQUE
);

CREATE TABLE Tag (
  id        BIGSERIAL PRIMARY KEY,
  text      TEXT   NOT NULL UNIQUE, -- Если хотим связать один тэг
  master_id BIGINT NULL REFERENCES MasterTag, -- с неск мастерами, связь нужно
  text_norm TEXT   NULL                       -- вынести в отдельную таблицу
);

CREATE TABLE Unigram (
  id   BIGSERIAL PRIMARY KEY,
  text TEXT NOT NULL UNIQUE
);

CREATE TABLE Digram (
  id   BIGSERIAL PRIMARY KEY,
  text TEXT NOT NULL UNIQUE
);

CREATE TABLE Trigram (
  id   BIGSERIAL PRIMARY KEY,
  text TEXT NOT NULL UNIQUE
);


CREATE TABLE TagToPost (
  tag_id  BIGINT REFERENCES Tag,
  post_id BIGINT REFERENCES Post,
  PRIMARY KEY (tag_id, post_id)
);

CREATE TABLE TagToUserLJ (
  tag_id  BIGINT REFERENCES Tag,
  user_id BIGINT REFERENCES UserLJ,
  uses    INT NULL, --количество использований конкретным userLJ
  PRIMARY KEY (tag_id, user_id)
);

CREATE TABLE UnigramToPost (
  ngram_id   BIGINT REFERENCES Unigram,
  post_id    BIGINT REFERENCES Post,
  uses_str   TEXT NULL, -- здесь и далее, использов в конкр посте
  uses_cnt   INT  NULL,
  PRIMARY KEY (ngram_id, post_id)
);

CREATE TABLE DigramToPost (
  ngram_id  BIGINT REFERENCES Digram,
  post_id   BIGINT REFERENCES Post,
  uses_str  TEXT NULL,
  uses_cnt  INT  NULL,
  PRIMARY KEY (ngram_id, post_id)
);

CREATE TABLE TrigramToPost (
  ngram_id   BIGINT REFERENCES Trigram,
  post_id    BIGINT REFERENCES Post,
  uses_str   TEXT NULL,
  uses_cnt   INT  NULL,
  PRIMARY KEY (ngram_id, post_id)
);

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
