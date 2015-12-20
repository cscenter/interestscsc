package crawler;

import crawler.proxy.ProxyFactory;
import com.interestscsc.db.DBConnector;
import com.interestscsc.db.DBConnectorToCrawler;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class FindWorkingProxyTest {
    private ProxyFactory proxyFactory;
    private static final Logger logger = Logger.getLogger(FindWorkingProxyTest.class);

    @Before
    public void setUp() {
        proxyFactory = new ProxyFactory();
    }

    @Test
    public void findWorkingProxy() throws SQLException {
        String fileName = "proxies.txt";
        proxyFactory.insertFromFile(fileName);
        DBConnector db;
        try {
            db = new DBConnectorToCrawler(DBConnector.DataBase.TEST, System.getProperty("user.name"));
        } catch (SQLException e) {
            logger.error("Error connection to DB. " + e);
            throw e;
        }

        Queue<String> rawUsersQueue = db.getRawUsers();
        Set<String> rawUsersSet = new HashSet<>(rawUsersQueue);
        proxyFactory.findWorkingProxy(rawUsersSet);

        logger.info("Test was finished!");
        logger.info("-----------------------------");
        logger.info("Count of all proxies: " + proxyFactory.getCountRawProxies());
        logger.info("Count of working proxies: " + proxyFactory.getCountWorkingProxies());
    }
}
