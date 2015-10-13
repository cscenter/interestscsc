DROP TABLE IF EXISTS DigramToPost;
DROP TABLE IF EXISTS UnigramToPost;
DROP TABLE IF EXISTS TagToUserLJ;
DROP TABLE IF EXISTS TagToPost;
DROP TABLE IF EXISTS Digram;
DROP TABLE IF EXISTS Unigram;
DROP TABLE IF EXISTS Tag;
DROP TABLE IF EXISTS MasterTag;
DROP TABLE IF EXISTS Post;
DROP TABLE IF EXISTS UserLJ;
DROP TABLE IF EXISTS Region;


-- ? TODO �������� ����� ��������� ������� ��������

CREATE TABLE Region (
  id   BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL UNIQUE
);

CREATE TABLE UserLJ (
  id        INT PRIMARY KEY,
  nick      TEXT      NOT NULL UNIQUE,
  region_id INT       NOT NULL REFERENCES Region,
  created   TIMESTAMP NOT NULL,
  update    TIMESTAMP NOT NULL,
  fetched   TIMESTAMP NULL, -- NULL ����� ����������, ���
                                            -- �������� ����� ���� ������
  birthday  DATE      NULL,
  interests TEXT      NULL

  -- ���-�� ��� ��������?
);

CREATE TABLE Post (
  id        BIGSERIAL PRIMARY KEY,
  url       INT       NOT NULL UNIQUE, -- ����� ������ ����� �� ������ �� ����
                                            -- ��� ��� ���� <user>.lj.com/<number>
  user_id   INT       NOT NULL REFERENCES UserLJ,
  date      TIMESTAMP NOT NULL,
  title     TEXT      NOT NULL,
  text      TEXT      NOT NULL,
  text_norm TEXT      NULL,
  repostNum INT       NULL

  -- ���-�� ��� �����\����� ��������?
);

CREATE TABLE MasterTag (
  id   BIGSERIAL PRIMARY KEY,
  text TEXT NOT NULL UNIQUE
);

CREATE TABLE Tag (
  id        BIGSERIAL PRIMARY KEY,
  text      TEXT NOT NULL UNIQUE, -- ���� ����� ������� ���� ���
  master_id INT  NULL REFERENCES MasterTag, -- � ���� ���������, ����� �����
  text_norm TEXT NULL                       -- ������� � ��������� �������
);

CREATE TABLE Unigram (
  id   BIGSERIAL PRIMARY KEY,
  text TEXT NOT NULL UNIQUE
);

CREATE TABLE Digram (
  id   BIGSERIAL PRIMARY KEY,
  text TEXT NOT NULL UNIQUE
);


CREATE TABLE TagToPost (
  tag_id  INT REFERENCES Tag,
  post_id INT REFERENCES Post,
  PRIMARY KEY (tag_id, post_id)
);

CREATE TABLE TagToUserLJ (
  tag_id  INT REFERENCES Tag,
  user_id INT REFERENCES UserLJ,
  uses    INT NULL, --���������� ������������� ���������� userLJ
  PRIMARY KEY (tag_id, user_id)
);

CREATE TABLE UnigramToPost (
  unigram_id INT REFERENCES Unigram,
  post_id    INT REFERENCES Post,
  PRIMARY KEY (unigram_id, post_id)
);

CREATE TABLE DigramToPost (
  digram_id INT REFERENCES Digram,
  post_id   INT REFERENCES Post,
  PRIMARY KEY (digram_id, post_id)
);


-- ���� ��������� �������� �������� ����������.

INSERT INTO Region VALUES
  (DEFAULT, 'RU'),
  (DEFAULT, 'EN');

INSERT INTO UserLJ VALUES
  (1, 'sssmaxusss', (SELECT id
                     FROM Region
                     WHERE name = 'RU'),
   '2015-09-17T13:09:03', '2015-09-17T13:09:03', NULL, NULL, NULL),
  (2, 'mi3ch', (SELECT id
                FROM Region
                WHERE name = 'EN'),
   '2003-04-03T08:11:41', '2015-09-17T13:09:03', NULL, '1966-03-27', '������ ���� �������');
--
-- INSERT INTO Tag VALUES
--   (DEFAULT, 'coffee', NULL),
--   (DEFAULT, 'java', NULL);
--
-- INSERT INTO TagToUserLJ VALUES
--   ((SELECT id
--     FROM Tag
--     WHERE text = 'java'), 1),
--   ((SELECT id
--     FROM Tag
--     WHERE text = 'coffee'), 2);