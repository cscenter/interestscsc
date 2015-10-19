package db;

import crawler.Post;
import crawler.Tag;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Scanner;

/**
 * User: allight
 * Date: 06.10.2015 14:47
 */

public class DBConnector {
    private static final String SCHEMA_PATH = "db/schema.sql";
    private static final String SCHEMA_ENCODING = "UTF-8";
    private String host = "localhost"; //TODO храним здесь или оставляем метод для задания
    private String port = "5432";
    private String db = "interests";
    private String user = "interests";
    private String pass = "12345";
    private Connection connection;

    public DBConnector setConnectionParams(String host, String port, String db, String user, String pass) {
        this.host = host;
        this.port = port;
        this.db = db;
        this.user = user;
        this.pass = pass;
        return this;
    }

    public DBConnector connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.postgresql.Driver");
        String url = "jdbc:postgresql://" + host + ":" + port + "/" + db;
        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", pass);
        connection = DriverManager.getConnection(url, props);
        return this;
    }

    public Connection getConnection() throws SQLException, ClassNotFoundException {
        if (connection == null) connect();
        return connection;
    }

    public DBConnector recreateDatabase() throws SQLException, FileNotFoundException {
        String schemaSQL = new Scanner(new File(SCHEMA_PATH), SCHEMA_ENCODING).useDelimiter("\\Z").next();
        connection.createStatement().execute(schemaSQL);
        return this;
    }

    public DBConnector insertTag(Tag tag) throws SQLException { //TODO нужно как-то возвращать успешность внесенных изменений
        connection.createStatement().execute("INSERT INTO Tag VALUES " +
                "(DEFAULT, '" + tag.getName() + "', NULL, NULL );");
        return this;
    }

    public DBConnector insertPost(Post post) throws SQLException {
        connection.createStatement().execute( //Не очень красивый способ. найти параметризованный
                "INSERT INTO Post VALUES " +
                        "(DEFAULT, " +
                        post.getUrl() + ", " +
                        "(SELECT id FROM UserLJ WHERE nick = '" + post.getAuthor() + "'), " +
                        "'" + post.getDate() + "', " +
                        "'" + post.getTitle() + "', " +
                        "'" + post.getText() + "', " +
                        "NULL, " +
                        post.getCountComment() +
                        ");"
        );
        return this;
    }
}
