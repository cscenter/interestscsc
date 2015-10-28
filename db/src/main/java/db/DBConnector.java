package db;

import crawler.Post;
import crawler.Tag;
import crawler.User;
import org.postgresql.ds.PGPoolingDataSource;
import org.postgresql.util.PSQLException;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;


/**
 * User: allight
 * Date: 06.10.2015 14:47
 */

public class DBConnector {
    private static final String SCHEMA_PATH = "db/schema.sql";
    private static final String SCHEMA_ENCODING = "UTF-8";
    private static final String HOST = "185.72.144.129";
    private static final int PORT = 5432; // стандартный порт в постгрес
    private static final String DB = "veiloneru";
    private static final String USER = "veiloneru";
    private static final String PASS = "wasddsaw";
    private static final int MAX_CONNECTIONS = 100; // 100 - внутреннее ограничение постгрес
    private static final int MAX_TRIES = 5; // TODO число попыток выполнения при временной (*) неудаче (>1)
    private PGPoolingDataSource connectionPool;

    public DBConnector() {
        connectionPool = new PGPoolingDataSource();
        connectionPool.setServerName(HOST);
        connectionPool.setPortNumber(PORT);
        connectionPool.setDatabaseName(DB);
        connectionPool.setUser(USER);
        connectionPool.setPassword(PASS);
        connectionPool.setMaxConnections(MAX_CONNECTIONS);
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
    public DBConnector dropInitDatabase() throws FileNotFoundException, SQLException {

        String schemaSQL = new Scanner(new File(SCHEMA_PATH), SCHEMA_ENCODING)
                .useDelimiter("\\Z").next();
        try (Connection conn = getConnection()) {
            conn.createStatement().execute(schemaSQL);
        }
        return this;
    }

    private int tryUpdateTransaction(PreparedStatement toExecute, String currEntry, String tablename) throws SQLException {   //SQLException пробрасываем
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
                if (ss.equals("23505")) { // unique_violation
                    System.err.println("PSQLException: unique_violation. We already have entry \"" +
                            currEntry + "\" in table \"" + tablename + "\". Continue."); //TODO нормальный лог
                    break;
                } else if (ss.equals("40001") || ss.equals("40P01")) { // serialization_failure, deadlock_detected
                    retryTransaction = true;
                    System.err.println("PSQLException: serialization_failure or deadlock_detected. " +
                            "Retrying transaction.");
                } else if (ss.startsWith("08") || ss.startsWith("53")) { //connection_exception, insufficient_resources
                    System.err.println("PSQLException: suspected bad connection. Closing transaction " +
                            "with first unadded entry: " + currEntry); //TODO как-то по другому обрабатывать?
                    pse.printStackTrace();
                } else {
                    System.err.println(ss);
                    throw pse;
                }
            }
        return affected;
    }

    private int tryUpdateTransaction(Connection con, Iterable<PreparedStatement> toExecute, String currEntry, String tablename) throws SQLException {   //SQLException пробрасываем
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
                if (ss.equals("23505")) { // unique_violation
                    System.err.println("PSQLException: unique_violation. We already have entry \"" +
                            currEntry + "\" in table \"" + tablename + "\". Continue.");
                    break;
                } else if (ss.equals("40001") || ss.equals("40P01")) { // serialization_failure, deadlock_detected
                    retryTransaction = true;
                    System.err.println("PSQLException: serialization_failure or deadlock_detected. " +
                            "Retrying transaction.");
                } else if (ss.startsWith("08") || ss.startsWith("53")) { //connection_exception, insufficient_resources
                    System.err.println("PSQLException: suspected bad connection. Closing transaction " +
                            "with first unadded entry: " + currEntry); //TODO как-то по другому обрабатывать?
                    pse.printStackTrace();
                } else {
                    System.err.println(ss);
                    throw pse;
                }
            } finally {
                con.setAutoCommit(true);
            }
        return affected;
    }


    /**
     * Поочередно добавляет в БД регионы из любого итерабельного контейнера.
     *
     * @return кол-во добавленных записей
     */
    public int insertRegions(Iterable<String> regions) throws SQLException {
        int rowsAffected = 0;
        String insertRegionString =
                "INSERT INTO Region VALUES (DEFAULT, ?);";
        try (
                Connection con = getConnection();
                PreparedStatement insertRegion = con.prepareStatement(insertRegionString)
        ) {
            for (String region : regions) {
                insertRegion.setString(1, region);
                rowsAffected += tryUpdateTransaction(insertRegion, region, "Region");
            }
        }
        return rowsAffected;
    }


    /**
     * Поочередно добавляет в БД пользователей из любого итерабельного контейнера.
     * Информация о регионах добавляемых пользователей уже должна быть в базе.
     *
     * @return кол-во добавленных записей
     */
    public int insertUserNicks(Iterable<String> users) throws SQLException {
        int rowsAffected = 0;
        String insertUserString =
                "INSERT INTO RawUserLJ VALUES (?, NULL);";
        try (
                Connection con = getConnection();
                PreparedStatement insertUser = con.prepareStatement(insertUserString)
        ) {
            for (String user : users) {
                insertUser.setString(1, user);
                rowsAffected += tryUpdateTransaction(insertUser, user, "RawUserLJ");
            }
        }
        return rowsAffected;
    }

    /**
     * Поочередно добавляет в БД пользователей из любого итерабельного контейнера.
     * Информация о регионах добавляемых пользователей уже должна быть в базе, их
     * имена должны присутствовать в RawUserLJ.
     *
     * @return кол-во добавленных записей
     */
    public int insertUsers(Iterable<User> users) throws SQLException {
        int rowsAffected = 0;       //TODO Несуществующий референс пока прокидываем
        String insertUserString =   //TODO в какой момент проверять\добавлять регион & etc?
                "INSERT INTO UserLJ VALUES (DEFAULT, ?, (SELECT id FROM Region WHERE name = ?), ?, ?, NULL, ?, ?);";
        String insertUserWithoutRegionString =
                "INSERT INTO UserLJ VALUES (DEFAULT, ?, NULL, ?, ?, NULL, ?, ?);";
        String updateRawUserString =
                "UPDATE RawUserLJ SET user_id = (SELECT id FROM UserLJ WHERE nick = ?) WHERE nick = ?;";

        try (
                Connection con = getConnection();
                PreparedStatement insertUser = con.prepareStatement(insertUserString);
                PreparedStatement insertUserWithoutRegion = con.prepareStatement(insertUserWithoutRegionString);
                PreparedStatement updateRawUser = con.prepareStatement(updateRawUserString)
        ) {
            for (User user : users) {
                if (user.getRegion() != null) {
                    insertUser.setString(1, user.getNick());
                    insertUser.setString(2, user.getRegion());
                    insertUser.setTimestamp(3, user.getDateCreated());
                    insertUser.setTimestamp(4, user.getDateUpdated());
                    insertUser.setDate(5, user.getBirthday());
                    insertUser.setString(6, user.getInterests());

                    rowsAffected += tryUpdateTransaction(insertUser, user.getNick(), "UserLJ");
                } else {
                    insertUserWithoutRegion.setString(1, user.getNick());
                    insertUserWithoutRegion.setTimestamp(2, user.getDateCreated());
                    insertUserWithoutRegion.setTimestamp(3, user.getDateUpdated());
                    insertUserWithoutRegion.setDate(4, user.getBirthday());
                    insertUserWithoutRegion.setString(5, user.getInterests());

                    rowsAffected += tryUpdateTransaction(insertUserWithoutRegion, user.getNick(), "UserLJ");
                }

                updateRawUser.setString(1, user.getNick());
                updateRawUser.setString(2, user.getNick());

                rowsAffected += tryUpdateTransaction(updateRawUser, user.getNick(), "RawUserLJ");
            }
        }
        return rowsAffected;
    }


    /**
     * Поочередно добавляет в БД тэги из любого итерабельного контейнера.
     * Информация о пользователе добавляемых тэгов уже должна быть в базе,
     * в таблице UserLJ поле nick.
     *
     * @return кол-во добавленных записей
     */
    public int insertTags(Iterable<Tag> tags, String userLJ_nick) throws SQLException {
        int rowsAffected = 0;
        String insertTagString = "INSERT INTO Tag (id, text) VALUES (DEFAULT, ?);";
        String insertTagToUserLJString = "INSERT INTO TagToUserLJ VALUES (" +
                "(SELECT id FROM Tag WHERE text = ?), " +
                "(SELECT id FROM UserLJ WHERE nick = ?), " +
                "?);";
        try (
                Connection con = getConnection();
                PreparedStatement insertTag = con.prepareStatement(insertTagString);
                PreparedStatement insertTagToUserLJ = con.prepareStatement(insertTagToUserLJString)
        ) {
            for (Tag tag : tags) {
                insertTag.setString(1, tag.getName());
                rowsAffected += tryUpdateTransaction(insertTag, tag.getName(), "Tag");

                insertTagToUserLJ.setString(1, tag.getName());
                insertTagToUserLJ.setString(2, userLJ_nick);
                insertTagToUserLJ.setInt(3, tag.getUses());
                rowsAffected += tryUpdateTransaction(insertTagToUserLJ, tag.getName() + "<->" + userLJ_nick, "TagToUserLJ");
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
                "INSERT INTO Post VALUES (DEFAULT, ?, (SELECT id FROM UserLJ WHERE nick = ?), ?, ?, ?, NULL, ?);";
        String insertTagToPostString = "INSERT INTO TagToPost VALUES (" +
                "(SELECT id FROM Tag WHERE text = ?), " +
                "(SELECT id FROM Post WHERE user_id = (SELECT id FROM UserLJ WHERE nick = ?) AND url = ?));";
        try (
                Connection con = getConnection();
                PreparedStatement insertPost = con.prepareStatement(insertPostString);
                PreparedStatement insertTagToPost = con.prepareStatement(insertTagToPostString)
        ) {
            for (Post post : posts) {
                insertPost.setInt(1, post.getUrl());
                insertPost.setString(2, post.getAuthor());
                insertPost.setTimestamp(3, post.getDate());
                insertPost.setString(4, post.getTitle());
                insertPost.setString(5, post.getText());
                insertPost.setInt(6, post.getCountComment());

                rowsAffected += tryUpdateTransaction(insertPost, post.getAuthor() + ">" + post.getUrl(), "Post");

                for (String tag : post.getTags()) {
                    insertTagToPost.setString(1, tag);
                    insertTagToPost.setString(2, post.getAuthor());
                    insertTagToPost.setInt(3, post.getUrl());
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
     * @param userLJ_nick - автор поста
     * @param post_url    - url поста
     * @param ngramType   - тип добавляемых n-грамм. [1..3]
     *                    1 - unigram, 2 - digram, 3 - trigram.
     * @return кол-во добавленных записей
     */
    public int insertNgrams(Iterable<String> ngrams, String userLJ_nick, int post_url, int ngramType) throws SQLException {
        String ngramTableName;
        switch (ngramType) {
            case 1:
                ngramTableName = "unigram";
                break;
            case 2:
                ngramTableName = "digram";
                break;
            case 3:
                ngramTableName = "trigram";
                break;
            default:
                throw new IllegalArgumentException("Parameter ngramTableName should be one of these literals: " +
                        "'unigram', 'digram' or 'trigram'.");
        }
        int rowsAffected = 0;
        String insertNgramString = "INSERT INTO " + ngramTableName + " VALUES (DEFAULT, ?);";
        String insertNgramToPostString = "INSERT INTO " + ngramTableName + "ToPost VALUES (" +
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
                insertNgramToPost.setString(2, userLJ_nick);
                insertNgramToPost.setInt(3, post_url);
                rowsAffected += tryUpdateTransaction(insertNgramToPost, userLJ_nick + ">" + post_url + "<->" + ngram, ngramTableName + "ToPost");
            }
        }
        return rowsAffected;
    }
}
