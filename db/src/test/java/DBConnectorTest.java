import crawler.Post;
import crawler.Tag;
import db.DBConnector;

import java.io.FileNotFoundException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * User: allight
 * Date: 13.10.2015 0:04
 */
public class DBConnectorTest {

    public static void main(String[] args) throws SQLException, ClassNotFoundException, FileNotFoundException {
        DBConnector dbConnector = new DBConnector();
        dbConnector.setConnectionParams("localhost", "5432", "interests", "interests", "12345");
        dbConnector.connect();

        dbConnector.recreateDatabase();

        Statement st = dbConnector.getConnection().createStatement();

        st.execute("INSERT INTO Region VALUES\n" + //тестовые примеры
                "  (DEFAULT, 'RU'),\n" +
                "  (DEFAULT, 'EN');\n");
        st.execute("INSERT INTO UserLJ VALUES\n" +
                        "  (1, 'sssmaxusss', (SELECT id\n" +
                        "                     FROM Region\n" +
                        "                     WHERE name = 'RU'),\n" +
                        "   '2015-09-17T13:09:03', '2015-09-17T13:09:03', NULL," +
                        "   NULL, NULL),\n" +
                        "  (2, 'mi3ch', (SELECT id\n" +
                        "                FROM Region\n" +
                        "                WHERE name = 'EN'),\n" +
                        "   '2003-04-03T08:11:41', '2015-09-17T13:09:03', NULL," +
                        "   '1966-03-27', 'бабель бабы байсикл');\n"
        );

        Tag coffee = new Tag("coffee", 12);
        Tag java = new Tag("java", 120);
        dbConnector.insertTag(coffee);
        dbConnector.insertTag(java);
        ResultSet rs = st.executeQuery("SELECT * FROM Tag");
        while (rs.next())
            System.out.println("name: " + rs.getString(2) + "\tuses: " + rs.getString(4));
        rs.close();

        Post post = new Post("No pain..", "..no game!", "sssmaxusss", "Sun, 11 Oct 2015 16:53:16 GMT",
                3098540, 20, new ArrayList<String>());
        dbConnector.insertPost(post);
        rs = st.executeQuery("SELECT title, text FROM Post");
        while (rs.next())
            System.out.println("title: " + rs.getString(1) + "\ttext: " + rs.getString(2));
        rs.close();

        st.close();
    }
}
