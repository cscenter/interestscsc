package db;

import data.NGram;
import data.User;
import org.postgresql.ds.PGPoolingDataSource;
import org.postgresql.util.PSQLException;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * User: allight
 * Date: 06.10.2015 14:47
 */

@SuppressWarnings("Duplicates")
public class DBConnector {
    protected static final String SCHEMA_PATH = "db/schema.sql";
    protected static final String SCHEMA_ENCODING = "UTF-8";
    protected static final String DROPDATA_PASS = "Bzw7HPtmHmVVqKvSHe7d";

    protected final DataBase dataBase;

    public enum NGramType {
        UNIGRAM("unigram", "unigramToPost"),
        DIGRAM("digram", "digramToPost"),
        TRIGRAM("trigram", "trigramToPost");

        private final String tableName;
        private final String tableToPostName;

        NGramType(String tableName, String tableToPostName) {
            this.tableName = tableName;
            this.tableToPostName = tableToPostName;
        }

        public String getTableName() {
            return tableName;
        }

        public String getTableToPostName() {
            return tableToPostName;
        }
    }

    public enum DataBase {
        MAIN("185.72.144.129", 5432, "veiloneru", "veiloneru", "wasddsaw", 20, 5),
        TEST("185.72.144.129", 5432, "veiloneru_test", "veiloneru_test", "wasddsaw", 20, 5),
        LOCAL("localhost", 5432, "interests", "interests", "12345", 20, 5);

        private final String host;
        private final int port;                 // 5432 - стандартный порт постгрес
        private final String db;
        private final String user;
        private final String pass;
        private final int maxConnections;       // 100 - внутреннее ограничение постгрес
        private final int maxTries;             // число попыток выполнения при временной (*) неудаче (>1)

        private PGPoolingDataSource connectionPool;

        DataBase(String host, int port, String db, String user, String pass, int maxConnections, int maxTries) {
            this.host = host;
            this.port = port;
            this.db = db;
            this.user = user;
            this.pass = pass;
            this.maxConnections = maxConnections;
            this.maxTries = maxTries;
            this.connectionPool = preparePool();
        }

        private PGPoolingDataSource preparePool() {
            PGPoolingDataSource pool = new PGPoolingDataSource();
            pool.setServerName(host);
            pool.setPortNumber(port);
            pool.setDatabaseName(db);
            pool.setUser(user);
            pool.setPassword(pass);
            pool.setMaxConnections(maxConnections);
            return pool;
        }

        public int getMaxTries() {
            return maxTries;
        }
    }

    public DBConnector(DataBase dataBase) {
        this.dataBase = dataBase;
    }

    /**
     * Пересоздает указанную базу из схемы, указанной в SCHEMA_PATH
     * с кодировкой SCHEMA_ENCODING
     */
    public static void dropInitDatabase(DataBase dataBase, String pass) throws FileNotFoundException, SQLException {
        if (!DROPDATA_PASS.equalsIgnoreCase(pass))
            throw new IllegalArgumentException("Maybe you shouldn't drop db?");
        String schemaSQL = new Scanner(new File(SCHEMA_PATH), SCHEMA_ENCODING)
                .useDelimiter("\\Z").next();
        try (Connection conn = dataBase.connectionPool.getConnection()) {
            conn.createStatement().execute(schemaSQL);
        }
    }

    protected boolean checkTable(String tableName) throws SQLException {
        //noinspection SqlResolve
        String selectTableString = "SELECT * FROM pg_catalog.pg_tables WHERE tablename = ?;";
        try (
                Connection con = getConnection();
                PreparedStatement selectTable = con.prepareStatement(selectTableString)
        ) {
            int i = 0;
            selectTable.setString(++i, tableName);
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
        return dataBase.connectionPool.getConnection();
    }

    /**
     * Возвращает соединение в очередь.
     *
     * @deprecated Лучше использовать соедиление в качестве ресурса в
     * try-with-resources (см., например, метод dropInitDatabase).
     */
    public void closeConnection(Connection connectionToClose) throws SQLException {
        //noinspection EmptyTryBlock,unused
        try (Connection autoClose = connectionToClose) {
        }
    }

    public List<String> getRegions() throws SQLException {
        List<String> result = new LinkedList<>();
        String selectRegionsString = "SELECT name FROM Region;";
        try (
                Connection con = getConnection();
                PreparedStatement selectRegions = con.prepareStatement(selectRegionsString)
        ) {
            ResultSet rs = tryQueryTransaction(selectRegions, "Region");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getString(1));
        }
        return result;
    }

    public Queue<String> getRawUsers() throws SQLException {
        Queue<String> result = new LinkedList<>();
        String selectRawUsersString = "SELECT nick FROM RawUserLJ;";
        try (
                Connection con = getConnection();
                PreparedStatement selectRawUsers = con.prepareStatement(selectRawUsersString)
        ) {
            ResultSet rs = tryQueryTransaction(selectRawUsers, "RawUserLJ");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getString(1));
        }
        return result;
    }

    /**
     * Возвращвет все заполненные профили пользователей.
     * Информация о школах не включается.
     */
    public List<User> getUsers() throws SQLException {
        List<User> result = new LinkedList<>();
        String selectUsersString = "SELECT u.id, u.nick, r.name region, " +
                "u.created, u.update, u.fetched, u.birthday, u.interests, " +
                "u.city_cstm, u.posts_num, u.cmmnt_in , u.cmmnt_out, u.bio " +
                "FROM UserLJ u JOIN Region r ON u.region_id = r.id;";
        try (
                Connection con = getConnection();
                PreparedStatement selectUsers = con.prepareStatement(selectUsersString)
        ) {
            ResultSet rs = tryQueryTransaction(selectUsers, "UserLJ");
            if (rs != null)
                while (rs.next()) {
                    int i = 0;
                    Long id = rs.getLong(++i);
                    result.add(new User.UserBuilder(rs.getString(++i))
                            .setId(id)
                            .setRegion(rs.getString(++i))
                            .setDateCreated(rs.getTimestamp(++i))
                            .setDateUpdated(rs.getTimestamp(++i))
                            .setDateFetched(rs.getTimestamp(++i))
                            .setBirthday(rs.getDate(++i))
                            .setInterests(rs.getString(++i))
                            .setCustomCity(rs.getString(++i))
                            .setPostsNum(rs.getInt(++i))
                            .setCommentsPosted(rs.getInt(++i))
                            .setCommentsReceived(rs.getInt(++i))
                            .setBiography(rs.getString(++i))
                            .setSchools(new LinkedList<>())
                            .build()
                    );
                }
        }
        return result;
    }

    public List<String> getAllTagNames() throws SQLException {
        List<String> result = new LinkedList<>();
        String selectTagString = "SELECT text FROM Tag;";
        try (
                Connection con = getConnection();
                PreparedStatement selectTag = con.prepareStatement(selectTagString)
        ) {
            ResultSet rs = tryQueryTransaction(selectTag, "Tag");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getString(1));
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
                PreparedStatement selectTag = con.prepareStatement(selectTagString)
        ) {
            int i = 0;
            selectTag.setString(++i, userLJNick);
            ResultSet rs = tryQueryTransaction(selectTag, "Tag");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getString(1));
        }
        return result;
    }

    public List<String> getAllTagNames(long post_id) throws SQLException {
        List<String> result = new LinkedList<>();
        String selectTagNamesString = "SELECT text FROM TagNameToPost tnp " +
                "WHERE tnp.post_id = ?;";
        try (
                Connection con = getConnection();
                PreparedStatement selectTagNames = con.prepareStatement(selectTagNamesString)
        ) {
            int i = 0;
            selectTagNames.setLong(++i, post_id);
            ResultSet rs = tryQueryTransaction(selectTagNames, "TagNameToPost");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getString(1));
        }
        return result;
    }

    public List<String> getAllTagNames(List<Long> postIDs) throws SQLException {
        if (postIDs == null)
            throw new IllegalArgumentException("Expected not null argument");
        List<String> result = new LinkedList<>();
        if (postIDs.isEmpty())
            return result;
        //noinspection SqlResolve,SqlCheckUsingColumns
        String selectTagNamesString =
                "WITH post_list AS ( " +
                        "    SELECT post_id FROM ( " +
                        "        VALUES (?) %s " +
                        "    ) AS post_list(post_id) " +
                        "), tag_list AS ( " +
                        "    SELECT tp.tag_id id " +
                        "    FROM TagToPost tp " +
                        "      JOIN post_list USING (post_id) " +
                        "    GROUP BY tp.tag_id " +
                        ")" +
                        "SELECT t.text " +
                        "FROM TAG t " +
                        "  JOIN tag_list USING(id);";
        String attr = ",(?)";
        StringBuilder attrs = new StringBuilder(postIDs.size() * attr.length());
        for (int i = postIDs.size() - 1; i > 0; i--)
            attrs.append(attr);
        selectTagNamesString = String.format(selectTagNamesString,attrs.toString());
        try (
                Connection con = getConnection();
                PreparedStatement selectTagNames = con.prepareStatement(selectTagNamesString)
        ) {
            int i = 0;
            for (long postID : postIDs)
                selectTagNames.setLong(++i, postID);
            ResultSet rs = tryQueryTransaction(selectTagNames, "TagNameToPost");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getString(1));
        }
        return result;
    }

    public List<String> getTopNormalizedTagNames(int minUses, int maxUses) throws SQLException {
        List<String> result = new LinkedList<>();
        String selectTopTagNamesString = "" +
                "WITH norm AS ( " +
                "    SELECT id AS post_id " +
                "    FROM Post " +
                "    WHERE normalized " +
                ") " +
                "SELECT text " +
                "FROM TagNameToPost " +
                "JOIN norm USING(post_id) " +
                "GROUP BY text " +
                "HAVING count(*) > ? AND count(*) < ?";
        try (
                Connection con = getConnection();
                PreparedStatement selectTopTagNames = con.prepareStatement(selectTopTagNamesString)
        ) {
            int i = 0;
            selectTopTagNames.setInt(++i, minUses);
            selectTopTagNames.setInt(++i, maxUses);
            ResultSet rs = tryQueryTransaction(selectTopTagNames, "TagNameToPost");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getString(1));
        }
        return result;
    }

    public Set<Long> getAllUserPostUrls(String userLJNick) throws SQLException {
        Set<Long> result = new HashSet<>();
        String selectUserPostUrlsString = "SELECT url FROM Post p " +
                "WHERE p.user_id = (SELECT id FROM UserLJ WHERE nick = ?);";
        try (
                Connection con = getConnection();
                PreparedStatement selectUserPostUrls = con.prepareStatement(selectUserPostUrlsString)
        ) {
            int i = 0;
            selectUserPostUrls.setString(++i, userLJNick);
            ResultSet rs = tryQueryTransaction(selectUserPostUrls, "UserLJ");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getLong(1));
        }
        return result;
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

    public List<Long> getAllPostNormalizedIds() throws SQLException {
        List<Long> result = new LinkedList<>();
        String selectPostNormalizedIdsString = "SELECT id FROM Post WHERE normalized;";
        try (
                Connection con = getConnection();
                PreparedStatement selectPostNormalizedIds = con.prepareStatement(selectPostNormalizedIdsString)
        ) {
            ResultSet rs = tryQueryTransaction(selectPostNormalizedIds, "Post");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getLong(1));
        }
        return result;
    }

    @Deprecated
    public List<Long> getAllPostNormalizedIds(String tag1, String tag2) throws SQLException {
        if (tag1 == null || tag2 == null)
            throw new IllegalArgumentException("Expected not null arguments");
        List<Long> result = new LinkedList<>();
        //noinspection SqlResolve
        String selectPostNormalizedIdsString =
                "WITH with_tags AS (" +
                        "    SELECT post_id " +
                        "    FROM TagNameToPost " +
                        "    WHERE text IN (VALUES (?), (?)) " +
                        ") " +
                        "SELECT id " +
                        "FROM Post p " +
                        "  JOIN with_tags wt ON wt.post_id = p.id " +
                        "  JOIN unigramtopost USING(post_id)" +            //TODO не особо эффективный JOIN
                        "WHERE p.normalized;";
        try (
                Connection con = getConnection();
                PreparedStatement selectPostNormalizedIds = con.prepareStatement(selectPostNormalizedIdsString)
        ) {
            int i = 0;
            selectPostNormalizedIds.setString(++i, tag1);
            selectPostNormalizedIds.setString(++i, tag2);
            ResultSet rs = tryQueryTransaction(selectPostNormalizedIds, "Post");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getLong(1));
        }
        return result;
    }

    public List<Long> getAllPostNormalizedIds(List<String> tags) throws SQLException {
        if (tags == null)
            throw new IllegalArgumentException("Expected not null argument");
        List<Long> result = new LinkedList<>();
        if (tags.isEmpty())
            return result;
        //noinspection SqlResolve
        String selectPostNormalizedIdsString =
                "WITH tag_text_list AS ( " +
                        "    SELECT text " +
                        "    FROM ( " +
                        "           VALUES (?) %s " +
                        "    ) AS tag_text_list(text) " +
                        "), post_list AS ( " +
                        "    SELECT tp.post_id id " +
                        "    FROM Tag t " +
                        "      JOIN tag_text_list ttl ON t.text = ttl.text " +
                        "      JOIN tagtopost tp ON t.id = tp.tag_id " +
                        "      JOIN unigramtopost USING(post_id) " +            //TODO не особо эффективный JOIN
                        "    GROUP BY tp.post_id " +
                        ") " +
                        "SELECT id " +
                        "FROM Post p " +
                        "  JOIN post_list USING(id) " +
                        "WHERE p.normalized;";
        String attr = ",(?)";
        StringBuilder attrs = new StringBuilder(tags.size() * attr.length());
        for (int i = tags.size() - 1; i > 0; i--)
            attrs.append(attr);
        selectPostNormalizedIdsString =
                String.format(selectPostNormalizedIdsString,attrs.toString());
        try (
                Connection con = getConnection();
                PreparedStatement selectPostNormalizedIds = con.prepareStatement(selectPostNormalizedIdsString)
        ) {
            int i = 0;
            for (String tag : tags)
                selectPostNormalizedIds.setString(++i, tag);
            ResultSet rs = tryQueryTransaction(selectPostNormalizedIds, "Post");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getLong(1));
        }
        return result;
    }

    public int getPostLength(long postId) throws SQLException {
        String selectLengthString = "SELECT length FROM PostLength WHERE post_id = ?;";
        try (
                Connection con = getConnection();
                PreparedStatement selectLength = con.prepareStatement(selectLengthString)
        ) {
            int i = 0;
            selectLength.setLong(++i, postId);
            ResultSet rs = tryQueryTransaction(selectLength, "PostLength");
            if (rs == null || !rs.next())
                throw new IllegalStateException("Requested post (id = " + postId + ") wasn't normalized yet");
            else
                return rs.getInt("length");
        }
    }

    public int getPostUniqueWordCount(long postId) throws SQLException {
        String selectUniqueWordCountString = "SELECT count FROM PostUniqueWordCount WHERE id = ?;";
        try (
                Connection con = getConnection();
                PreparedStatement selectUniqueWordCount = con.prepareStatement(selectUniqueWordCountString)
        ) {
            int i = 0;
            selectUniqueWordCount.setLong(++i, postId);
            ResultSet rs = tryQueryTransaction(selectUniqueWordCount, "PostUniqueWordCount");
            if (rs == null || !rs.next())
                throw new IllegalStateException("Requested post wasn't normalized yet");
            else
                return rs.getInt("count");
        }
    }

    public List<String> getAllNGramNames() throws SQLException {
        List<String> result = new LinkedList<>();
        String selectNGramString = "SELECT text FROM AllNGramTexts;";
        try (
                Connection con = getConnection();
                PreparedStatement selectNGram = con.prepareStatement(selectNGramString)
        ) {
            ResultSet rs = tryQueryTransaction(selectNGram, "AllNGramTexts");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getString(1));
        }
        return result;
    }

    public List<NGram> getAllNGramNames(long postId) throws SQLException {
        List<NGram> result = new LinkedList<>();
        String selectNGramString = "SELECT text, uses_cnt FROM AllNGramTextUsesPost " +
                "WHERE post_id = ?;";
        try (
                Connection con = getConnection();
                PreparedStatement selectNGram = con.prepareStatement(selectNGramString)
        ) {
            int i = 0;
            selectNGram.setLong(++i, postId);
            ResultSet rs = tryQueryTransaction(selectNGram, "AllNGramTextPost");
            if (rs != null)
                while (rs.next()) {
                    i = 0;
                    result.add(new NGram(rs.getString(++i), null, rs.getInt(++i)));
                }
        }
        return result;
    }

    public List<String> getAllNGramNames(List<Long> postIDs) throws SQLException {
        if (postIDs == null)
            throw new IllegalArgumentException("Expected not null argument");
        List<String> result = new LinkedList<>();
        if (postIDs.isEmpty())
            return result;
        //noinspection SqlResolve
        String selectNGramString =
                "WITH post_list AS ( " +
                        "    SELECT post_id  " +
                        "    FROM ( " +
                        "      VALUES (?) %s " +
                        "    ) AS post_list(post_id) " +
                        ") " +
                        "SELECT text  " +
                        "FROM AllNGramTextUsesPost " +
                        "  JOIN post_list USING(post_id) " +
                        "GROUP BY text;";
        String attr = ",(?)";
        StringBuilder attrs = new StringBuilder(postIDs.size() * attr.length());
        for (int i = postIDs.size() - 1; i > 0; i--)
            attrs.append(attr);
        selectNGramString = String.format(selectNGramString,attrs.toString());
        try (
                Connection con = getConnection();
                PreparedStatement selectNGram = con.prepareStatement(selectNGramString)
        ) {
            int i = 0;
            for (long postID : postIDs)
                selectNGram.setLong(++i, postID);
            ResultSet rs = tryQueryTransaction(selectNGram, "AllNGramTextPost");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getString(1));
        }
        return result;
    }

    public List<NGram> getAllNGramNames(long postId, NGramType nGramType) throws SQLException {
        List<NGram> result = new LinkedList<>();
        //noinspection SqlResolve
        String selectNGramString = "SELECT n.text, np.uses_cnt " +
                "FROM " + nGramType.getTableName() + " n " +
                "JOIN " + nGramType.getTableToPostName() + " np ON n.id = np.ngram_id " +
                "WHERE np.post_id = ?;";
        try (
                Connection con = getConnection();
                PreparedStatement selectNGram = con.prepareStatement(selectNGramString)
        ) {
            int i = 0;
            selectNGram.setLong(++i, postId);
            ResultSet rs = tryQueryTransaction(selectNGram, nGramType.getTableName());
            if (rs != null)
                while (rs.next()) {
                    i = 0;
                    result.add(new NGram(rs.getString(++i), null, rs.getInt(++i)));
                }
        }
        return result;
    }

    public List<String> getAllNGramNames(List<Long> postIDs, NGramType nGramType) throws SQLException {
        if (postIDs == null)
            throw new IllegalArgumentException("Expected not null argument");
        List<String> result = new LinkedList<>();
        if (postIDs.isEmpty())
            return result;
        //noinspection SqlResolve
        String selectNGramString =
                "WITH post_list AS ( " +
                        "    SELECT post_id " +
                        "    FROM ( " +
                        "      VALUES (?) %s " +
                        "    ) AS post_list(post_id) " +
                        "), ngram_list AS ( " +
                        "    SELECT ngram_id id " +
                        "    FROM " + nGramType.getTableToPostName() + " " +
                        "      JOIN post_list USING(post_id) " +
                        "    GROUP BY ngram_id " +
                        ") " +
                        "SELECT text " +
                        "FROM " + nGramType.getTableName() + " " +
                        "  JOIN ngram_list USING(id);";
        String attr = ",(?)";
        StringBuilder attrs = new StringBuilder(postIDs.size() * attr.length());
        for (int i = postIDs.size() - 1; i > 0; i--)
            attrs.append(attr);
        selectNGramString = String.format(selectNGramString,attrs.toString());
        try (
                Connection con = getConnection();
                PreparedStatement selectNGram = con.prepareStatement(selectNGramString)
        ) {
            int i = 0;
            for (long postID : postIDs)
                selectNGram.setLong(++i, postID);
            ResultSet rs = tryQueryTransaction(selectNGram, nGramType.getTableName());
            if (rs != null)
                while (rs.next())
                    result.add(rs.getString(1));
        }
        return result;
    }

    @SuppressWarnings("SqlResolve")
    public int getNGramCount(long postId, NGramType nGramType) throws SQLException {
        String selectNGramCountString = "SELECT count(*) FROM " + nGramType.getTableToPostName() + " np " +
                "WHERE np.post_id = ?;";
        try (
                Connection con = getConnection();
                PreparedStatement selectNGramCount = con.prepareStatement(selectNGramCountString)
        ) {
            int i = 0;
            selectNGramCount.setLong(++i, postId);
            ResultSet rs = tryQueryTransaction(selectNGramCount, nGramType.getTableToPostName());
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
    protected ResultSet tryQueryTransaction(PreparedStatement statement, String tableName) throws SQLException {
        boolean retryTransaction = true;
        int tries = dataBase.maxTries;
        ResultSet resultSet = null;
        while (retryTransaction && tries > 0)
            try {
                retryTransaction = false;
                --tries;
                resultSet = statement.executeQuery();
            } catch (PSQLException pse) {
                retryTransaction = processPSE(pse, tableName, null);
            }
        return resultSet;
    }

    protected int tryUpdateTransaction(PreparedStatement toExecute, String currEntry,
                                       String tableName) throws SQLException {
        boolean retryTransaction = true;
        int tries = dataBase.maxTries;
        int affected = 0;
        while (retryTransaction && tries > 0)
            try {
                retryTransaction = false;
                --tries;
                affected += toExecute.executeUpdate();
            } catch (PSQLException pse) {
                retryTransaction = processPSE(pse, tableName, currEntry);
            }
        return affected;
    }

    protected void tryBatchTransaction(Connection con, Iterable<PreparedStatement> toExecute,
                                       String tableName) throws SQLException {
        boolean retryTransaction = true;
        int tries = dataBase.maxTries;
        while (retryTransaction && tries > 0)
            try {
                retryTransaction = false;
                --tries;
                con.setAutoCommit(false);
                for (PreparedStatement st : toExecute) {
                    if (con != st.getConnection())
                        throw new IllegalArgumentException(
                                "One of the Statements doesn't related with current Connection");
                    st.execute();
                }
                con.commit();
            } catch (PSQLException pse) {
                retryTransaction = processPSE(pse, tableName, null);
            } finally {
                con.setAutoCommit(true);
            }
    }

    protected boolean processPSE(PSQLException pse, String tableName, String currEntry) throws PSQLException {
        final String ss = pse.getSQLState();
        boolean entry = currEntry != null;
        if ("23505".equals(ss)) { // unique_violation
            System.err.println("PSQLException: unique_violation" +
                    (entry ? ". We already have entry \"" + currEntry + "\"" : "") +
                    " in table \"" + tableName + "\". Continue.");
            return false;
        } else if ("40001".equals(ss) || "40P01".equals(ss)) { // serialization_failure, deadlock_detected
            System.err.println("PSQLException: serialization_failure or deadlock_detected. " +
                    "Retrying transaction.");
            return true;
        } else if (ss.startsWith("08") || ss.startsWith("53")) { //connection_exception, insufficient_resources
            System.err.println("PSQLException: suspected bad connection. Closing transaction " +
                    (entry ? "with first unprocessed entry: \"" + currEntry + "\"" : "") +
                    ". \n If you see this exception than we need a code fix");
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
}
