package crawler;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import crawler.loaders.*;
import crawler.parsers.*;
import crawler.proxy.ProxyFactory;
import data.Post;
import data.Tag;
import data.User;
import db.DBConnector;
import db.DBConnectorToCrawler;
import org.apache.http.HttpHost;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

public class Crawler {

    private static final int MAX_TRIES_RECONNECT = 5;
    private static final int MAX_NUMBER_OF_SESSIONS = 10;
    private static final int MAX_NUMBER_OF_USERS_PER_SESSION = 1000;
    private static final String RAW_PROXIES_FILE = "proxies.txt";
    private static final String WORKING_PROXIES_FILE = "working-proxies.txt";

    // queue of users who should be considered
    private Queue<String> usersQueue;

    // dictionary where key=tags' name; value=count uses of this tags
    private Map<String, Integer> allTags;

    // connector to DB
    private DBConnectorToCrawler db;

    // proxyFactory-factory
    private ProxyFactory proxyFactory;
    private HttpHost proxy;

    // set of all user's post's url
    public static Set<Long> userPostUrls;

    // set of region
    private Set<String> regions;

    // only statistics
    private List<String> usersDisallow;
    private List<String> usersNoTags;
    private List<Tag> tagsNoAccess;

    // users haven't access
    private List<String> usersNoAccess;

    private static final Logger logger = Logger.getLogger(Crawler.class);

    public Crawler(DBConnector.DataBase dataBase, String crawlerName) throws SQLException {
        try {
            db = new DBConnectorToCrawler(dataBase, crawlerName);
        } catch (SQLException sqle) {
            logger.error("Error connection to DB. " + sqle);
            throw sqle;
        }
        Unirest.setTimeouts(10000, 60000);
        usersQueue = new LinkedList<>();
        allTags = new HashMap<>();
        proxyFactory = new ProxyFactory();
    }

    public void crawl(String startUser) throws SQLException {

        logger.info("Start...");

        usersQueue.add(startUser.replaceAll("_", "-"));
        db.insertRawUsers(usersQueue);
        usersQueue.clear();
        int numberSession = MAX_NUMBER_OF_SESSIONS;
        while (numberSession-- > 0) {
            logger.info("Start " + (MAX_NUMBER_OF_SESSIONS - numberSession) + " session...");
            try {
                initStartUsersQueue();
            } catch (SQLException sqle) {
                logger.error("Error initialization of user's queue from DB. " + sqle);
                throw sqle;
            }

            // reconnect
            boolean retryResponse = true;
            int triesReconnect = MAX_TRIES_RECONNECT;
            while (retryResponse && triesReconnect-- > 0) {
                // for all users
                while (!usersQueue.isEmpty()) {

                    String nick = usersQueue.poll();
                    logger.info(nick);
                    logger.info("Left in users' queue: " + usersQueue.size());
                    Set<Tag> userTags = null;
                    User userInfo = null;
                    List<String> friends = null;
                    Boolean allowedUsers = null;
                    try {
                        allowedUsers = doesUserAllowPages(nick);
                        if (allowedUsers) {
                            friends = getUserFriends(nick);
                            userInfo = getUserInfo(nick);
                            userTags = getUserTags(nick);
                        }
                    } catch (UnirestException e) {
                        logger.warn("User: " + nick + " haven't access. Uniress exception.");
                        logger.error("User: " + nick + " haven't access. " + e);
                    } catch (InterruptedException | IllegalArgumentException | NullPointerException | IOException e) {
                        logger.error("User: " + nick + " " + e);
                    }

                    if (allowedUsers == null) {
                        usersNoAccess.add(nick);
                        proxy = proxyFactory.getNextProxy();
                        logger.warn("No access to user: " + nick);
                        continue;
                    }

                    if (!allowedUsers) {
                        usersDisallow.add(nick);
                        db.updateUserFetched(nick);
                        logger.warn("User: " + nick + " disallow our pages.");
                        continue;
                    }

                    if (userInfo == null || userTags == null || friends == null) {
                        usersNoAccess.add(nick);
                        proxy = proxyFactory.getNextProxy();
                        logger.warn("No access to user: " + nick);
                        continue;
                    }

                    if (userTags.isEmpty()) {
                        usersNoTags.add(nick);
                        logger.info("User: " + nick + " haven't any tags.");
                        db.updateUserFetched(nick);
                    }

                    logger.info("Count friends: " + friends.size());
                    logger.info("Count tags: " + userTags.size());
                    try {
                        // add information into DB
                        logger.info("Insertion friends to DB...");
                        db.insertRawUsers(friends);
                        logger.info("Insertion user info and tags to DB...");
                        db.insertUser(userInfo);
                        db.insertTags(userTags, nick);

                        // if find new region, add it into DB
                        if (userInfo.getRegion() != null && !regions.contains(userInfo.getRegion())) {
                            db.insertRegion(userInfo.getRegion());
                            regions.add(userInfo.getRegion());
                        }

                        // if user's region don't suit us
                        if (userInfo.getRegion() != null && !userInfo.getRegion().equals("RU")) {
                            db.updateUserFetched(nick);
                            continue;
                        }
                    } catch (SQLException sqle) {
                        logger.error("Inserting user's info into DB or working with region weren't successful!");
                        logger.error("Error working with DB. " + sqle);
                        continue;
                    }

                    tagsNoAccess = new ArrayList<>();
                    userPostUrls = db.getAllUserPostUrls(nick);
                    logger.info("Getting posts...");
                    for (Tag tag : userTags) {
                        logger.info("Tag: " + tag.getName() + " - " + (tag.getUses() != null ? tag.getUses() : 0) + " uses.");
                        List<Post> posts = null;
                        try {
                            posts = getTagPosts(nick, tag);
                        } catch (UnirestException | InterruptedException | ParseException | IOException e) {
                            logger.error("User: " + nick + " " + e);
                        }

                        if (posts == null) {
                            tagsNoAccess.add(tag);
                            logger.warn("No access to user: " + nick + " with tag: " + tag.getName());
                            continue;
                        }

                        if (posts.isEmpty()) {
                            logger.info("User: " + nick + " haven't NEW posts with tag: " + tag.getName()
                                    + ". All posts with this tag are already in DB.");
                            continue;
                        }

                        try {
                            db.insertPosts(posts);
                        } catch (SQLException sqle) {
                            logger.error("Inserting posts into DB wasn't successful! Tag: " + tag.getName());
                            logger.error("Error working with DB. " + sqle);
                        }

                        Integer tagUses = tag.getUses() != null ? tag.getUses() : 0;
                        Integer lastUses = allTags.containsKey(tag.getName()) ? allTags.get(tag.getName()) : 0;
                        allTags.put(tag.getName(), tagUses + lastUses);
                    }

                    try {
                        db.updateUserFetched(nick);
                    } catch (SQLException sqle) {
                        logger.error("Update user fetched wasn't successful!");
                        logger.error("Error working with DB. " + sqle);
                    }
                    // logging info about tag
                    logTagStatistics(nick, userTags);

                }

                logger.info(Arrays.toString(usersNoAccess.toArray()));
                logger.info("Count no access: " + usersNoAccess.size());
                if (!usersNoAccess.isEmpty()) {
                    usersQueue.addAll(usersNoAccess);
                    usersNoAccess.clear();
                } else {
                    retryResponse = false;
                }
            }
            // logging statistics for session
            logFinalStatistics();
            UserInfoParser.logStatistics();
        }
    }

    // logging statistics by Tag
    private void logTagStatistics(final String nick, final Set<Tag> userTags) {
        logger.info("Count tags no access: " + tagsNoAccess.size());
        if (!tagsNoAccess.isEmpty()) {
            logger.info("Tags no access:");
            for (Tag tagNoAccess : tagsNoAccess) {
                logger.info(tagNoAccess.getName());
            }
        }
        logger.info("----------------------------------------");

        int countUserTags = userTags.size();
        int countUserTagsUses = 0;
        for (Tag tag : userTags) {
            countUserTagsUses += tag.getUses() != null ? tag.getUses() : 0;
        }
        logger.info(nick + " have tags: " + countUserTags);
        logger.info(nick + " use tags: " + countUserTagsUses);
    }

    // logging statistics by user
    private void logFinalStatistics() {

        long countTags = allTags.keySet().size();
        logger.info("=============================================");
        logger.info("Count users without allowed pages: " + usersDisallow.size());
        if (!usersDisallow.isEmpty()) {
            logger.info("Users without allowed pages:");
            usersDisallow.forEach(logger::info);
        }
        logger.info("Count tags: " + countTags);
        logger.info("Count users without tags: " + usersNoTags.size());
        if (!usersNoTags.isEmpty()) {
            logger.info("Users without tags:");
            usersNoTags.forEach(logger::info);
        }
    }

    // initialization of user's queue
    // add starting user and get list of users from DB
    private void initStartUsersQueue() throws SQLException {
        proxyFactory.insertFromFile(WORKING_PROXIES_FILE, true);
        proxyFactory.insertFromFile(RAW_PROXIES_FILE, false);
        Set<String> rawAllUsers = new HashSet<>(db.getRawUsers());
        Thread proxyThread = new Thread(() -> {
            proxyFactory.findWorkingProxy(rawAllUsers);
        });
        proxyThread.start();
        proxy = proxyFactory.getNextProxy();

        //get regions
        regions = new HashSet<>(db.getRegions());

        List<String> usersToProceed = db.getUnfinishedRawUsers();
        usersToProceed.addAll(db.getReservedRawUsers());

        if (usersToProceed.size() == 0) {
            db.reserveRawUserForCrawler(MAX_NUMBER_OF_USERS_PER_SESSION);
            usersToProceed = db.getReservedRawUsers();
        }

        usersQueue.addAll(usersToProceed);

        // get users without tags
        usersDisallow = new ArrayList<>();
        usersNoTags = new ArrayList<>();
        usersNoAccess = new ArrayList<>();
    }

    // get user's info
    private User getUserInfo(final String nick) throws UnirestException, InterruptedException, IllegalArgumentException, IOException {

        logger.info("Use proxy: " + proxy.toString() + " for getting user info.");
        String response = new UserInfoLoader().loadData(proxy, nick);
        if (BaseLoader.ERROR_STATUS_PAGE.equals(response)) {
            proxy = proxyFactory.getNextProxy();
            return null;
        }
        return UserInfoParser.getUserInfo(response, nick);

    }

    // get all user's friends
    private List<String> getUserFriends(final String nick) throws UnirestException, InterruptedException, IOException {

        String response = new UserFriendsLoader().loadData(null, nick);
        if (BaseLoader.ERROR_STATUS_PAGE.equals(response)) {
            return null;
        }
        return UserFriendsParser.getFriends(response);

    }

    // get all user's tags
    private Set<Tag> getUserTags(final String nick) throws UnirestException, InterruptedException, IOException {

        logger.info("Use proxy: " + proxy.toString() + " for getting tags.");
        String response = new UserTagsLoader().loadData(proxy, nick);
        if (BaseLoader.ERROR_STATUS_PAGE.equals(response)) {
            proxy = proxyFactory.getNextProxy();
            return null;
        }
        return UserTagsParser.getTags(response, nick);

    }

    // get 25 posts by current tag
    private List<Post> getTagPosts(final String nick, final Tag tag) throws UnirestException, InterruptedException, ParseException, IOException {

        String response = new TagPostLoader().loadData(null, nick, tag.getName());
        if (BaseLoader.ERROR_STATUS_PAGE.equals(response)) {
            return null;
        }
        return TagPostParser.getPosts(response, nick);

    }

    private boolean doesUserAllowPages(final String nick) throws UnirestException, InterruptedException, IOException {

        String response = new UserRobotsLoader().loadData(null, nick);
        return !BaseLoader.ERROR_STATUS_PAGE.equals(response) && !UserRobotsParser.getDisallowPages(response).contains("/");

    }
}
