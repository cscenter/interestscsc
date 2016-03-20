package com.interestscsc.db;

import com.interestscsc.data.NGram;
import com.interestscsc.data.User;
import org.postgresql.ds.PGPoolingDataSource;
import org.postgresql.util.PSQLException;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * User: allight
 * Date: 06.10.2015 14:47
 */

public class DBConnector {
    protected static final String SCHEMA_RESOURCE_PATH = "db/schema.sql";
    protected static final String SCHEMA_ENCODING = "UTF-8";
    protected static final String DROPDATA_PASS = "Bzw7HPtmHmVVqKvSHe7d";

    protected final DataBase dataBase;

    public DBConnector(DataBase dataBase) {
        this.dataBase = dataBase;
    }

    /**
     * Пересоздает указанную базу из схемы, указанной в SCHEMA_RESOURCE_PATH
     * с кодировкой SCHEMA_ENCODING
     */
    public static void dropInitDatabase(DataBase dataBase, String pass) throws SQLException {
        if (!DROPDATA_PASS.equalsIgnoreCase(pass))
            throw new IllegalArgumentException("Maybe you shouldn't drop db?");
        URL url = DBConnector.class.getClassLoader().getResource(SCHEMA_RESOURCE_PATH);
        String schemaSQL;
        try {
            //noinspection ConstantConditions
            schemaSQL = new Scanner(
                    new File(new URI(url.toString()).getPath()), SCHEMA_ENCODING
            ).useDelimiter("\\Z").next();
        } catch (NullPointerException | FileNotFoundException | URISyntaxException e) {
            e.printStackTrace();
            throw new IllegalStateException("resource '" + SCHEMA_RESOURCE_PATH +
                    "' could not be found or the invoker doesn't have adequate " +
                    "privileges to get the resource");
        }
        try (Connection conn = dataBase.connectionPool.getConnection()) {
            conn.createStatement().execute(schemaSQL);
        }
    }

    protected boolean checkTable(String tableName) throws SQLException {
        @SuppressWarnings("SqlResolve")
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
    @Deprecated
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

    public List<Long> getTagIdsByNames(List<String> tagNames) throws SQLException {
        if (tagNames == null)
            throw new IllegalArgumentException("Expected not null argument");
        List<Long> result = new LinkedList<>();
        if (tagNames.isEmpty())
            return result;
        String attr = ",(?)";
        StringBuilder attrs = new StringBuilder(tagNames.size() * attr.length());
        for (int i = tagNames.size() - 1; i > 0; i--)
            attrs.append(attr);
        @SuppressWarnings("SqlResolve")
        String selectTagIdsString =
                "WITH tag_list AS ( " +
                "    SELECT text FROM ( " +
                "        VALUES (?) " + attrs.toString() + " " +
                "    ) AS tag_list(text) " +
                ") " +
                "SELECT id " +
                "FROM Tag " +
                "  JOIN tag_list USING (text) " +
                "GROUP BY id ;";
        try (
                Connection con = getConnection();
                PreparedStatement selectTagIds = con.prepareStatement(selectTagIdsString)
        ) {
            int i = 0;
            for (String name : tagNames)
                selectTagIds.setString(++i, name);
            ResultSet rs = tryQueryTransaction(selectTagIds, "Tag");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getLong(1));
        }
        return result;
    }

    /**
     * @deprecated Вместо этого воспользуйтесь List<Long> getAllTagIds()
     */
    @Deprecated
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

    public List<Long> getAllTagIds() throws SQLException {
        List<Long> result = new LinkedList<>();
        String selectTagString = "SELECT id FROM Tag;";
        try (
                Connection con = getConnection();
                PreparedStatement selectTag = con.prepareStatement(selectTagString)
        ) {
            ResultSet rs = tryQueryTransaction(selectTag, "Tag");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getLong(1));
        }
        return result;
    }

    /**
     * @deprecated Вместо этого воспользуйтесь методом
     * List<Long> getAllTagIds(String userLJNick)
     */
    @Deprecated
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

    public List<Long> getAllTagIds(String userLJNick) throws SQLException {
        List<Long> result = new LinkedList<>();
        String selectTagString = "SELECT tag_id FROM TagToUserLJ " +
                "WHERE user_id = (SELECT id FROM UserLJ WHERE nick = ?);";
        try (
                Connection con = getConnection();
                PreparedStatement selectTag = con.prepareStatement(selectTagString)
        ) {
            int i = 0;
            selectTag.setString(++i, userLJNick);
            ResultSet rs = tryQueryTransaction(selectTag, "Tag");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getLong(1));
        }
        return result;
    }

    /**
     * @deprecated Вместо этого воспользуйтесь методом
     * List<Long> getAllTagIds(long postId)
     */
    @Deprecated
    public List<String> getAllTagNames(long postId) throws SQLException {
        List<String> result = new LinkedList<>();
        String selectTagNamesString = "SELECT text FROM TagNameToPost tnp " +
                "WHERE tnp.post_id = ?;";
        try (
                Connection con = getConnection();
                PreparedStatement selectTagNames = con.prepareStatement(selectTagNamesString)
        ) {
            int i = 0;
            selectTagNames.setLong(++i, postId);
            ResultSet rs = tryQueryTransaction(selectTagNames, "TagNameToPost");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getString(1));
        }
        return result;
    }

    public List<Long> getAllTagIds(long postId) throws SQLException {
        List<Long> result = new LinkedList<>();
        String selectTagIdsString =
                "SELECT tag_id FROM TagToPost WHERE post_id = ?;";
        try (
                Connection con = getConnection();
                PreparedStatement selectTagIds = con.prepareStatement(selectTagIdsString)
        ) {
            int i = 0;
            selectTagIds.setLong(++i, postId);
            ResultSet rs = tryQueryTransaction(selectTagIds, "TagToPost");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getLong(1));
        }
        return result;
    }

    /**
     * @deprecated Вместо этого воспользуйтесь методом
     * List<Long> getAllTagIds(List<Long> postIDs)
     */
    @Deprecated
    public List<String> getAllTagNames(List<Long> postIDs) throws SQLException {
        if (postIDs == null)
            throw new IllegalArgumentException("Expected not null argument");
        List<String> result = new LinkedList<>();
        if (postIDs.isEmpty())
            return result;
        String attr = ",(?)";
        StringBuilder attrs = new StringBuilder(postIDs.size() * attr.length());
        for (int i = postIDs.size() - 1; i > 0; i--)
            attrs.append(attr);
        @SuppressWarnings("SqlResolve")
        String selectTagNamesString =
                "WITH post_list AS ( " +
                "    SELECT post_id FROM ( " +
                "        VALUES (?) " + attrs.toString() + " " +
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

    public List<Long> getAllTagIds(List<Long> postIDs) throws SQLException {
        if (postIDs == null)
            throw new IllegalArgumentException("Expected not null argument");
        List<Long> result = new LinkedList<>();
        if (postIDs.isEmpty())
            return result;
        String attr = ",(?)";
        StringBuilder attrs = new StringBuilder(postIDs.size() * attr.length());
        for (int i = postIDs.size() - 1; i > 0; i--)
            attrs.append(attr);
        @SuppressWarnings("SqlResolve")
        String selectTagIdsString =
                "WITH post_list AS ( " +
                "    SELECT post_id FROM ( " +
                "        VALUES (?) " + attrs.toString() + " " +
                "    ) AS post_list(post_id) " +
                ") " +
                "SELECT tag_id " +
                "FROM TagToPost " +
                "  JOIN post_list USING (post_id) " +
                "GROUP BY tag_id ;";
        try (
                Connection con = getConnection();
                PreparedStatement selectTagIds = con.prepareStatement(selectTagIdsString)
        ) {
            int i = 0;
            for (long postID : postIDs)
                selectTagIds.setLong(++i, postID);
            ResultSet rs = tryQueryTransaction(selectTagIds, "TagToPost");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getLong(1));
        }
        return result;
    }

    /**
     * @deprecated Вместо этого воспользуйтесь методом
     * Map<Long, List<Long>> getAllTagsByPosts(List<Long> postIDs)
     */
    @Deprecated
    public Map<Long, List<String>> getAllTags(List<Long> postIDs) throws SQLException {
        if (postIDs == null)
            throw new IllegalArgumentException("Expected not null argument");
        Map<Long, List<String>> result = new HashMap<>(postIDs.size());
        if (postIDs.isEmpty())
            return result;
        for (Long postId : postIDs)
            result.put(postId, new LinkedList<>());
        String attr = ",(?)";
        StringBuilder attrs = new StringBuilder(postIDs.size() * attr.length());
        for (int i = postIDs.size() - 1; i > 0; i--)
            attrs.append(attr);
        @SuppressWarnings("SqlResolve")
        String selectTagNamesString =
                "WITH post_list AS ( " +
                "    SELECT post_id FROM ( " +
                "        VALUES (?) " + attrs.toString() + " " +
                "    ) AS post_list(post_id) " +
                "), tag_list AS ( " +
                "    SELECT post_id, tag_id id " +
                "    FROM TagToPost " +
                "      JOIN post_list USING (post_id) " +
                "    GROUP BY post_id, tag_id " +
                ") " +
                "SELECT post_id, text " +
                "FROM TAG " +
                "  JOIN tag_list USING(id) " +
                "ORDER BY post_id;";
        try (
                Connection con = getConnection();
                PreparedStatement selectTagNames = con.prepareStatement(selectTagNamesString)
        ) {
            int i = 0;
            for (long postID : postIDs)
                selectTagNames.setLong(++i, postID);
            ResultSet rs = tryQueryTransaction(selectTagNames, "TagNameToPost");
            if (rs != null) {
                List<String> currentList = null;
                Long currentId = null;
                while (rs.next()) {
                    i = 0;
                    Long nextId = rs.getLong(++i);
                    if (!nextId.equals(currentId)) {
                        currentId = nextId;
                        currentList = result.get(nextId);
                    }
                    currentList.add(rs.getString(++i));
                }
            }
        }
        return result;
    }

    public Map<Long, List<Long>> getAllTagsByPosts(List<Long> postIDs) throws SQLException {
        if (postIDs == null)
            throw new IllegalArgumentException("Expected not null argument");
        Map<Long, List<Long>> result = new HashMap<>(postIDs.size());
        if (postIDs.isEmpty())
            return result;
        for (Long postId : postIDs)
            result.put(postId, new LinkedList<>());
        String attr = ",(?)";
        StringBuilder attrs = new StringBuilder(postIDs.size() * attr.length());
        for (int i = postIDs.size() - 1; i > 0; i--)
            attrs.append(attr);
        @SuppressWarnings("SqlResolve")
        String selectTagByPostString =
                "WITH post_list AS ( " +
                "    SELECT post_id FROM ( " +
                "        VALUES (?) " + attrs.toString() + " " +
                "    ) AS post_list(post_id) " +
                ") " +
                "SELECT post_id, tag_id " +
                "FROM TagToPost " +
                "  JOIN post_list USING (post_id) " +
                "GROUP BY post_id, tag_id " +
                "ORDER BY post_id;";
        try (
                Connection con = getConnection();
                PreparedStatement selectTagByPost = con.prepareStatement(selectTagByPostString)
        ) {
            int i = 0;
            for (long postID : postIDs)
                selectTagByPost.setLong(++i, postID);
            ResultSet rs = tryQueryTransaction(selectTagByPost, "TagToPost");
            if (rs != null) {
                List<Long> currentList = null;
                Long currentPostId = null;
                while (rs.next()) {
                    i = 0;
                    Long nextPostId = rs.getLong(++i);
                    if (!nextPostId.equals(currentPostId)) {
                        currentPostId = nextPostId;
                        currentList = result.get(nextPostId);
                    }
                    currentList.add(rs.getLong(++i));
                }
            }
        }
        return result;
    }

    /**
     * Возвращает HashSet, содержащий все связи тег-пост для указанного списка
     * тегов (т.к. количество тегов вцелом меньше к-ва постов, кроме того,
     * обучаемся обычно на заданном наборе тегов).
     *
     * TODO Можно возвращать все связи, но сет будет очень большим. Как-то лучше?
     */
    public Set<TagToPostRel> getTagToPostRelations(List<Long> tagIDs) throws SQLException {
        if (tagIDs == null)
            throw new IllegalArgumentException("Expected not null argument");
        Set<TagToPostRel> result = new HashSet<>(tagIDs.size());
        if (tagIDs.isEmpty())
            return result;
        String attr = ",(?)";
        StringBuilder attrs = new StringBuilder(tagIDs.size() * attr.length());
        for (int i = tagIDs.size() - 1; i > 0; i--)
            attrs.append(attr);
        @SuppressWarnings("SqlResolve")
        String selectTagToPostSetString =
                "WITH tag_list AS ( " +
                "    SELECT tag_id FROM ( " +
                "        VALUES (?) " + attrs.toString() + " " +
                "    ) AS tag_list(tag_id) " +
                ") " +
                "SELECT  tag_id, post_id " +
                "FROM TagToPost " +
                "  JOIN tag_list USING (tag_id) " +
                "GROUP BY tag_id, post_id;";
        try (
                Connection con = getConnection();
                PreparedStatement selectTagToPostSet = con.prepareStatement(selectTagToPostSetString)
        ) {
            int i = 0;
            for (long tagID : tagIDs)
                selectTagToPostSet.setLong(++i, tagID);
            ResultSet rs = tryQueryTransaction(selectTagToPostSet, "TagToPost");
            if (rs != null) {
                while (rs.next()) {
                    i = 0;
                    result.add(new TagToPostRel(rs.getLong(++i), rs.getLong(++i)));
                }
            }
        }
        return result;
    }

    /**
     * Строит топ тегов по популярности среди нормализованных постов (post_uses),
     * с учетом количества пользователей (user_uses), которые их использовали в
     * этих постах.
     * Очки (score) начисляются тегу как произведение чисел user_uses и post_uses.
     *
     * @param minScore - нижняя граница score.
     * @param maxScore - верхняя граница score.
     *                 <p>
     *                 norm_users - пользователи, имеющий хоть один нормализованный пост
     *                 tags_user_uses - теги и количество пользователей из norm_users, имеющих их
     *                 TODO Должно быть: использовавших тег только (!) в нормализованных постах
     *                 tags_post_uses - теги и количесвто их использований в нормализованных постах
     */
    public List<String> getTopNormalizedTagNamesByScoreGap(long minScore, long maxScore) throws SQLException {
        List<String> result = new LinkedList<>();
        String selectTopTagNamesString =
                "WITH norm_users AS ( " +
                "    SELECT user_id " +
                "    FROM Post " +
                "    WHERE normalized " +
                "    GROUP BY user_id " +
                "), " +
                "tags_user_uses AS ( " +
                "    SELECT tu.tag_id, count(DISTINCT user_id) user_uses " +
                "    FROM norm_users nu " +
                "      JOIN tagtouserlj tu USING(user_id) " +
                "    GROUP BY tag_id " +
                "), " +
                "tags_post_uses AS ( " +
                "    SELECT tp.tag_id, count(post_id) post_uses " +
                "    FROM Post p " +
                "      JOIN TagToPost tp ON p.id = tp.post_id " +
                "    WHERE normalized " +
                "    GROUP BY tag_id " +
                ") " +
                "SELECT text " +
                "FROM Tag t " +
                "  JOIN tags_post_uses tpu ON t.id = tpu.tag_id " +
                "  JOIN tags_user_uses tuu ON t.id = tuu.tag_id " +
                "WHERE user_uses::BIGINT * post_uses::BIGINT BETWEEN ? AND ?" +
                "ORDER BY user_uses::BIGINT * post_uses::BIGINT DESC;";
        try (
                Connection con = getConnection();
                PreparedStatement selectTopTagNames = con.prepareStatement(selectTopTagNamesString)
        ) {
            int i = 0;
            selectTopTagNames.setLong(++i, minScore);
            selectTopTagNames.setLong(++i, maxScore);
            ResultSet rs = tryQueryTransaction(selectTopTagNames, "TagNameToPost");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getString(1));
        }
        return result;
    }

    /**
     * Строит топ тегов по популярности среди нормализованных постов (post_uses),
     * с учетом количества пользователей (user_uses), которые их использовали в
     * этих постах.
     * Очки (score) начисляются тегу как произведение чисел user_uses и post_uses.
     *
     * @param offset - сдвиг по топу.
     * @param limit  - количество требуемых позиций топа, начиная с offset.
     *               <p>
     *               norm_users - пользователи, имеющий хоть один нормализованный пост
     *               tags_user_uses - теги и количество пользователей из norm_users, имеющих их
     *               TODO Должно быть: использовавших тег только (!) в нормализованных постах
     *               tags_post_uses - теги и количесвто их использований в нормализованных постах
     */
    public List<String> getTopNormalizedTagNamesByOffset(int offset, int limit) throws SQLException {
        if (offset < 0 || limit < 0) throw new IllegalArgumentException("Arguments must be greater or equal to 0.");
        List<String> result = new LinkedList<>();
        String selectTopTagNamesString =
                "WITH norm_users AS ( " +
                "    SELECT user_id " +
                "    FROM Post " +
                "    WHERE normalized " +
                "    GROUP BY user_id " +
                "), " +
                "tags_user_uses AS ( " +
                "    SELECT tu.tag_id, count(DISTINCT user_id) user_uses " +
                "    FROM norm_users nu " +
                "      JOIN tagtouserlj tu USING(user_id) " +
                "    GROUP BY tag_id " +
                "), " +
                "tags_post_uses AS ( " +
                "    SELECT tp.tag_id, count(post_id) post_uses " +
                "    FROM Post p " +
                "      JOIN TagToPost tp ON p.id = tp.post_id " +
                "    WHERE normalized " +
                "    GROUP BY tag_id " +
                ") " +
                "SELECT text " +
                "FROM Tag t " +
                "  JOIN tags_post_uses tpu ON t.id = tpu.tag_id " +
                "  JOIN tags_user_uses tuu ON t.id = tuu.tag_id " +
                "ORDER BY user_uses::BIGINT * post_uses::BIGINT DESC " +
                "OFFSET ? LIMIT ?;";
        try (
                Connection con = getConnection();
                PreparedStatement selectTopTagNames = con.prepareStatement(selectTopTagNamesString)
        ) {
            int i = 0;
            selectTopTagNames.setInt(++i, offset);
            selectTopTagNames.setInt(++i, limit);
            ResultSet rs = tryQueryTransaction(selectTopTagNames, "TagNameToPost");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getString(1));
        }
        return result;
    }

    /**
     * Обновляет материализованное представление в базе.
     *
     * Если после последнего такого обновления, например представления tf-idf,
     * были нормализованны новые посты, для обновления информации в TFIDFSimple
     * необходимо вызвать этот метод.
     *
     * Обновление происходит полностью внутри БД. Тем не менее, операция может
     * занять много времени для больших представлений.
     * Т.о. обновления раз в сутки должно быть достаточно
     *
     * @param view предствавление, которое требует обновления.
     * @throws SQLException
     */
    public void refreshMaterializedView(MaterializedView view) throws SQLException {
        String refreshString = "REFRESH MATERIALIZED VIEW " + view.name+ ";";
        try (Connection conn = getConnection()) {
            conn.createStatement().execute(refreshString);
        }
    }

    /**
     * Извлевает из БД список н-грамм связанных с заданным списком тегов
     * {@code tagList} и максимальным значением tf-idf-simple, ограничивая
     * число выдаваемых н-грамм по {@code perTagLimit} на каждый тег.
     * Т.о. общее число возвращаемых н-грамм не превышает
     * {@code tagList.size * perTagLimit}.
     *
     * ВАЖНО: Метод не проверяет выдачу на наличие повторов между разными
     * типами н-грамм. (Пока таких повторов в базе не должно быть и пока
     * не было, а проверять это каждый раз слишном затратно.
     *
     * @param tagList список тегов
     * @param perTagLimit макс число выдаваемых н-грамм  на каждый тег
     * @return список строк - значений н-грамм
     * @throws SQLException
     */
    public List<String> getTFIDFTopNGramNamesByPerTagLimit(List<String> tagList, int perTagLimit) throws SQLException {
        if (tagList == null || perTagLimit <= 0)
            throw new IllegalStateException("Expecting notNull tagList, and positive perTagLimit");
        List<String> result = new LinkedList<>();
        if (tagList.isEmpty())
            return result;
        String attr = ",(?)";
        StringBuilder attrs = new StringBuilder(tagList.size() * attr.length());
        for (int i = tagList.size() - 1; i > 0; i--)
            attrs.append(attr);
        @SuppressWarnings("SqlResolve")
        String selectTfIdfTopNgramsString =
                "WITH tag_list AS ( " +
                "  SELECT id AS tag_id " +
                "  FROM ( " +
                "         VALUES (?) " + attrs.toString() + " " +
                "       ) AS tag_text_list(text) " +
                "    JOIN tag USING(text) " +
                "), ranked_ngram_top AS ( " +
                "  SELECT  " +
                "    type,  " +
                "    ngram_id,  " +
                "    row_number() OVER (PARTITION BY tag_id ORDER BY tf_idf DESC ) AS row_number " +
                "  FROM tag_list " +
                "    JOIN TFIDFSimple USING(tag_id) " +
                "), ranked_ngram_top_limited AS ( " +
                "  SELECT type, ngram_id " +
                "  FROM ranked_ngram_top " +
                "  WHERE row_number <= ? " +
                "  GROUP BY type, ngram_id " +
                ")  " +
                "SELECT text FROM ranked_ngram_top_limited rnt " +
                "  JOIN unigram u ON (type = 1 AND rnt.ngram_id = u.id) " +
                "UNION ALL " +
                "SELECT text FROM ranked_ngram_top_limited rnt " +
                "  JOIN digram d ON (type = 2 AND rnt.ngram_id = d.id) " +
                "UNION ALL " +
                "SELECT text FROM ranked_ngram_top_limited rnt " +
                "  JOIN trigram t ON (type = 3 AND rnt.ngram_id = t.id);";
        try (
                Connection con = getConnection();
                PreparedStatement selectTfIdfTopNgrams = con.prepareStatement(selectTfIdfTopNgramsString)
        ) {
            int i = 0;
            for (String tag : tagList)
                selectTfIdfTopNgrams.setString(++i, tag);
            selectTfIdfTopNgrams.setInt(++i, perTagLimit);
            ResultSet rs = tryQueryTransaction(selectTfIdfTopNgrams, "tf-idf");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getString(1));
        }
        return result;
    }

    /**
     * Извлевает из БД список н-грамм связанных с заданным списком тегов
     * {@code tagList} и максимальным значением tf-idf-simple, ограничивая
     * общее число выдаваемых н-грамм по {@code maxNGramNum}.
     *
     * Число н-грамм для каждого тега равно
     * {@code (maxNGramNum / totalNGramNum) * tagNGramNum }, где
     * {@code maxNGramNum} - параметр метода, {@code totalNGramNum} - общеее
     * число н-грамм для данного набора тегов, {@code tagNGramNum } - число
     * н-грамм для конкретного тега. Т.о. от каждого тега берется равная
     * часть н-грамм из числа его представляющих.
     * Проще говоря, из заданного параметра и общего числа н-грамм для набора
     * тегов вычисляется процент топовых н-грамм, который берется для каждого
     * тега.
     *
     * ВАЖНО: Метод не проверяет выдачу на наличие повторов между разными
     * типами н-грамм. (Пока таких повторов в базе не должно быть и пока
     * не было, а проверять это каждый раз слишном затратно.
     *
     * @param tagList список тегов
     * @param maxNGramNum макс число выдаваемых н-грамм
     * @return список строк - значений н-грамм
     * @throws SQLException
     */
    public List<String> getTFIDFTopNGramNamesByTotalLimit(List<String> tagList, int maxNGramNum) throws SQLException {
        if (tagList == null || maxNGramNum <= 0)
            throw new IllegalStateException("Expecting notNull tagList, and positive perTagLimit");
        List<String> result = new LinkedList<>();
        if (tagList.isEmpty())
            return result;
        String attr = ",(?)";
        StringBuilder attrs = new StringBuilder(tagList.size() * attr.length());
        for (int i = tagList.size() - 1; i > 0; i--)
            attrs.append(attr);
        @SuppressWarnings("SqlResolve")
        String selectTfIdfTopNgramsString =
                "WITH tag_list AS (" +
                "  SELECT id AS tag_id" +
                "  FROM (" +
                "         VALUES (?) " + attrs.toString() + " " +
                "       ) AS tag_text_list(text)" +
                "  JOIN tag USING(text)" +
                "), percent AS (" +
                "  SELECT (?)::FLOAT / count(*) AS val" +
                "  FROM tag_list" +
                "    JOIN TFIDFSimple USING(tag_id)" +
                "), ranked_ngram_top AS (" +
                "  SELECT" +
                "    type," +
                "    ngram_id," +
                "    row_number() OVER (PARTITION BY tag_id ORDER BY tf_idf DESC ) AS row_number," +
                "    count(*) OVER (PARTITION BY tag_id) * (SELECT val FROM percent) AS max_row" +
                "  FROM tag_list" +
                "    JOIN TFIDFSimple USING(tag_id)" +
                "), ranked_ngram_top_limited AS (" +
                "  SELECT type, ngram_id" +
                "  FROM ranked_ngram_top" +
                "  WHERE row_number <= max_row" +
                "  GROUP BY type, ngram_id" +
                ")" +
                "  SELECT text FROM ranked_ngram_top_limited rnt" +
                "    JOIN unigram u ON (type = 1 AND rnt.ngram_id = u.id)" +
                "  UNION ALL" +
                "  SELECT text FROM ranked_ngram_top_limited rnt" +
                "    JOIN digram d ON (type = 2 AND rnt.ngram_id = d.id)" +
                "  UNION ALL" +
                "  SELECT text FROM ranked_ngram_top_limited rnt" +
                "    JOIN trigram t ON (type = 3 AND rnt.ngram_id = t.id)" +
                "  LIMIT ?;";
        try (
                Connection con = getConnection();
                PreparedStatement selectTfIdfTopNgrams = con.prepareStatement(selectTfIdfTopNgramsString)
        ) {
            int i = 0;
            for (String tag : tagList)
                selectTfIdfTopNgrams.setString(++i, tag);
            for (int j = 0; j < 2; j++)
                selectTfIdfTopNgrams.setInt(++i, maxNGramNum);
            ResultSet rs = tryQueryTransaction(selectTfIdfTopNgrams, "tf-idf");
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
        //noinspection Duplicates
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
        //noinspection Duplicates
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

    public List<Long> getAllPostIds() throws SQLException {
        List<Long> result = new LinkedList<>();
        String selectPostIdsString = "SELECT id FROM Post;";
        try (
                Connection con = getConnection();
                PreparedStatement selectPostIds = con.prepareStatement(selectPostIdsString)
        ) {
            ResultSet rs = tryQueryTransaction(selectPostIds, "Post");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getLong(1));
        }
        return result;
    }

    /**
     * @deprecated Вместо этого воспользуйтесь методом
     * List<Long> getAllPostNormalizedIdsByTagIds(List<Long> tagIds)
     */
    @Deprecated
    public List<Long> getAllPostNormalizedIds(List<String> tags) throws SQLException {
        if (tags == null)
            throw new IllegalArgumentException("Expected not null argument");
        List<Long> result = new LinkedList<>();
        if (tags.isEmpty())
            return result;
        String attr = ",(?)";
        StringBuilder attrs = new StringBuilder(tags.size() * attr.length());
        for (int i = tags.size() - 1; i > 0; i--)
            attrs.append(attr);
        @SuppressWarnings("SqlResolve")
        String selectPostNormalizedIdsString =
                "WITH tag_text_list AS ( " +
                "    SELECT text " +
                "    FROM ( " +
                "        VALUES (?) " + attrs.toString() + " " +
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

    public List<Long> getAllPostNormalizedIdsByTagIds(List<Long> tagIds) throws SQLException {
        if (tagIds == null)
            throw new IllegalArgumentException("Expected not null argument");
        List<Long> result = new LinkedList<>();
        if (tagIds.isEmpty())
            return result;
        String attr = ",(?)";
        StringBuilder attrs = new StringBuilder(tagIds.size() * attr.length());
        for (int i = tagIds.size() - 1; i > 0; i--)
            attrs.append(attr);
        /**
         * Здесь в post_list еще был JOIN unigramtopost USING(post_id) - зачем,
         * если и так выбираются p.normalized?
         */
        @SuppressWarnings("SqlResolve")
        String selectPostNormalizedIdsString =
                "WITH tag_list AS ( " +
                "    SELECT tag_id " +
                "    FROM ( " +
                "        VALUES (?) " + attrs.toString() + " " +
                "    ) AS tag_list(tag_id) " +
                "), post_list AS ( " +
                "    SELECT post_id id " +
                "    FROM tag_list " +
                "      JOIN tagtopost USING(tag_id) " +
                "    GROUP BY post_id " +
                ") " +
                "SELECT id " +
                "FROM Post p " +
                "  JOIN post_list USING(id) " +
                "WHERE p.normalized;";
        try (
                Connection con = getConnection();
                PreparedStatement selectPostNormalizedIds = con.prepareStatement(selectPostNormalizedIdsString)
        ) {
            int i = 0;
            for (Long tag : tagIds)
                selectPostNormalizedIds.setLong(++i, tag);
            ResultSet rs = tryQueryTransaction(selectPostNormalizedIds, "Post");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getLong(1));
        }
        return result;
    }

    public int getPostLength(long postId) throws SQLException {
        String selectLengthString = "SELECT length FROM PostLength WHERE id = ?;";
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

    /**
     * @param postIDs список id нормализованных (!) постов
     * @return Отображение id нормализованных постов в значение их длины
     * @throws IllegalStateException если один из передаваемых постов не был нормализован
     */
    public Map<Long, Integer> getPostLength(List<Long> postIDs) throws SQLException {
        if (postIDs == null)
            throw new IllegalArgumentException("Expected not null argument");
        Map<Long, Integer> result = new HashMap<>();
        if (postIDs.isEmpty())
            return result;
        String attr = ",(?)";
        StringBuilder attrs = new StringBuilder(postIDs.size() * attr.length());
        for (int i = postIDs.size() - 1; i > 0; i--)
            attrs.append(attr);
        @SuppressWarnings("SqlResolve")
        String selectLengthString =
                "WITH post_list AS ( " +
                "    SELECT id FROM ( " +
                "        VALUES (?) " + attrs.toString() + " " +
                "    ) AS post_list(id) " +
                ")" +
                "SELECT pl.id, coalesce(sum(up.uses_cnt),0) length " +
                "FROM post_list pl " +
                "  JOIN Post p USING(id) " +
                "  LEFT JOIN UnigramToPost up ON p.id = up.post_id " +
                "WHERE p.normalized " +
                "GROUP BY pl.id;";
        try (
                Connection con = getConnection();
                PreparedStatement selectLength = con.prepareStatement(selectLengthString)
        ) {
            int i = 0;
            for (long postID : postIDs)
                selectLength.setLong(++i, postID);
            ResultSet rs = tryQueryTransaction(selectLength, "PostLength");
            if (rs != null) {
                while (rs.next()) {
                    i = 0;
                    result.put(rs.getLong(++i), rs.getInt(++i));
                }
            }
            if (postIDs.size() != result.size())
                throw new IllegalStateException("One of Requested posts wasn't normalized yet");
            else
                return result;
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

    public List<NGram> getAllNGrams(long postId) throws SQLException {
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

    public Map<Long, List<NGram>> getAllNGrams(List<Long> postIDs) throws SQLException {
        if (postIDs == null)
            throw new IllegalArgumentException("Expected not null argument");
        Map<Long, List<NGram>> result = new HashMap<>(postIDs.size());
        if (postIDs.isEmpty())
            return result;
        for (Long postId : postIDs)
            result.put(postId, new LinkedList<>());
        String attr = ",(?)";
        StringBuilder attrs = new StringBuilder(postIDs.size() * attr.length());
        for (int i = postIDs.size() - 1; i > 0; i--)
            attrs.append(attr);
        @SuppressWarnings("SqlResolve")
        String selectNGramString =
                "WITH post_list AS ( " +
                "    SELECT post_id  " +
                "    FROM ( " +
                "        VALUES (?) " + attrs.toString() + " " +
                "    ) AS post_list(post_id) " +
                ") " +
                "SELECT post_id, text, uses_cnt  " +
                "FROM AllNGramTextUsesPost " +
                "  JOIN post_list USING(post_id) " +
                "ORDER BY post_id;";
        try (
                Connection con = getConnection();
                PreparedStatement selectNGram = con.prepareStatement(selectNGramString)
        ) {
            int i = 0;
            for (long postID : postIDs)
                selectNGram.setLong(++i, postID);
            ResultSet rs = tryQueryTransaction(selectNGram, "AllNGramTextPost");
            //noinspection Duplicates
            if (rs != null) {
                List<NGram> currentList = null;
                Long currentId = null;
                while (rs.next()) {
                    i = 0;
                    Long nextId = rs.getLong(++i);
                    if (!nextId.equals(currentId)) {
                        currentId = nextId;
                        currentList = result.get(nextId);
                    }
                    currentList.add(new NGram(rs.getString(++i), null, rs.getInt(++i)));
                }
            }
        }
        return result;
    }

    public Map<Long, List<NGram>> getAllNGrams(List<Long> postIDs, NGramType nGramType) throws SQLException {
        if (postIDs == null)
            throw new IllegalArgumentException("Expected not null argument");
        Map<Long, List<NGram>> result = new HashMap<>(postIDs.size());
        if (postIDs.isEmpty())
            return result;
        for (Long postId : postIDs)
            result.put(postId, new LinkedList<>());
        String attr = ",(?)";
        StringBuilder attrs = new StringBuilder(postIDs.size() * attr.length());
        for (int i = postIDs.size() - 1; i > 0; i--)
            attrs.append(attr);
        @SuppressWarnings("SqlResolve")
        String selectNGramString =
                "WITH post_list AS ( " +
                "    SELECT post_id  " +
                "    FROM ( " +
                "      VALUES (?) " + attrs.toString() +
                "    ) AS post_list(post_id) " +
                ") " +
                "SELECT np.post_id, n.text, np.uses_cnt " +
                "FROM " + nGramType.getTableToPostName() + " np " +
                "  JOIN " + nGramType.getTableName() + " n " +
                "    ON np.ngram_id = n.id " +
                "  JOIN post_list USING(post_id) " +
                "ORDER BY np.post_id;";
        try (
                Connection con = getConnection();
                PreparedStatement selectNGram = con.prepareStatement(selectNGramString)
        ) {
            int i = 0;
            for (long postID : postIDs)
                selectNGram.setLong(++i, postID);
            ResultSet rs = tryQueryTransaction(selectNGram, nGramType.getTableName());
            //noinspection Duplicates
            if (rs != null) {
                List<NGram> currentList = null;
                Long currentId = null;
                while (rs.next()) {
                    i = 0;
                    Long nextId = rs.getLong(++i);
                    if (!nextId.equals(currentId)) {
                        currentId = nextId;
                        currentList = result.get(nextId);
                    }
                    currentList.add(new NGram(rs.getString(++i), null, rs.getInt(++i)));
                }
            }
        }
        return result;
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

    public List<String> getAllNGramNames(List<Long> postIDs) throws SQLException {
        if (postIDs == null)
            throw new IllegalArgumentException("Expected not null argument");
        List<String> result = new LinkedList<>();
        if (postIDs.isEmpty())
            return result;
        String attr = ",(?)";
        StringBuilder attrs = new StringBuilder(postIDs.size() * attr.length());
        for (int i = postIDs.size() - 1; i > 0; i--)
            attrs.append(attr);
        @SuppressWarnings("SqlResolve")
        String selectNGramString =
                "WITH post_list AS ( " +
                "    SELECT post_id  " +
                "    FROM ( " +
                "        VALUES (?) " + attrs.toString() + " " +
                "    ) AS post_list(post_id) " +
                ") " +
                "SELECT text  " +
                "FROM AllNGramTextUsesPost " +
                "  JOIN post_list USING(post_id) " +
                "GROUP BY text;";
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
        @SuppressWarnings("SqlResolve")
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
        @SuppressWarnings("SqlResolve")
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
        selectNGramString = String.format(selectNGramString, attrs.toString());
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

    public int[][] getTagByWeekStatisticsPerYear(ArrayList<Date> weekInvPos,
                                                 ArrayList<String> tagInvPos, int topLimit) throws SQLException {
        final int MAX_WEEKS_IN_YEAR = 53;
        if (weekInvPos == null || tagInvPos == null)
            throw new IllegalArgumentException("Expecting not null arguments.");
        if (topLimit <= 0) throw new IllegalArgumentException("Argument topLimit must be greater than 0.");
        String selectWeekStatString =
                "SELECT week.week::DATE " +
                "FROM generate_series('now' :: DATE - 365, 'now', '1 week') " +
                "AS week (week);";
        String selectTagStatString =
                "WITH tags_user_uses AS ( " +
                "  SELECT tu.tag_id, count(DISTINCT user_id) user_uses " +
                "  FROM tagtouserlj tu " +
                "  GROUP BY tag_id " +
                "), tags_post_uses AS ( " +
                "  SELECT tag_id, count(post_id) post_uses " +
                "  FROM TagToPost " +
                "  GROUP BY tag_id " +
                "), top_tags AS ( " +
                "  SELECT tag_id, user_uses :: BIGINT * post_uses :: BIGINT AS score " +
                "  FROM tags_post_uses tpu " +
                "    JOIN tags_user_uses tuu USING (tag_id) " +
                "  ORDER BY score DESC " +
                "  LIMIT ? " +
                "), week AS ( " +
                "  SELECT week.week" +
                "  FROM generate_series('now' :: DATE - 365, 'now', '1 week') " +
                "  AS week (week) " +
                ") " +
                "SELECT w.week :: DATE, t.text, coalesce(count(*), 0) AS count " +
                "FROM week w " +
                "  JOIN post p ON p.date BETWEEN w.week AND w.week + '1 week' " +
                "  JOIN tagtopost tp ON p.id = tp.post_id " +
                "  JOIN top_tags tt ON tp.tag_id = tt.tag_id " +
                "  JOIN tag t ON tp.tag_id = t.id " +
                "GROUP BY w.week, t.text " +
                "ORDER BY t.text, w.week;";
        try (
                Connection con = getConnection();
                PreparedStatement selectWeekStat = con.prepareStatement(selectWeekStatString);
                PreparedStatement selectTagStat = con.prepareStatement(selectTagStatString)
        ) {
            HashMap<Date, Integer> weekPos = new HashMap<>(MAX_WEEKS_IN_YEAR);
            weekInvPos.ensureCapacity(MAX_WEEKS_IN_YEAR);
            {
                ResultSet rs = tryQueryTransaction(selectWeekStat, "TempTable");
                if (rs == null) throw new IllegalStateException("If you see this exception, the code need a fix");
                int weekInc = -1;
                while (rs.next()) {
                    Date date = rs.getDate(1);
                    weekPos.put(date, ++weekInc);
                    weekInvPos.add(weekInc, date);
                }
            }

            //noinspection SuspiciousNameCombination
            selectTagStat.setInt(1, topLimit);
            ResultSet rs = tryQueryTransaction(selectTagStat, "Multiple tables");

            int tagInc = 0;
            HashMap<String, Integer> tagPos = new HashMap<>(topLimit);
            int[][] weekTagResult = new int[topLimit][MAX_WEEKS_IN_YEAR];

            if (rs != null)
                while (rs.next()) {
                    int paramInd = 0;
                    Integer week = weekPos.get(rs.getDate(++paramInd));
                    if (week == null) throw new IllegalStateException("If you see this exception, the code need a fix");
                    String tagName = rs.getString(++paramInd);
                    Integer tag = tagPos.putIfAbsent(tagName, tagInc);
                    if (tag == null) {
                        tag = tagInc;
                        tagInvPos.add(tagInc++, tagName);
                    }
                    weekTagResult[tag][week] = rs.getInt(++paramInd);
                }
            return weekTagResult;
        }
    }

    /**
     * Пытается выполнить запрос к БД, обрабатывая некоторые исключения.
     * При возникновении некоторых исключений, повторяет запрос до MAX_TRIES-1 раз.
     *
     * @return объект класса <code>ResultSet</code>, содержащий результат запроса;
     * надо проверять на <code>null</code>
     * <p>
     * TODO хочется никогда не возвращать null
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
        final String uniqueViolation = "23505";
        final String deadlockDetected = "40P01";
        final String serializationFailure = "40001";
        final String undefinedTable = "42P01";
        final String connectionExceptionClass = "08";
        final String insufficientResourcesClass = "53";

        final String ss = pse.getSQLState();
        boolean entry = currEntry != null;
        if (uniqueViolation.equals(ss)) {
            System.err.println("PSQLException: unique_violation" +
                    (entry ? ". We already have entry \"" + currEntry + "\"" : "") +
                    " in table \"" + tableName + "\". Continue.");
            return false;
        } else if (serializationFailure.equals(ss) || deadlockDetected.equals(ss)) {
            System.err.println("PSQLException: serialization_failure or deadlock_detected. " +
                    "Retrying transaction.");
            return true;
        } else if (ss.startsWith(connectionExceptionClass) ||
                ss.startsWith(insufficientResourcesClass)) {
            System.err.println("PSQLException: suspected bad connection. Closing transaction " +
                    (entry ? "with first unprocessed entry: \"" + currEntry + "\"" : "") +
                    ". \n If you see this exception than we need a code fix");
            throw pse;
        } else if (undefinedTable.equals(ss)) {
            System.err.println("PSQLException: undefined_table. Seems like you need to initialize DB, " +
                    "try DBConnector method dropInitDatabase().");
            throw pse;
        } else {
            System.err.println(ss);
            throw pse;
        }
    }

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

        /**
         * host,
         * port (5432 - стандартный для postgresql),
         * database,
         * user,
         * password,
         * maxConnections (100 - внутреннее ограничение postgresql),
         * maxTries - число попыток выполнения при временной неудаче (>1)
         */

        PROD("185.72.144.129", 5432, "veiloneru_prod", "veiloneru_prod", "veiloneru_prod", 20, 5),
        MAIN("185.72.144.129", 5432, "veiloneru", "veiloneru", "wasddsaw", 20, 5),
        TEST("185.72.144.129", 5432, "veiloneru_test", "veiloneru_test", "wasddsaw", 20, 5),
        LOCAL("localhost", 5432, "interests", "interests", "12345", 20, 5);

        private final String host;
        private final int port;
        private final String db;
        private final String user;
        private final String pass;
        private final int maxConnections;
        private final int maxTries;

        private final PGPoolingDataSource connectionPool;

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

    public enum MaterializedView {

        /**
         * Названия материализованных представлений для обновления
         */

        TFIDFSimple("TFIDFSimple");

        public final String name;

        MaterializedView(String matViewName) {
            this.name = matViewName;
        }


    }

    public static class TagToPostRel {
        public final Long tagId;
        public final Long postId;

        public TagToPostRel(Long tagId, Long postId) {
            this.tagId = tagId;
            this.postId = postId;
        }
    }
}
