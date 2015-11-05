import crawler.Crawler;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.sql.SQLException;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class);

    public static void main(String[] args) {

        String startUser = "mi3ch";
        try {
            Crawler ljCrawler = new Crawler();
            ljCrawler.crawl(startUser);
        } catch (SQLException sqle) {
            logger.error("Error working with DB. " + sqle);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}