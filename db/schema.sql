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


-- ? TODO Возможно нужно прописать касдады удаления

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

  -- Что-то еще добавить?
);

CREATE TABLE RawUserLJ (-- Здесь сохраняем пользователей
  nick    TEXT PRIMARY KEY, -- при первом столкновении
  user_id BIGINT REFERENCES UserLJ
);

CREATE TABLE Post (
  id        BIGSERIAL PRIMARY KEY,
  url      INT    NOT NULL, -- здесь храним номер из ссылки на пост
                                -- они все вида <user>.lj.com/<number>
  user_id  BIGINT NOT NULL REFERENCES UserLJ,
  date      TIMESTAMP NOT NULL,
  title     TEXT      NOT NULL,
  text      TEXT      NOT NULL,
  text_norm TEXT      NULL,
  comments INT    NULL,
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
  unigram_id BIGINT REFERENCES Unigram,
  post_id    BIGINT REFERENCES Post,
  PRIMARY KEY (unigram_id, post_id)
);

CREATE TABLE DigramToPost (
  digram_id BIGINT REFERENCES Digram,
  post_id   BIGINT REFERENCES Post,
  PRIMARY KEY (digram_id, post_id)
);

CREATE TABLE TrigramToPost (
  trigram_id BIGINT REFERENCES Trigram,
  post_id    BIGINT REFERENCES Post,
  PRIMARY KEY (trigram_id, post_id)
);