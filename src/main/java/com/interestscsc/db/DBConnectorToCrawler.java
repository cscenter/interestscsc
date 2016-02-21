package com.interestscsc.db;

import com.interestscsc.data.Post;
import com.interestscsc.data.Tag;
import com.interestscsc.data.User;
import org.postgresql.util.PSQLException;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;


/**
 * User: allight
 * Date: 06.10.2015 14:47
 */

@SuppressWarnings("Duplicates")
public class DBConnectorToCrawler extends DBConnector {
    private Integer crawlerId;

    public DBConnectorToCrawler(DataBase dataBase, String crawlerName) throws SQLException {
        super(dataBase);
        setNewCrawlerName(crawlerName);
    }

    public void setNewCrawlerName(String crawlerName) throws SQLException {
        if (checkTable("crawler"))
            this.crawlerId = getCrawlerId(crawlerName);
        else
            System.err.println("WARNING: There is no table \"crawler\" in current DB. " +
                    "Maybe you'll need to recreate DB with method <code>dropInitDatabase()</code>, " +
                    "and then manually set you crawler name with method <code>setNewCrawlerName(..)</code>");
    }

    private Integer getCrawlerId(String crawlerName) throws SQLException {
        String insertCrawlerString =
                "INSERT INTO crawler (name) VALUES (?);";
        String selectCrawlerString = "SELECT id FROM crawler WHERE name = ?;";
        try (
                Connection con = getConnection();
                PreparedStatement insertCrawler = con.prepareStatement(insertCrawlerString);
                PreparedStatement selectCrawler = con.prepareStatement(selectCrawlerString)
        ) {
            int i = 0;
            insertCrawler.setString(++i, crawlerName);
            tryUpdateTransaction(insertCrawler, crawlerName, "crawler");
            i = 0;
            selectCrawler.setString(++i, crawlerName);
            ResultSet rs = tryQueryTransaction(selectCrawler, "crawler");
            if (rs == null || !rs.next())
                throw new IllegalStateException("If you see this, our code needs a fix");
            return rs.getInt("id");
        }
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
            int i = 0;
            insertRegion.setString(++i, region);
            rowsAffected += tryUpdateTransaction(insertRegion, region, "Region");
        }
        return rowsAffected;
    }

    /**
     * Добавляет в БД ники пользователей из любого итерабельного контейнера.
     */
    public void insertRawUsers(Iterable<String> rawUsersLJ) throws SQLException {
        String createTempTableString =
                "CREATE TEMPORARY TABLE RawUserLJTemp (nick TEXT) ON COMMIT DROP";
        //noinspection SqlResolve
        String insertUserString =
                "INSERT INTO RawUserLJTemp (nick) VALUES (?);";
        //noinspection SqlResolve
        String updateMainTableString =
                "LOCK RawUserLJ IN SHARE UPDATE EXCLUSIVE MODE; " +
                "DELETE FROM RawUserLJTemp rt USING RawUserLJ r WHERE rt.nick = r.nick; " +
                "INSERT INTO rawuserlj (nick) SELECT nick FROM RawUserLJTemp GROUP BY nick; ";

        boolean retryTransaction = true;
        int tries = dataBase.getMaxTries();
        while (retryTransaction && tries > 0)
            try (
                    Connection con = getConnection();
                    PreparedStatement createTempTable = con.prepareStatement(createTempTableString);
                    PreparedStatement insertUser = con.prepareStatement(insertUserString);
                    PreparedStatement updateMainTable = con.prepareStatement(updateMainTableString)
            ) {
                retryTransaction = false;
                --tries;

                try {
                    con.setAutoCommit(false);
                    createTempTable.execute();
                    for (String user : rawUsersLJ) {
                        int i = 0;
                        insertUser.setString(++i, user);
                        insertUser.execute();
                    }
                    updateMainTable.execute();
                    con.commit();
                } finally {
                    con.setAutoCommit(true);
                }
            } catch (PSQLException pse) {
                retryTransaction = processPSE(pse,"RawUserLJ",null);
            }
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
                "BEGIN; " +
                "LOCK RawUserLJ IN SHARE UPDATE EXCLUSIVE MODE; " +
                "UPDATE RawUserLJ r " +
                "SET crawler_id = ? " +
                "FROM ( " +
                "    WITH max_r AS ( " +
                "      SELECT count(*)-1 FROM RawUserLJRanked " +
                "    ), " +
                "    rand AS ( " +
                "      SELECT 1 + (random() * (SELECT * FROM max_r)) :: INTEGER AS row_number " +
                "      FROM generate_series(1, ?) " +
                "      GROUP BY row_number " +
                "    ) " +
                "    SELECT nick  " +
                "    FROM RawUserLJRanked r " +
                "    JOIN rand USING(row_number) " +
                "     ) free " +
                "WHERE r.nick = free.nick; " +
                "COMMIT;";
        try (
                Connection con = getConnection();
                PreparedStatement reserveUserNicks = con.prepareStatement(reserveUserNicksString)
        ) {
            int i = 0;
            reserveUserNicks.setInt(++i, crawlerId);
            reserveUserNicks.setInt(++i, reserveNum);
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
                PreparedStatement selectReserved = con.prepareStatement(selectReservedString)
        ) {
            int i = 0;
            selectReserved.setInt(++i, crawlerId);
            ResultSet rs = tryQueryTransaction(selectReserved, "RawUserLJ");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getString(1));
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
                PreparedStatement selectUnfinished = con.prepareStatement(selectUnfinishedString)
        ) {
            int i = 0;
            selectUnfinished.setInt(++i, crawlerId);
            ResultSet rs = tryQueryTransaction(selectUnfinished, "RawUserLJ JOIN UserLJ");
            if (rs != null)
                while (rs.next())
                    result.add(rs.getString(1));
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
            int i = 0;
            insertUser.setString(++i, userLJ.getNick());
            insertUser.setString(++i, userLJ.getRegion());
            insertUser.setTimestamp(++i, userLJ.getDateCreated());
            insertUser.setTimestamp(++i, userLJ.getDateUpdated());
            insertUser.setDate(++i, userLJ.getBirthday());
            insertUser.setString(++i, userLJ.getInterests());
            insertUser.setString(++i, userLJ.getCustomCity());
            if (userLJ.getPostsNum() == null)
                insertUser.setNull(++i, Types.INTEGER);
            else insertUser.setInt(++i, userLJ.getPostsNum());
            if (userLJ.getCommentsReceived() == null)
                insertUser.setNull(++i, Types.INTEGER);
            else insertUser.setInt(++i, userLJ.getCommentsReceived());
            if (userLJ.getCommentsPosted() == null)
                insertUser.setNull(++i, Types.INTEGER);
            else insertUser.setInt(++i, userLJ.getCommentsPosted());
            insertUser.setString(++i, userLJ.getBiography());

            rowsAffected += tryUpdateTransaction(insertUser, userLJ.getNick(), "UserLJ");

            i = 0;
            selectUserLJ.setString(++i, userLJ.getNick());
            ResultSet rs = tryQueryTransaction(selectUserLJ, "UserLJ");
            if (rs == null || !rs.next())
                throw new IllegalStateException("If you see this, our code needs a fix");
            Long userId = rs.getLong("id");

            for (User.School school : userLJ.getSchools()) {
                i = 0;
                insertSchool.setString(++i, school.getTitle());
                rowsAffected += tryUpdateTransaction(insertSchool, school.getTitle(), "School");
            }

            for (User.School school : userLJ.getSchools()) {
                i = 0;
                insertUserToSchool.setLong(++i, userId);
                insertUserToSchool.setString(++i, school.getTitle());
                insertUserToSchool.setDate(++i, school.getStart());
                insertUserToSchool.setDate(++i, school.getEnd());

                rowsAffected += tryUpdateTransaction(insertUserToSchool,
                        userLJ.getNick() + "<->" + school.getTitle(), "UserToSchool");
            }

            i = 0;
            updateRawUser.setLong(++i, userId);
            updateRawUser.setString(++i, userLJ.getNick());

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
            int i = 0;
            updateFetched.setTimestamp(++i, Timestamp.valueOf(LocalDateTime.now()));
            updateFetched.setString(++i, userLJNick);

            rowsAffected += tryUpdateTransaction(updateFetched, userLJNick, "UserLJ");
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
                int i = 0;
                insertTag.setString(++i, tag.getName());
                rowsAffected += tryUpdateTransaction(insertTag, tag.getName(), "Tag");

                i = 0;
                insertTagToUserLJ.setString(++i, tag.getName());
                insertTagToUserLJ.setString(++i, userLJNick);
                if (tag.getUses() == null)
                    insertTagToUserLJ.setNull(++i, Types.INTEGER);
                else insertTagToUserLJ.setInt(++i, tag.getUses());
                rowsAffected += tryUpdateTransaction(insertTagToUserLJ,
                        tag.getName() + "<->" + userLJNick, "TagToUserLJ");
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
                int i = 0;
                insertPost.setLong(++i, post.getUrl());        //never null
                insertPost.setString(++i, post.getAuthor());
                insertPost.setTimestamp(++i, post.getDate());
                insertPost.setString(++i, post.getTitle());
                insertPost.setString(++i, post.getText());
                if (post.getCountComment() == null)
                    insertPost.setNull(++i, Types.INTEGER);
                else insertPost.setInt(++i, post.getCountComment());

                rowsAffected += tryUpdateTransaction(insertPost, post.getAuthor() + ">" + post.getUrl(), "Post");

                assert (post.getTags() != null);
                for (String tag : post.getTags()) {
                    i = 0;
                    insertTagToPost.setString(++i, tag);
                    insertTagToPost.setString(++i, post.getAuthor());
                    insertTagToPost.setLong(++i, post.getUrl());      //never null
                    rowsAffected += tryUpdateTransaction(insertTagToPost,
                            post.getAuthor() + ">" + post.getUrl() + "<->" + tag, "TagTOPost");
                }

            }
        }
        return rowsAffected;
    }
}
