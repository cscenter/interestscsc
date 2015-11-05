package db;

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
import java.util.Scanner;


/**
 * User: allight
 * Date: 06.10.2015 14:47
 */

public class DBConnector {
    private static final String DROPDATA_PASS = "Bzw7HPtmHmVVqKvSHe7d";
    private static final String SCHEMA_PATH = "db/schema.sql";
    private static final String SCHEMA_ENCODING = "UTF-8";
    private static final String HOST = "185.72.144.129";
    private static final int PORT = 5432; // стандартный порт в постгрес
    private static final String DB = "veiloneru";
    private static final String USER = "veiloneru";
    private static final String PASS = "wasddsaw";
    private static final int MAX_CONNECTIONS = 100; // 100 - внутреннее ограничение постгрес
    private static final int MAX_TRIES = 5; // TODO число попыток выполнения при временной (*) неудаче (>1)
    private static final String[] NGRAM_TABLE_NAMES = new String[]{"", "unigram", "digram", "trigram"};
    private PGPoolingDataSource connectionPool;
    private int crawlerId;


    public DBConnector(String crawlerName) throws SQLException {
        connectionPool = new PGPoolingDataSource();
        connectionPool.setServerName(HOST);
        connectionPool.setPortNumber(PORT);
        connectionPool.setDatabaseName(DB);
        connectionPool.setUser(USER);
        connectionPool.setPassword(PASS);
        connectionPool.setMaxConnections(MAX_CONNECTIONS);
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


    public LinkedList<String> getRegions() throws SQLException {
        LinkedList<String> result = new LinkedList<>();
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


    public LinkedList<String> getReservedRawUsers() throws SQLException {
        LinkedList<String> result = new LinkedList<>();
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


    public LinkedList<String> getUnfinishedRawUsers() throws SQLException {
        LinkedList<String> result = new LinkedList<>();
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
                "INSERT INTO UserLJ (nick, region_id, created, update, birthday, interests) VALUES " +
                        "(?, (SELECT id FROM Region WHERE name = COALESCE(?,'')), ?, ?, ?, ?);";
        String updateRawUserString =
                "UPDATE RawUserLJ SET user_id = (SELECT id FROM UserLJ WHERE nick = ?) WHERE nick = ?;";

        try (
                Connection con = getConnection();
                PreparedStatement insertUser = con.prepareStatement(insertUserString);
                PreparedStatement updateRawUser = con.prepareStatement(updateRawUserString)
        ) {
            insertUser.setString(1, userLJ.getNick());
            insertUser.setString(2, userLJ.getRegion());
            insertUser.setTimestamp(3, userLJ.getDateCreated());
            insertUser.setTimestamp(4, userLJ.getDateUpdated());
            insertUser.setDate(5, userLJ.getBirthday());
            insertUser.setString(6, userLJ.getInterests());

            rowsAffected += tryUpdateTransaction(insertUser, userLJ.getNick(), "UserLJ");

            updateRawUser.setString(1, userLJ.getNick());
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


    public LinkedList<User> getUsers() throws SQLException { //TODO сейчас не тащит fetched, т.к. его нет в User
        LinkedList<User> result = new LinkedList<>();
        String selectUsersString = "SELECT u.nick, r.name region, " +
                "u.created, u.update, u.birthday, u.interests " +
                "FROM UserLJ u JOIN Region r ON u.region_id = r.id;";
        try (
                Connection con = getConnection();
                PreparedStatement selectUsers = con.prepareStatement(selectUsersString);
        ) {
            ResultSet rs = tryQueryTransaction(selectUsers, "UserLJ");
            if (rs != null)
                while (rs.next())
                    result.add(new User(
                            rs.getString("nick"), rs.getString("region"),
                            rs.getTimestamp("created"), rs.getTimestamp("update"),
                            rs.getDate("birthday"), rs.getString("interests")
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


    /**
     * Поочередно добавляет в БД n-граммы из любого итерабельного контейнера.
     * Информация о посте добавляемых n-грамм уже должна быть в базе,
     * в таблице UserLJ поле nick.
     *
     * @param ngrams      - итерабельный контейнер с n-граммами в строках.
     * @param userLJNick - автор поста
     * @param postUrl    - url поста
     * @param nGramType   - тип добавляемых n-грамм. [1..3]
     *                    1 - unigram, 2 - digram, 3 - trigram.
     * @return кол-во добавленных записей
     */
    public int insertNgrams(Iterable<String> ngrams, String userLJNick, int postUrl, int nGramType) throws SQLException {
        if (nGramType < 1 || nGramType > 3)
            throw new IllegalArgumentException("Argument nGramType must be in range [1..3].");
        String ngramTableName = NGRAM_TABLE_NAMES[nGramType];
        int rowsAffected = 0;
        //TODO Вроде бы SQL Injections здесь не пройдет, но надо подумать
        String insertNgramString = "INSERT INTO " + ngramTableName + " (text) VALUES (?);";
        String insertNgramToPostString = "INSERT INTO " + ngramTableName + "ToPost VALUES (" +//TODO (..gram_id,post_id)
                "(SELECT id FROM " + ngramTableName + " WHERE text = ?), " +
                "(SELECT id FROM Post WHERE user_id = (SELECT id FROM UserLJ WHERE nick = ?) AND url = ?));";
        try (
                Connection con = getConnection();
                PreparedStatement insertNgram = con.prepareStatement(insertNgramString);
                PreparedStatement insertNgramToPost = con.prepareStatement(insertNgramToPostString)
        ) {
            for (String ngram : ngrams) {
                insertNgram.setString(1, ngram);
                rowsAffected += tryUpdateTransaction(insertNgram, ngram, ngramTableName);

                insertNgramToPost.setString(1, ngram);
                insertNgramToPost.setString(2, userLJNick);
                insertNgramToPost.setInt(3, postUrl);        //never null
                rowsAffected += tryUpdateTransaction(insertNgramToPost, userLJNick + ">" + postUrl + "<->" + ngram, ngramTableName + "ToPost");
            }
        }
        return rowsAffected;
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