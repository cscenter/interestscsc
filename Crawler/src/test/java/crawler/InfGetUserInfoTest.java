package crawler;

import com.mashape.unirest.http.exceptions.UnirestException;
import crawler.loaders.UserInfoLoader;
import db.DBConnector;
import db.DBConnectorToCrawler;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class InfGetUserInfoTest {
    private DBConnector db;
    private static final Logger logger = Logger.getLogger(InfGetUserInfoTest.class);

    @Before
    public void setUp() {
        try {
            db = new DBConnectorToCrawler(DBConnector.DataBase.TEST, System.getProperty("user.name"));
        } catch (SQLException sqle) {
            logger.error("Error connection to DB. " + sqle);
        }
    }

    @Test
    public void testGettingUserInfo() {
        Queue<String> rawUsers = null;
        try {
            rawUsers = db.getRawUsers();
        } catch (SQLException e) {
            logger.error("Error getting raw users. " + e);
        }

        Assert.assertNotNull(rawUsers);
        Queue<String> workingUsers = new LinkedList<>(rawUsers);
        long iter = 0;
        UserInfoLoader loader = new UserInfoLoader();
        while (true) {
            if (workingUsers.isEmpty()) {
                workingUsers.addAll(rawUsers);
            }
            String nick = workingUsers.poll();
            String response = null;
            logger.info("Iteration: " + ++iter + " : " + nick);
            try {
                response = loader.loadData(nick);
            } catch (UnirestException e) {
                logger.warn("User: " + nick + " haven't access. Uniress exception.");
                logger.error("User: " + nick + " haven't access. " + e);

            } catch (InterruptedException | IllegalArgumentException | NullPointerException | IOException e) {
                logger.error("User: " + nick + " " + e);
            }

            if (response == null) {
                logger.warn("No access to user: " + nick);
                continue;
            }
        }
    }
}