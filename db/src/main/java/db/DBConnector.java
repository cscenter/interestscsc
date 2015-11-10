package db;

import data.NGram;
import data.Post;
import data.Tag;
import data.User;
import org.postgresql.ds.PGPoolingDataSource;
import org.postgresql.util.PSQLException;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;


/**
 * User: allight
 * Date: 06.10.2015 14:47
 */

@SuppressWarnings("Duplicates")
public class DBConnector {
    private static final String SCHEMA_PATH = "db/schema.sql";
    private static final String SCHEMA_ENCODING = "UTF-8";

    private static final String HOST = "185.72.144.129";
    private static final int PORT = 5432; // стандартный порт в постгрес
    private static final String DB = "veiloneru";
    private static final String USER = "veiloneru";
    private static final String PASS = "wasddsaw";
    private static final int MAX_CONNECTIONS = 20; // * 5 < 100 - внутреннее ограничение постгрес

    private static final int MAX_TRIES = 5; // TODO число попыток выполнения при временной (*) неудаче (>1)
    private static final String DROPDATA_PASS = "Bzw7HPtmHmVVqKvSHe7d";
    private static final String[] N_GRAM_TABLE_NAMES = new String[]{"", "unigram", "digram", "trigram"};
    private int crawlerId;

    private PGPoolingDataSource connectionPool;


    public DBConnector(String crawlerName) throws SQLException {
        connectionPool = new PGPoolingDataSource();
        connectionPool.setServerName(HOST);
        connectionPool.setPortNumber(PORT);
        connectionPool.setDatabaseName(DB);
        connectionPool.setUser(USER);
        connectionPool.setPassword(PASS);
        connectionPool.setMaxConnections(MAX_CONNECTIONS);
        if(checkTable("crawler"))
            crawlerId = getCrawlerId(crawlerName);
    }


    private int getCrawlerId(String crawlerName) throws SQLException {
        String insertCrawlerString =
                "INSERT INTO Crawler (name) VALUES (?);";
        String selectCrawlerString = "SELECT id FROM Crawler WHERE name = ?;";
        try (
                Connection con = getConnection();
                PreparedStatement insertCrawler = con.prepareStatement(insertCrawlerString);
                PreparedStatement selectCrawler = con.prepareStatement(selectCrawlerString)
        ) {
            insertCrawler.setString(1, crawlerName);
            tryUpdateTransaction(insertCrawler, crawlerName, "Crawler");
            selectCrawler.setString(1, crawlerName);
            ResultSet rs = tryQueryTransaction(selectCrawler, "Crawler");
            if (rs == null || !rs.next())
                throw new IllegalStateException("If you see this, our code needs a fix");
            return rs.getInt("id");
        }
    }


    private boolean checkTable(String tableName) throws SQLException {
        String selectTableString = "SELECT * FROM pg_catalog.pg_tables WHERE tablename = ?;";
        try (
                Connection con = getConnection();
                PreparedStatement selectTable = con.prepareStatement(selectTableString)
        ) {
            selectTable.setString(1, tableName);
            ResultSet rs = tryQueryTransaction(selectTable, "pg_tables");
            if (rs == null)
                throw new IllegalStateException("If you see this, our code needs a fix");
            return rs.next();
        }
    }


    /**
     * Выдает соединение для использования в качестве контекстного объекта.
     * ! Необходимо самостоятельно вернуть соединение в пул методом
     * myDBConnector.closeConnection(conn) или conn.close(). Лучше всего
     * использовать try-with-resources (см., например, метод dropInitDatabase).
     */
    public Connection getConnection() throws SQLException {
        return connectionPool.getConnection();
    }


    /**
     * Возвращает соединение в очередь.
     *
     * @deprecated Лучше использовать соедиление в качестве ресурса в
     * try-with-resources (см., например, метод dropInitDatabase).
     */
    public void closeConnection(Connection connectionToClose) throws SQLException {
        //noinspection EmptyTryBlock
        try (Connection autoclose = connectionToClose) {
        }
    }


    /**
     * Пересоздает базу из схемы, указанной в SCHEMA_PATH с кодировкой
     * SCHEMA_ENCODING
     */
    public DBConnector dropInitDatabase(String newCrawlerName, String pass) throws FileNotFoundException, SQLException {
        if (!DROPDATA_PASS.equalsIgnoreCase(pass))
            throw new IllegalArgumentException("Maybe you shouldn't drop db?");
        String schemaSQL = new Scanner(new File(SCHEMA_PATH), SCHEMA_ENCODING)
                .useDelimiter("\\Z").next();
        try (Connection conn = getConnection()) {
            conn.createStatement().execute(schemaSQL);
        }
        crawlerId = getCrawlerId(newCrawlerName);
        return this;
    }


    /**
     * Добавляет в БД регион.
     *
     * @return кол-во добавленных записей
     */
    public int insertRegion(String region) throws SQLException {
        int rowsAffected = 0;
        String insertRegionString =
                "INSERT INTO Region (name) VALUES (?);";
        try (
                Connection con = getConnection();
                PreparedStatement insertRegion = con.prepareStatement(insertRegionString)
        ) {
            insertRegion.setString(1, region);
            rowsAffected += tryUpdateTransaction(insertRegion, region, "Region");
        }
        return rowsAffected;
    }


    public List<String> getRegions() throws SQLException {
        List<String> result = new LinkedList<>();
        String selectRegionsString = "SELECT name FROM Region;";
        try (
                Connection con = getConnection();
                PreparedStatement selectRegions = con.prepareStatement(selectRegionsString);
        ) {
            ResultSet rs = tryQueryTransaction(selectRegions, "Region");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getString("name"));
        }
        return result;
    }


    /**
     * Поочередно добавляет в БД пользователей из любого итерабельного контейнера.
     * Информация о регионах добавляемых пользователей уже должна быть в базе.
     *
     * @return кол-во добавленных записей
     */
    public int insertRawUsers(Iterable<String> rawUsersLJ) throws SQLException {
        int rowsAffected = 0;
        String insertUserString =
                "INSERT INTO RawUserLJ (nick) VALUES (?);";
        try (
                Connection con = getConnection();
                PreparedStatement insertUser = con.prepareStatement(insertUserString)
        ) {
            for (String user : rawUsersLJ) {
                insertUser.setString(1, user);
                rowsAffected += tryUpdateTransaction(insertUser, user, "RawUserLJ");
            }
        }
        return rowsAffected;
    }


    /**
     * Резервирует в требуемое количество необработанных пользователей под указанный краулер.
     * Название краулера уже должно быть в таблице Crawler.
     *
     * @return кол-во добавленных записей
     */
    public int reserveRawUserForCrawler(int reserveNum) throws SQLException {
        if (reserveNum <= 0) throw new IllegalArgumentException("Argument reserveNum must be greater than 0.");
        int rowsAffected = 0;
        String reserveUserNicksString =
                "UPDATE RawUserLJ r SET crawler_id = ? " +
                        "FROM ( " +
                        "       SELECT nick FROM RawUserLJ r " +
                        "       WHERE r.crawler_id IS NULL AND r.user_id IS NULL " +
                        "       LIMIT ? FOR UPDATE " +
                        "     ) free " +
                        "WHERE r.nick = free.nick;";
        try (
                Connection con = getConnection();
                PreparedStatement reserveUserNicks = con.prepareStatement(reserveUserNicksString)
        ) {
            reserveUserNicks.setInt(1, crawlerId);
            reserveUserNicks.setInt(2, reserveNum);
            rowsAffected += tryUpdateTransaction(reserveUserNicks, "crawlerId = " + crawlerId, "RawUserLJ");
        }
        return rowsAffected;
    }


    public List<String> getReservedRawUsers() throws SQLException {
        List<String> result = new LinkedList<>();
        String selectReservedString = "SELECT nick FROM RawUserLJ " +
                "WHERE user_id IS NULL AND crawler_id = ?;";
        try (
                Connection con = getConnection();
                PreparedStatement selectReserved = con.prepareStatement(selectReservedString);
        ) {
            selectReserved.setInt(1, crawlerId);
            ResultSet rs = tryQueryTransaction(selectReserved, "RawUserLJ");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getString("nick"));
        }
        return result;
    }


    public List<String> getUnfinishedRawUsers() throws SQLException {
        List<String> result = new LinkedList<>();
        //TODO По максимуму сделать вьюшки для сложных запросов
        String selectUnfinishedString = "SELECT r.nick " +
                "FROM RawUserLJ r JOIN UserLJ u ON r.user_id = u.id " +
                "WHERE r.crawler_id = ? AND u.fetched IS NULL;";
        try (
                Connection con = getConnection();
                PreparedStatement selectUnfinished = con.prepareStatement(selectUnfinishedString);
        ) {
            selectUnfinished.setInt(1, crawlerId);
            ResultSet rs = tryQueryTransaction(selectUnfinished, "RawUserLJ JOIN UserLJ");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getString("nick"));
        }
        return result;
    }


    /**
     * Поочередно добавляет в БД пользователей из любого итерабельного контейнера.
     * Информация о регионах добавляемых пользователей уже должна быть в базе, их
     * имена должны присутствовать в RawUserLJ.
     *
     * @return кол-во добавленных записей
     */
    public int insertUser(User userLJ) throws SQLException { //TODO возможно, здесь нужна транзакция?
        int rowsAffected = 0;
        String insertUserString =
                "INSERT INTO UserLJ (nick, region_id, created, update, birthday, interests, " +
                        "city_cstm, posts_num, cmmnt_in, cmmnt_out, bio) VALUES " +
                        "(?, (SELECT id FROM Region WHERE name = COALESCE(?,'')), ?, ?, ?, ?, ?, ?, ?, ?, ?);";
        String selectUserLJString = "SELECT id FROM UserLJ WHERE nick = ?;";
        String insertSchoolString = "INSERT INTO School (name) VALUES (?);";
        String insertUserToSchoolString = "INSERT INTO UserToSchool " +
                "(user_id, school_id, start_date, finish_date) " +
                "VALUES (?, (SELECT id FROM School WHERE name = ?), ?, ?);";
        String updateRawUserString =
                "UPDATE RawUserLJ SET user_id = ? WHERE nick = ?;";

        try (
                Connection con = getConnection();
                PreparedStatement insertUser = con.prepareStatement(insertUserString);
                PreparedStatement selectUserLJ = con.prepareStatement(selectUserLJString);
                PreparedStatement insertSchool = con.prepareStatement(insertSchoolString);
                PreparedStatement insertUserToSchool = con.prepareStatement(insertUserToSchoolString);
                PreparedStatement updateRawUser = con.prepareStatement(updateRawUserString)
        ) {
            insertUser.setString(1, userLJ.getNick());
            insertUser.setString(2, userLJ.getRegion());
            insertUser.setTimestamp(3, userLJ.getDateCreated());
            insertUser.setTimestamp(4, userLJ.getDateUpdated());
            insertUser.setDate(5, userLJ.getBirthday());
            insertUser.setString(6, userLJ.getInterests());
            insertUser.setString(7, userLJ.getCustomCity());
            if (userLJ.getPostsNum() == null)
                insertUser.setNull(8, Types.INTEGER);
            else insertUser.setInt(8, userLJ.getPostsNum());
            if (userLJ.getCommentsReceived() == null)
                insertUser.setNull(9, Types.INTEGER);
            else insertUser.setInt(9, userLJ.getCommentsReceived());
            if (userLJ.getCommentsPosted() == null)
                insertUser.setNull(10, Types.INTEGER);
            else insertUser.setInt(10, userLJ.getCommentsPosted());
            insertUser.setString(11, userLJ.getBiography());

            rowsAffected += tryUpdateTransaction(insertUser, userLJ.getNick(), "UserLJ");

            selectUserLJ.setString(1, userLJ.getNick());
            ResultSet rs = tryQueryTransaction(selectUserLJ, "UserLJ");
            if (rs == null || !rs.next())
                throw new IllegalStateException("If you see this, our code needs a fix");
            Long userId = rs.getLong("id");


            for(User.School school : userLJ.getSchools()) {
                insertSchool.setString(1, school.getTitle());
                rowsAffected += tryUpdateTransaction(insertSchool, school.getTitle(), "School");
            }

            for(User.School school : userLJ.getSchools()) {
                insertUserToSchool.setLong(1, userId);
                insertUserToSchool.setString(2, school.getTitle());
                insertUserToSchool.setDate(3, school.getStart());
                insertUserToSchool.setDate(4, school.getEnd());

                rowsAffected += tryUpdateTransaction(insertUserToSchool, userLJ.getNick() + "<->" + school.getTitle(), "UserToSchool");
            }

            updateRawUser.setLong(1, userId);
            updateRawUser.setString(2, userLJ.getNick());

            rowsAffected += tryUpdateTransaction(updateRawUser, userLJ.getNick(), "RawUserLJ");
        }
        return rowsAffected;
    }


    public int updateUserFetched(String userLJNick) throws SQLException {
        int rowsAffected = 0;
        String updateFetchedString =
                "UPDATE UserLJ SET fetched = ? WHERE nick = ?;";

        try (
                Connection con = getConnection();
                PreparedStatement updateFetched = con.prepareStatement(updateFetchedString)
        ) {
            updateFetched.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            updateFetched.setString(2, userLJNick);

            rowsAffected += tryUpdateTransaction(updateFetched, userLJNick, "UserLJ");
        }
        return rowsAffected;
    }

    /**
     * Возвращвет все заполненные профили пользователей.
     * Информация о школах не включается.
     * */
    public List<User> getUsers() throws SQLException {
        List<User> result = new LinkedList<>();
        String selectUsersString = "SELECT u.id, u.nick, r.name region, " +
                "u.created, u.update, u.fetched, u.birthday, u.interests, " +
                "u.city_cstm, u.posts_num, u.cmmnt_in , u.cmmnt_out, u.bio " +
                "FROM UserLJ u JOIN Region r ON u.region_id = r.id;";
        try (
                Connection con = getConnection();
                PreparedStatement selectUsers = con.prepareStatement(selectUsersString);
        ) {
            ResultSet rs = tryQueryTransaction(selectUsers, "UserLJ");
            if (rs != null)
                while (rs.next())
                    result.add(new User(
                            rs.getLong("id"), rs.getString("nick"), rs.getString("region"),
                            rs.getTimestamp("created"), rs.getTimestamp("update"),
                            rs.getTimestamp("fetched"), rs.getDate("birthday"),
                            rs.getString("interests"), rs.getString("city_cstm"),
                            rs.getInt("posts_num"), rs.getInt("cmmnt_in"),
                            rs.getInt("cmmnt_out"), rs.getString("bio"), new LinkedList<>()
                    ));
        }
        return result;
    }


    /**
     * Поочередно добавляет в БД тэги из любого итерабельного контейнера.
     * Информация о пользователе добавляемых тэгов уже должна быть в базе,
     * в таблице UserLJ поле nick.
     *
     * @return кол-во добавленных записей
     */
    public int insertTags(Iterable<Tag> tags, String userLJNick) throws SQLException {
        int rowsAffected = 0;
        String insertTagString = "INSERT INTO Tag (text) VALUES (?);";
        String insertTagToUserLJString = "INSERT INTO TagToUserLJ (tag_id, user_id, uses) " +
                "VALUES (" +
                "(SELECT id FROM Tag WHERE text = ?), " +
                "(SELECT id FROM UserLJ WHERE nick = ?), " +
                "?" +
                ");";
        try (
                Connection con = getConnection();
                PreparedStatement insertTag = con.prepareStatement(insertTagString);
                PreparedStatement insertTagToUserLJ = con.prepareStatement(insertTagToUserLJString)
        ) {
            for (Tag tag : tags) {
                insertTag.setString(1, tag.getName());
                rowsAffected += tryUpdateTransaction(insertTag, tag.getName(), "Tag");

                insertTagToUserLJ.setString(1, tag.getName());
                insertTagToUserLJ.setString(2, userLJNick);
                if (tag.getUses() == null)
                    insertTagToUserLJ.setNull(3, Types.INTEGER);
                else insertTagToUserLJ.setInt(3, tag.getUses());
                rowsAffected += tryUpdateTransaction(insertTagToUserLJ, tag.getName() + "<->" + userLJNick, "TagToUserLJ");
            }
        }
        return rowsAffected;
    }


    public List<String> getAllTagNames() throws SQLException {
        List<String> result = new LinkedList<>();
        String selectTagString = "SELECT text FROM Tag;";
        try (
                Connection con = getConnection();
                PreparedStatement selectTag = con.prepareStatement(selectTagString);
        ) {
            ResultSet rs = tryQueryTransaction(selectTag, "Tag");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getString("text"));
        }
        return result;
    }


    public List<String> getAllTagNames(String userLJNick) throws SQLException {
        List<String> result = new LinkedList<>();
        String selectTagString = "SELECT text FROM Tag t " +
                "JOIN TagToUserLJ tu ON t.id = tu.tag_id " +
                "WHERE tu.user_id = (SELECT id FROM UserLJ WHERE nick = ?);";
        try (
                Connection con = getConnection();
                PreparedStatement selectTag = con.prepareStatement(selectTagString);
        ) {
            selectTag.setString(1, userLJNick);
            ResultSet rs = tryQueryTransaction(selectTag, "Tag");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getString("text"));
        }
        return result;
    }


    public List<String> getAllTagNames(long post_id) throws SQLException {
        List<String> result = new LinkedList<>();
        String selectTagNamesString = "SELECT text FROM TagNameToPost tnp " +
                "WHERE tnp.post_id = ?;";
        try (
                Connection con = getConnection();
                PreparedStatement selectTagNames = con.prepareStatement(selectTagNamesString);
        ) {
            selectTagNames.setLong(1, post_id);
            ResultSet rs = tryQueryTransaction(selectTagNames, "TagNameToPost");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getString("text"));
        }
        return result;
    }


    /**
     * Поочередно добавляет в БД посты из любого итерабельного контейнера.
     * Информация об авторах добавляемых постов уже должна быть в базе.
     *
     * @return кол-во добавленных записей
     */
    public int insertPosts(Iterable<Post> posts) throws SQLException {
        int rowsAffected = 0;
        String insertPostString =
                "INSERT INTO Post (url, user_id, date, title, text, comments) " +
                        "VALUES (?, (SELECT id FROM UserLJ WHERE nick = ?), ?, ?, ?, ?);";
        String insertTagToPostString = "INSERT INTO TagToPost (tag_id, post_id) VALUES (" +
                "(SELECT id FROM Tag WHERE text = ?), " +
                "(SELECT id FROM Post WHERE user_id = (SELECT id FROM UserLJ WHERE nick = ?) AND url = ?));";
        try (
                Connection con = getConnection();
                PreparedStatement insertPost = con.prepareStatement(insertPostString);
                PreparedStatement insertTagToPost = con.prepareStatement(insertTagToPostString)
        ) {
            for (Post post : posts) {
                insertPost.setInt(1, post.getUrl());        //never null
                insertPost.setString(2, post.getAuthor());
                insertPost.setTimestamp(3, post.getDate());
                insertPost.setString(4, post.getTitle());
                insertPost.setString(5, post.getText());
                if (post.getCountComment() == null)
                    insertPost.setNull(6, Types.INTEGER);
                else insertPost.setInt(6, post.getCountComment());

                rowsAffected += tryUpdateTransaction(insertPost, post.getAuthor() + ">" + post.getUrl(), "Post");

                assert (post.getTags() != null);
                for (String tag : post.getTags()) {
                    insertTagToPost.setString(1, tag);
                    insertTagToPost.setString(2, post.getAuthor());
                    insertTagToPost.setInt(3, post.getUrl());      //never null
                    rowsAffected += tryUpdateTransaction(insertTagToPost, post.getAuthor() + ">" + post.getUrl() + "<->" + tag, "TagTOPost");
                }

            }
        }
        return rowsAffected;
    }


    public List<Post> getPostsToNormalize(int limit) throws SQLException {
        if (limit <= 0) throw new IllegalArgumentException
                ("Argument limit must be greater than 0.");
        List<Post> result = new LinkedList<>();
        String selectPostsString = "SELECT id, title, text " +
                "FROM Post WHERE NOT normalized LIMIT ?";
        try (
                Connection con = getConnection();
                PreparedStatement selectPosts = con.prepareStatement(selectPostsString)
        ) {
            selectPosts.setInt(1, limit);
            ResultSet rs = tryQueryTransaction(selectPosts, "Post");
            if (rs != null)
                while (rs.next())
                    result.add(new Post(
                            rs.getLong("id"),
                            rs.getString("title"),
                            rs.getString("text")));
        }
        return result;
    }


    public int updatePostNormalized(long postId) throws SQLException {
        int rowsAffected = 0;
        String updateNormalizedString =
                "UPDATE Post SET normalized = TRUE WHERE id = ?;";

        try (
                Connection con = getConnection();
                PreparedStatement updateNormalized = con.prepareStatement(updateNormalizedString)
        ) {
            updateNormalized.setLong(1, postId);
            rowsAffected += tryUpdateTransaction(updateNormalized, "Post_id = " + postId, "Post");
        }
        return rowsAffected;
    }


    public int getPostCount() throws SQLException {
        String selectPostCountString = "SELECT count(*) FROM Post;";
        try (
                Connection con = getConnection();
                PreparedStatement selectPostCount = con.prepareStatement(selectPostCountString)
        ) {
            ResultSet rs = tryQueryTransaction(selectPostCount, "Post");
            if (rs == null || !rs.next())
                throw new IllegalStateException("If you see this, our code needs a fix");
            return rs.getInt("count");
        }
    }


    public int getPostNormalizedCount() throws SQLException {
        String selectPostNormalizedCountString = "SELECT count(*) FROM Post WHERE normalized;";
        try (
                Connection con = getConnection();
                PreparedStatement selectPostNormalizedCount = con.prepareStatement(selectPostNormalizedCountString)
        ) {
            ResultSet rs = tryQueryTransaction(selectPostNormalizedCount, "Post");
            if (rs == null || !rs.next())
                throw new IllegalStateException("If you see this, our code needs a fix");
            return rs.getInt("count");
        }
    }

    // TODO нужна реализация от postUrl+username ?
    // TODO Для неск постов, скажем, всех постов пользователя?
    public int getPostLength(long postId) throws SQLException {
        String selectLengthString = "SELECT length FROM PostLength WHERE post_id = ?;";
        try (
                Connection con = getConnection();
                PreparedStatement selectLength = con.prepareStatement(selectLengthString)
        ) {
            selectLength.setLong(1, postId);
            ResultSet rs = tryQueryTransaction(selectLength, "PostLength");
            if (rs == null || !rs.next()) return -1; //TODO нормально возвращать -1, если пост еще не обработан?
            else return rs.getInt("length");
        }
    }


    public int getPostUniqueWordCount(long postId) throws SQLException {
        String selectUniqueWordCountString = "SELECT count FROM PostUniqueWordCount WHERE post_id = ?;";
        try (
                Connection con = getConnection();
                PreparedStatement selectUniqueWordCount = con.prepareStatement(selectUniqueWordCountString)
        ) {
            selectUniqueWordCount.setLong(1, postId);
            ResultSet rs = tryQueryTransaction(selectUniqueWordCount, "PostUniqueWordCount");
            if (rs == null || !rs.next()) return -1;
            else return rs.getInt("count");
        }
    }


    /**
     * Поочередно добавляет в БД n-граммы из любого итерабельного контейнера.
     * Информация о посте добавляемых n-грамм уже должна быть в базе
     *
     * @param ngrams    - итерабельный контейнер с n-граммами в строках.
     * @param postId    - внутренний id поста в БД, выдается в объекте Post,
     *                  в методе getUnprocessedPosts(int num).
     * @param nGramType - тип добавляемых n-грамм. [1..3]
     *                  1 - unigram, 2 - digram, 3 - trigram.
     * @return кол-во добавленных записей
     */
    public int insertNGrams(Iterable<NGram> ngrams, long postId, int nGramType) throws SQLException {
        if (nGramType < 1 || nGramType > 3)
            throw new IllegalArgumentException("Argument nGramType must be in range [1..3].");
        String nGramTableName = N_GRAM_TABLE_NAMES[nGramType];
        int rowsAffected = 0;

        //TODO Вроде бы SQL Injections здесь не пройдет, но надо подумать
        String insertNGramString = "INSERT INTO " + nGramTableName + " (text) VALUES (?);";
        String insertNGramToPostString = "INSERT INTO " + nGramTableName + "ToPost " +
                "(ngram_id, post_id, uses_str, uses_cnt) VALUES ( " +
                "(SELECT id FROM " + nGramTableName + " WHERE text = ?), ?, ?, ?);";

        try (
                Connection con = getConnection();
                PreparedStatement insertNGram = con.prepareStatement(insertNGramString);
                PreparedStatement insertNGramToPost = con.prepareStatement(insertNGramToPostString)
        ) {
            for (NGram ngram : ngrams) {
                insertNGram.setString(1, ngram.getText());
                rowsAffected += tryUpdateTransaction(insertNGram, ngram.getText(), nGramTableName);
            }

            for (NGram ngram : ngrams) {
                insertNGramToPost.setString(1, ngram.getText());
                insertNGramToPost.setLong(2, postId);
                insertNGramToPost.setString(3, ngram.getUsesStr());
                insertNGramToPost.setInt(4, ngram.getUsesCnt());
                rowsAffected += tryUpdateTransaction(insertNGramToPost, "postId<->" + ngram.getText(), nGramTableName + "ToPost");
            }
        }
        return rowsAffected;
    }


    public List<String> getAllNGramNames() throws SQLException {
        List<String> result = new LinkedList<>();
        String selectNGramString = "SELECT text FROM AllNGramTexts;";
        try (
                Connection con = getConnection();
                PreparedStatement selectNGram = con.prepareStatement(selectNGramString);
        ) {
            ResultSet rs = tryQueryTransaction(selectNGram, "AllNGramTexts");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getString("text"));
        }
        return result;
    }


    public List<String> getAllNGramNames(long postId) throws SQLException {
        List<String> result = new LinkedList<>();
        String selectNGramString = "SELECT text FROM AllNGramTextPost " +
                "WHERE post_id = ?;";
        try (
                Connection con = getConnection();
                PreparedStatement selectNGram = con.prepareStatement(selectNGramString);
        ) {
            selectNGram.setLong(1, postId);
            ResultSet rs = tryQueryTransaction(selectNGram, "AllNGramTextPost");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getString("text"));
        }
        return result;
    }


    public int getNGramCount(long postId, int nGramType) throws SQLException {
        if (nGramType < 1 || nGramType > 3)
            throw new IllegalArgumentException("Argument nGramType must be in range [1..3].");
        String nGramTableName = N_GRAM_TABLE_NAMES[nGramType];
        int result;
        String selectNGramCountString = "SELECT count(*) FROM " + nGramTableName + "ToPost np " +
                "WHERE np.post_id = ?;";
        try (
                Connection con = getConnection();
                PreparedStatement selectNGramCount = con.prepareStatement(selectNGramCountString);
        ) {
            selectNGramCount.setLong(1, postId);
            ResultSet rs = tryQueryTransaction(selectNGramCount, nGramTableName + "ToPost");
            if (rs == null || !rs.next())
                throw new IllegalStateException("If you see this, our code needs a fix");
            return rs.getInt("count");
        }
    }


    /**
     * Пытается выполнить запрос к БД, обрабатывая некоторые исключения.
     * При возникновении некоторых исключений, повторяет запрос до MAX_TRIES-1 раз.
     *
     * @return объект класса <code>ResultSet</code>, содержащий результат запроса;
     * надо проверять на <code>null</code>      //TODO хочется никогда не возвращать null
     */
    private ResultSet tryQueryTransaction(PreparedStatement statement, String tableName) throws SQLException {   //SQLException пробрасываем
        boolean retryTransaction = true;
        int tries = MAX_TRIES;
        ResultSet resultSet = null;
        while (retryTransaction && tries > 0)
            try {
                retryTransaction = false;
                --tries;
                resultSet = statement.executeQuery();
            } catch (PSQLException pse) {
                final String ss = pse.getSQLState();
                if ("23505".equals(ss)) { // unique_violation
                    System.err.println("PSQLException: unique_violation on query <" +
                            statement + ">. Continue."); //TODO statement печатается норм
                    break;
                } else if ("40001".equals(ss) || "40P01".equals(ss)) { // serialization_failure, deadlock_detected
                    retryTransaction = true; //TODO переписать как отдельные методы
                    System.err.println("PSQLException: serialization_failure or deadlock_detected. " +
                            "Retrying transaction.");
                } else if (ss.startsWith("08") || ss.startsWith("53")) { //connection_exception, insufficient_resources
                    System.err.println("PSQLException: suspected bad connection. Closing query transaction to " +
                            "table " + tableName + ". \n If you see this exception than we need a code fix");
                    //TODO как-то по другому обрабатывать?
                    throw pse;
                } else if ("42P01".equals(ss)) { // undefined_table
                    System.err.println("PSQLException: undefined_table. Seems like you need to initialize DB, " +
                            "try DBConnector method dropInitDatabase().");
                    throw pse;
                } else {
                    System.err.println(ss);
                    throw pse;
                }
            }
        return resultSet;
    }


    private int tryUpdateTransaction(PreparedStatement toExecute, String currEntry, String tableName) throws SQLException {   //SQLException пробрасываем
        boolean retryTransaction = true;
        int tries = MAX_TRIES;
        int affected = 0;
        while (retryTransaction && tries > 0)
            try {
                retryTransaction = false;
                --tries;
                affected += toExecute.executeUpdate();
            } catch (PSQLException pse) {
                final String ss = pse.getSQLState();
                if ("23505".equals(ss)) { // unique_violation
                    System.err.println("PSQLException: unique_violation. We already have entry \"" +
                            currEntry + "\" in table \"" + tableName + "\". Continue."); //TODO нормальный лог
                    break;
                } else if ("40001".equals(ss) || "40P01".equals(ss)) { // serialization_failure, deadlock_detected
                    retryTransaction = true;
                    System.err.println("PSQLException: serialization_failure or deadlock_detected. " +
                            "Retrying transaction.");
                } else if (ss.startsWith("08") || ss.startsWith("53")) { //connection_exception, insufficient_resources
                    System.err.println("PSQLException: suspected bad connection. Closing transaction " +
                            "with first unadded entry: " + currEntry + ". \n If you see this exception than we need a code fix");
                    //TODO как-то по другому обрабатывать?
                    throw pse;
                } else if ("42P01".equals(ss)) { // undefined_table
                    System.err.println("PSQLException: undefined_table. Seems like you need to initialize DB, " +
                            "try DBConnector method dropInitDatabase().");
                    throw pse;
                } else {
                    System.err.println(ss);
                    throw pse;
                }
            }
        return affected;
    }


    private int tryUpdateTransaction(Connection con, Iterable<PreparedStatement> toExecute, String currEntry, String tableName) throws SQLException {   //SQLException пробрасываем
        boolean retryTransaction = true;
        int tries = MAX_TRIES;
        int affected = 0;
        while (retryTransaction && tries > 0)
            try {
                retryTransaction = false;
                --tries;
                con.setAutoCommit(false);
                for (PreparedStatement st : toExecute)
                    affected += st.executeUpdate();
                con.commit();
            } catch (PSQLException pse) {
                final String ss = pse.getSQLState();
                if ("23505".equals(ss)) { // unique_violation
                    System.err.println("PSQLException: unique_violation. We already have entry \"" +
                            currEntry + "\" in table \"" + tableName + "\". Continue.");
                    break;
                } else if ("40001".equals(ss) || "40P01".equals(ss)) { // serialization_failure, deadlock_detected
                    retryTransaction = true;
                    System.err.println("PSQLException: serialization_failure or deadlock_detected. " +
                            "Retrying transaction.");
                } else if (ss.startsWith("08") || ss.startsWith("53")) { //connection_exception, insufficient_resources
                    System.err.println("PSQLException: suspected bad connection. Closing transaction " +
                            "with first unadded entry: " + currEntry + ". \n If you see this exception than we need a code fix");
                    //TODO как-то по другому обрабатывать?
                    throw pse;
                } else if ("42P01".equals(ss)) { // undefined_table
                    System.err.println("PSQLException: undefined_table. Seems like you need to initialize DB, " +
                            "try DBConnector method dropInitDatabase().");
                    throw pse;
                } else {
                    System.err.println(ss);
                    throw pse;
                }
            } finally {
                con.setAutoCommit(true);
            }
        return affected;
    }
}
