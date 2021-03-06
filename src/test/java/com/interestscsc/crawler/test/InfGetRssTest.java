package com.interestscsc.crawler.test;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.interestscsc.crawler.loaders.TagPostLoader;
import com.interestscsc.db.DBConnector;
import com.interestscsc.db.DBConnectorToCrawler;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class InfGetRssTest {
    private DBConnector db;
    private static final Logger logger = Logger.getLogger(InfGetRssTest.class);

    @Before
    public void setUp() {
        try {
            db = new DBConnectorToCrawler(DBConnector.DataBase.TEST, System.getProperty("user.name"));
        } catch (SQLException sqle) {
            logger.error("Error connection to DB. " + sqle);
        }
    }

    @Test
    public void testGettingRSS() {

        String nick = "mi3ch";
        List<String> rawTags = null;
        try {
            rawTags = db.getAllTagNames(nick);
        } catch (SQLException e) {
            logger.error("User: " + nick + " " + e);
        }

        Assert.assertNotNull(rawTags);
        Queue<String> workingTags = new LinkedList<>(rawTags);
        long iter = 0;
        //noinspection InfiniteLoopStatement
        while (true) {
            if (workingTags.isEmpty()) {
                workingTags.addAll(rawTags);
            }
            String tagName = workingTags.poll();
            String response = null;
            logger.info("Iteration: " + ++iter + " : " + tagName);
            try {
                TagPostLoader loader = new TagPostLoader();
                response = loader.loadData(null, nick, tagName);
            } catch (UnirestException e) {
                logger.warn("User: " + nick + " haven't access. Unirest exception.");
                logger.error("User: " + nick + " haven't access. " + e);

            } catch (InterruptedException | IllegalArgumentException | NullPointerException | IOException e) {
                logger.error("User: " + nick + " " + e);
            } catch (RuntimeException e) {
                logger.error("User: " + nick + " " + e);
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ie) {
                    logger.error("Interrupted sleeping. " + ie);
                }
            }

            if (response == null)
                logger.warn("No access to user: " + nick);
        }
    }
}
