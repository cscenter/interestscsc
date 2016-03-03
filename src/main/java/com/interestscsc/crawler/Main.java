package com.interestscsc.crawler;

import com.interestscsc.db.DBConnector;
import org.apache.log4j.Logger;

import java.sql.SQLException;
import java.util.Calendar;

public class Main {

    private static final Logger logger = Logger.getLogger(Main.class);

    public static void main(String[] args) {

        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);

        String startUser = "mi3ch";
        try {
            Crawler ljCrawler = new Crawler(DBConnector.DataBase.PROD, System.getProperty("user.name") + "-" +
                    year + "-" + month + "-" + day);
            ljCrawler.crawl(startUser);
        } catch (SQLException sqle) {
            logger.error("Error working with DB. " + sqle);
        }
    }
}
