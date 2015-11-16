package db;

import data.Post;
import data.Tag;
import data.User;

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
            insertCrawler.setString(1, crawlerName);
            tryUpdateTransaction(insertCrawler, crawlerName, "crawler");
            selectCrawler.setString(1, crawlerName);
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
            insertRegion.setString(1, region);
            rowsAffected += tryUpdateTransaction(insertRegion, region, "Region");
        }
        return rowsAffected;
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
                "BEGIN; " +
                "LOCK RawUserLJ IN SHARE UPDATE EXCLUSIVE MODE; " +
                "UPDATE RawUserLJ r SET crawler_id = ? " +
                "FROM ( " +
                "       WITH mod AS (SELECT count(*)/? AS mod FROM RawUserLJRanked) " +
                "       SELECT nick FROM RawUserLJRanked r " +
                "       WHERE r.row_number % (SELECT mod FROM mod) = 0 " +
                "     ) free " +
                "WHERE r.nick = free.nick; " +
                "COMMIT;";
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


            for (User.School school : userLJ.getSchools()) {
                insertSchool.setString(1, school.getTitle());
                rowsAffected += tryUpdateTransaction(insertSchool, school.getTitle(), "School");
            }

            for (User.School school : userLJ.getSchools()) {
                insertUserToSchool.setLong(1, userId);
                insertUserToSchool.setString(2, school.getTitle());
                insertUserToSchool.setDate(3, school.getStart());
                insertUserToSchool.setDate(4, school.getEnd());

                rowsAffected += tryUpdateTransaction(insertUserToSchool,
                        userLJ.getNick() + "<->" + school.getTitle(), "UserToSchool");
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
                insertPost.setLong(1, post.getUrl());        //never null
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
                    insertTagToPost.setLong(3, post.getUrl());      //never null
                    rowsAffected += tryUpdateTransaction(insertTagToPost,
                            post.getAuthor() + ">" + post.getUrl() + "<->" + tag, "TagTOPost");
                }

            }
        }
        return rowsAffected;
    }
}
