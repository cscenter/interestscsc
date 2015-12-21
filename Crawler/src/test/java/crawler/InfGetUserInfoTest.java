package crawler;

import com.mashape.unirest.http.exceptions.UnirestException;
import crawler.loaders.UserInfoLoader;
import crawler.proxy.ProxyFactory;
import db.DBConnector;
import db.DBConnectorToCrawler;
import org.apache.http.HttpHost;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;

public class InfGetUserInfoTest {
    private DBConnector db;
    private ProxyFactory proxyFactory;
    private static final Logger logger = Logger.getLogger(InfGetUserInfoTest.class);

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
        HttpHost proxy = proxyFactory.getNextProxy();
        while (true) {
            if (workingUsers.isEmpty()) {
                workingUsers.addAll(rawUsers);
            }
            String nick = workingUsers.poll();
            String response = null;
            logger.info("Iteration: " + ++iter + " : " + nick);
            try {
                response = loader.loadData(proxy, nick);
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

            if (response == null) {
                logger.warn("No access to user: " + nick);
                continue;
            }
        }
    }
}
