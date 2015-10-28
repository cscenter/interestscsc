import crawler.Post;
import crawler.Tag;
import crawler.User;
import db.DBConnector;

import java.io.FileNotFoundException;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedList;

/**
 * User: allight
 * Date: 13.10.2015 0:04
 */
public class DBConnectorTest {

    public static void main(String[] args) throws SQLException, ClassNotFoundException, FileNotFoundException {
        DBConnector dbConnector = new DBConnector();
        dbConnector.dropInitDatabase();


        LinkedList<String> regions = new LinkedList<>();
        regions.add("EN");
        regions.add("EN");
        regions.add("RU");
        dbConnector.insertRegions(regions);


        LinkedList<User> users = new LinkedList<>();
        users.add(new User(
                "sssmaxusss", "RU", Timestamp.valueOf("2015-09-17T13:09:03".replaceFirst("T", " ")),
                Timestamp.valueOf("2015-09-17T13:09:03".replaceFirst("T", " ")), null, null
        ));
        users.add(new User(
                "mi3ch", "EN", Timestamp.valueOf("2003-04-03T08:11:41".replaceFirst("T", " ")),
                Timestamp.valueOf("2015-09-17T13:09:03".replaceFirst("T", " ")),
                Date.valueOf("1966-03-27"), "бабель бабы байсикл"
        ));
        dbConnector.insertUsers(users);

        try (
                ResultSet rs = dbConnector.getConnection().createStatement()
                        .executeQuery("SELECT * FROM UserLJ")
        ) {
            while (rs.next())
                System.out.println("id: " + rs.getInt(1) +
                                "\tnick: " + rs.getString(2) +
                                "\tregion_id: " + rs.getInt(3) +
                                "\tcreated: " + rs.getTimestamp(4) +
                                "\tuodated: " + rs.getTimestamp(5) +
                                "\tfetched: " + rs.getTimestamp(6) +
                                "\tbirthday: " + rs.getDate(7) +
                                "\tinterests: " + rs.getString(8)
                );
        }

        LinkedList<Tag> tags = new LinkedList<>();
        tags.add(new Tag("coffee", 12));
        tags.add(new Tag("java", 120));
        dbConnector.insertTags(tags, "sssmaxusss");

        try (
                ResultSet rs = dbConnector.getConnection().createStatement()
                        .executeQuery("SELECT * FROM Tag")
        ) {
            while (rs.next())
                System.out.println("name: " + rs.getString(2) + "\tuses: " + rs.getString(4));
        }

        LinkedList<String> postTags = new LinkedList<>();
        postTags.add("coffee");
        postTags.add("java");
        LinkedList<Post> posts = new LinkedList<>();
        posts.add(new Post("No pain..", "..no game!", "sssmaxusss",
                Timestamp.valueOf("2015-10-19 08:11:41"),
                3098540, 20, postTags));
        dbConnector.insertPosts(posts);

        try (
                ResultSet rs = dbConnector.getConnection().createStatement()
                        .executeQuery("SELECT title, text FROM Post")
        ) {
            while (rs.next())
                System.out.println("title: " + rs.getString(1) + "\ttext: " + rs.getString(2));
        }


        dbConnector.insertNgrams(postTags, "sssmaxusss", 3098540, 2);

        try (
                ResultSet rs = dbConnector.getConnection().createStatement()
                        .executeQuery("SELECT id, text FROM Digram")
        ) {
            while (rs.next())
                System.out.println("id: " + rs.getInt(1) + "\ttext: " + rs.getString(2));
        }
    }
}
