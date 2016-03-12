package com.interestscsc.crawler;

import com.interestscsc.db.DBConnector;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class);

    public static void main(String[] args) {

        String startUser = "mi3ch";
        try {
            Crawler ljCrawler = new Crawler(DBConnector.DataBase.PROD, System.getProperty("user.name"));
            ljCrawler.crawl(startUser);
        } catch (SQLException sqle) {
            logger.error("Error working with DB. " + sqle);
        }
    }
}