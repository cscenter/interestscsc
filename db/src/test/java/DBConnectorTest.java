import crawler.Post;
import crawler.Tag;
import db.DBConnector;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * User: allight
 * Date: 13.10.2015 0:04
 */
public class DBConnectorTest {

    public static void main(String[] args) throws Exception {
        DBConnector dbConnector = new DBConnector();
        dbConnector.setConnectionParams("localhost", "5432", "interests", "interests", "12345");
        dbConnector.connect();

        dbConnector.recreateDatabase();

        Statement st = dbConnector.getConnection().createStatement();

        Tag coffee = new Tag("coffee", 12);
        Tag java = new Tag("java", 120);
        dbConnector.insertTag(coffee);
        dbConnector.insertTag(java);
        ResultSet rs = st.executeQuery("SELECT * FROM Tag");
        while (rs.next())
            System.out.println("name: " + rs.getString(2) + "\tuses: " + rs.getString(4));
        rs.close();

        Post post = new Post("No pain..", "..no game!", "Sun, 11 Oct 2015 16:53:16 GMT",
                "http://mi3ch.livejournal.com/3098540.html", new ArrayList<String>());
        dbConnector.insertPost(post, "sssmaxusss");
        rs = st.executeQuery("SELECT title, text FROM Post");
        while (rs.next())
            System.out.println("title: " + rs.getString(1) + "\ttext: " + rs.getString(2));
        rs.close();

        st.close();
    }
}
