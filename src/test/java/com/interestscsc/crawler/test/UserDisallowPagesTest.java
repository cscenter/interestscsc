package com.interestscsc.crawler.test;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.interestscsc.crawler.loaders.UserRobotsLoader;
import com.interestscsc.crawler.parsers.UserRobotsParser;
import com.interestscsc.crawler.proxy.ProxyFactory;
import com.interestscsc.db.DBConnector;
import com.interestscsc.db.DBConnectorToCrawler;
import org.apache.http.HttpHost;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class UserDisallowPagesTest {
    private DBConnector db;
    private ProxyFactory proxyFactory;
    private static final Logger logger = Logger.getLogger(UserDisallowPagesTest.class);

    @Before
    public void setUp() {
        try {
            db = new DBConnectorToCrawler(DBConnector.DataBase.TEST, System.getProperty("user.name"));
        } catch (SQLException sqle) {
            logger.error("Error connection to DB. " + sqle);
        }
        proxyFactory = new ProxyFactory();
    }

    @Test
    public void getStatisticAboutUsersDisallowPages() {
        Queue<String> usersQueue = null;
        try {
            usersQueue = db.getRawUsers();
        } catch (SQLException e) {
            logger.error("Error getting raw users. " + e);
        }

        Assert.assertNotNull(usersQueue);

        int countDisallowFoaf = 0;
        int countDisallowFriends = 0;
        int countDisallowTag = 0;
        int countDisallowRSS = 0;
        int countDisallowCalendar = 0;
        int countDisallow2015 = 0;
        int countDisallow2012 = 0;
        int countDisallowRobots = 0;
        int countDisallowAll = 0;
        int countDisallowNone = 0;
        int countUser = usersQueue.size();
        Set<String> userDisallowNone = new HashSet<>();
        long iter = 0;
        HttpHost proxy = proxyFactory.getNextProxy();
        UserRobotsLoader loader = new UserRobotsLoader();
        while (!usersQueue.isEmpty()) {
            String nick = usersQueue.poll();
            Set<String> userDisallowPages = null;
            logger.info("Iteration: " + ++iter + " : " + nick);
            try {
                userDisallowPages = UserRobotsParser.getDisallowPages(loader.loadData(proxy, nick));
            } catch (UnirestException e) {
                proxy = proxyFactory.getNextProxy();
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

            boolean disallowed = false;
            if (userDisallowPages == null) {
                logger.warn("No access to user: " + nick);
                continue;
            }

            if (userDisallowPages.isEmpty()) {
                countDisallowNone++;
                userDisallowNone.add(nick);
                continue;
            }

            if (userDisallowPages.contains("/")) {
                disallowed = true;
                countDisallowAll++;
            }

            if (userDisallowPages.contains("/data/foaf/")) {
                disallowed = true;
                countDisallowFoaf++;
            }

            if (userDisallowPages.contains("/tag/")) {
                disallowed = true;
                countDisallowTag++;
            }

            if (userDisallowPages.contains("/data/rss/")) {
                disallowed = true;
                countDisallowRSS++;
            }

            if (userDisallowPages.contains("/misc/fdata/")) {
                disallowed = true;
                countDisallowFriends++;
            }

            if (userDisallowPages.contains("/calendar")) {
                disallowed = true;
                countDisallowCalendar++;
            }

            if (userDisallowPages.contains("/2015/")) {
                disallowed = true;
                countDisallow2015++;
            }

            if (userDisallowPages.contains("/2012/")) {
                disallowed = true;
                countDisallow2012++;
            }

            if (!disallowed) {
                countDisallowNone++;
                userDisallowNone.add(nick);
            }

            logger.info("User: " + nick + " denies access to");
            logger.info(userDisallowPages.toString());
        }

        countDisallowFoaf += countDisallowAll;
        countDisallowFriends += countDisallowAll;
        countDisallowRSS += countDisallowAll;
        countDisallowTag += countDisallowAll;
        countDisallowCalendar += countDisallowAll;
        countDisallow2012 += countDisallowAll;
        countDisallow2015 += countDisallowAll;

        logger.info("Count user: " + countUser);
        logger.info("Count disallow none: " + countDisallowNone);
        logger.info("Count disallow all: " + countDisallowAll);
        logger.info("Count disallow foaf: " + countDisallowFoaf);
        logger.info("Count disallow friends: " + countDisallowFriends);
        logger.info("Count disallow rss: " + countDisallowRSS);
        logger.info("Count disallow tag: " + countDisallowTag);
        logger.info("Count disallow calendar: " + countDisallowCalendar);
        logger.info("Count disallow 2012: " + countDisallow2012);
        logger.info("Count disallow 2015: " + countDisallow2015);
        logger.info("Count disallow robots: " + countDisallowRobots);
        logger.info("User without disallow: " + userDisallowNone.toString());

    }

}
