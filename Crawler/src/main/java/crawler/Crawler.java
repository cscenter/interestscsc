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
import java.util.concurrent.TimeUnit;

public class Crawler {

    private static final int MAX_TRIES_RECONNECT = 5;
    private static final int MAX_NUMBER_OF_SESSIONS = 100;
    private static final int MAX_NUMBER_OF_USERS_PER_SESSION = 100;

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

    // checking: is user allowed pages?
    private static Boolean allowedUser;

    // only statistics
    private List<String> usersDisallow;
    private List<String> usersNoTags;

    private static final Logger logger = Logger.getLogger(Crawler.class);

    public Crawler(DBConnector.DataBase dataBase, String crawlerName) throws SQLException {
        try {
            db = new DBConnectorToCrawler(dataBase, crawlerName);
        } catch (SQLException sqle) {
            logger.error("Error connection to DB. " + sqle);
            throw sqle;
        }
        Unirest.setTimeouts(TimeUnit.SECONDS.toMillis(6), TimeUnit.SECONDS.toMillis(10));
        usersQueue = new LinkedList<>();
        allTags = new HashMap<>();
        proxyFactory = new ProxyFactory();
    }

    public void crawl(String startUser) throws SQLException {

        logger.info("Start...");

        usersQueue.add(startUser.replaceAll("_", "-"));
        db.insertRawUsers(usersQueue);
        usersQueue.clear();
        proxyFactory.setRawAllUsers(new HashSet<>(db.getRawUsers()));
        proxyFactory.startCheckingProxy();
        int numberSession = MAX_NUMBER_OF_SESSIONS;
        while (numberSession-- > 0) {
            logger.info("Start " + (MAX_NUMBER_OF_SESSIONS - numberSession) + " session...");
            try {
                initStartUsersQueue();
            } catch (SQLException sqle) {
                logger.error("Error initialization of user's queue from DB. " + sqle);
                throw sqle;
            }

            List<String> usersNoAccess = new ArrayList<>();
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
                    allowedUser = null;
                    try {
                        allowedUser = doesUserAllowPages(nick);
                        friends = getUserFriends(nick);
                        userInfo = getUserInfo(nick);
                        userTags = getUserTags(nick);
                    } catch (UnirestException e) {
                        logger.warn("User: " + nick + " haven't access. Unirest exception.");
                        logger.error("User: " + nick + " haven't access. " + e);
                        proxyFactory.setBrokenProxies(proxy);
                    } catch (InterruptedException | IllegalArgumentException | NullPointerException | IOException e) {
                        logger.error("User: " + nick + " " + e);
                    } catch (RuntimeException e) {
                        logger.error("Runtime exception from the method setProxy() for user: " + nick + " " + e);
                        logger.error("Start sleeping.");
                        try {
                            Thread.sleep(TimeUnit.SECONDS.toMillis(10));        // sleeping 10 sec
                        } catch (InterruptedException ie) {
                            logger.error("Interrupted sleeping. " + ie);
                        }
                        proxyFactory.setBrokenProxies(proxy);
                    } finally {
                        try {
                            Unirest.shutdown();
                        } catch (IOException e) {
                            logger.error("User: " + nick + " " + e);
                        }
                        proxy = proxyFactory.getNextProxy();
                    }

                    if (allowedUser == null) {
                        usersNoAccess.add(nick);
                        logger.warn("No access to user: " + nick);
                        continue;
                    }

                    if (!allowedUser) {
                        usersDisallow.add(nick);
                        logger.info("User: " + nick + " disallow our pages.");
                    }

                    if (userInfo == null || userTags == null || friends == null) {
                        usersNoAccess.add(nick);
                        logger.warn("No access to user: " + nick);
                        continue;
                    }

                    logger.info("Count friends: " + friends.size());
                    logger.info("Count tags: " + userTags.size());
                    try {
                        // add information into DB
                        logger.info("Insertion friends to DB...");
                        db.insertRawUsers(friends);
                        logger.info("Insertion user info to DB...");
                        db.insertUser(userInfo);

                        // if find new region, add it into DB
                        if (userInfo.getRegion() != null && !regions.contains(userInfo.getRegion())) {
                            db.insertRegion(userInfo.getRegion());
                            regions.add(userInfo.getRegion());
                        }

                        // if user's region don't suit us
                        if (userInfo.getRegion() != null && !userInfo.getRegion().equals("RU")) {
                            logger.info("User: " + nick + " is not from RU. So, we ignore this user.");
                            db.updateUserFetched(nick);
                            continue;
                        }

                        logger.info("Insertion user's tags to DB...");
                        db.insertTags(userTags, nick);
                    } catch (SQLException sqle) {
                        logger.error("Inserting user's info into DB or working with region weren't successful!");
                        logger.error("Error working with DB. " + sqle);
                        continue;
                    }

                    if (userTags.isEmpty()) {
                        usersNoTags.add(nick);
                        logger.info("User: " + nick + " haven't any tags.");
                        db.updateUserFetched(nick);
                        continue;
                    }

                    List<Tag> tagsNoAccess = new ArrayList<>();

                    userPostUrls = db.getAllUserPostUrls(nick);
                    logger.info("Getting posts...");
                    logger.info("Use proxy: " + (!allowedUser ? proxy : null) + " for getting posts of user: " + nick);
                    // reconnect
                    boolean retryPostResponse = true;
                    int triesPostReconnect = MAX_TRIES_RECONNECT;
                    while (retryPostResponse && triesPostReconnect-- > 0) {
                        final int startChangeProxyCountdown = 10;
                        int changeProxyCountdown = startChangeProxyCountdown;
                        for (Tag tag : userTags) {
                            logger.info("Tag: " + tag.getName() + " - " + (tag.getUses() != null ? tag.getUses() : 0) + " uses.");
                            List<Post> posts = null;
                            try {
                                posts = getTagPosts(nick, tag);
                            } catch (UnirestException | IOException e) {
                                logger.error("User: " + nick + " " + e);
                                tagsNoAccess.add(tag);
                                proxyFactory.setBrokenProxies(proxy);
                                proxy = proxyFactory.getNextProxy();
                                changeProxyCountdown = startChangeProxyCountdown;
                                logger.info("Change proxy to: " + proxy + " for getting posts of user: " + nick);
                            } catch (InterruptedException | ParseException e) {
                                logger.error("User: " + nick + " " + e);
                            } catch (RuntimeException e) {
                                logger.error("Runtime exception from the method setProxy() for user: " + nick + " " + e);
                                logger.error("Start sleeping.");
                                try {
                                    Thread.sleep(TimeUnit.SECONDS.toMillis(10));
                                } catch (InterruptedException ie) {
                                    logger.error("Interrupted sleeping. " + ie);
                                }
                                proxyFactory.setBrokenProxies(proxy);
                                proxy = proxyFactory.getNextProxy();
                                changeProxyCountdown = startChangeProxyCountdown;
                                logger.info("Change proxy to: " + proxy + " for getting posts of user: " + nick);
                            } finally {
                                try {
                                    Unirest.shutdown();
                                } catch (IOException e) {
                                    logger.error("User: " + nick + " " + e);
                                }
                            }

                            if (posts == null) {
                                logger.warn("No access to user: " + nick + " with tag: " + tag.getName());
                                continue;
                            }

                            changeProxyCountdown--;
                            if (changeProxyCountdown == 0) {
                                proxy = proxyFactory.getNextProxy();
                                changeProxyCountdown = startChangeProxyCountdown;
                                logger.info("Change proxy to: " + proxy + " for getting posts of user: " + nick);
                            }

                            if (posts.isEmpty()) {
                                logger.info("User: " + nick + " haven't NEW posts with tag: " + tag.getName()
                                        + ". All posts with this tag are already in DB.");
                                continue;
                            }

                            try {
                                db.insertPosts(posts);
                            } catch (SQLException sqle) {
                                logger.error("Inserting posts into DB wasn't successful! Tag: " + tag.getName() + " by user: " + nick);
                                logger.error("Error working with DB. " + sqle);
                            }

                            Integer tagUses = tag.getUses() != null ? tag.getUses() : 0;
                            Integer lastUses = allTags.containsKey(tag.getName()) ? allTags.get(tag.getName()) : 0;
                            allTags.put(tag.getName(), tagUses + lastUses);
                        }

                        logger.info(Arrays.toString(tagsNoAccess.toArray()));
                        logger.info("Count tags no access: " + tagsNoAccess.size());
                        if (!tagsNoAccess.isEmpty()) {
                            userTags.clear();
                            userTags.addAll(tagsNoAccess);
                            tagsNoAccess.clear();
                            proxy = proxyFactory.getNextProxy();
                        } else {
                            retryPostResponse = false;
                        }
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
                logger.info("Count users no access: " + usersNoAccess.size());
                if (!usersNoAccess.isEmpty()) {
                    usersQueue.addAll(usersNoAccess);
                    usersNoAccess.clear();
                } else {
                    retryResponse = false;
                }

                // before reconnect crawler sleeps 10 sec, we don't want to connect on the same page too often
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(10));
                } catch (InterruptedException e) {
                    logger.error("Interrupt sleeping before reconnect. " + e);
                }
                logger.info("Start reconnecting...");
            }
            // logging statistics for session
            logFinalStatistics();
            UserInfoParser.logStatistics();
        }
    }

    // logging statistics by Tag
    private void logTagStatistics(final String nick, final Set<Tag> userTags) {
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

        proxy = proxyFactory.getNextProxy();
        logger.info("Starting proxy for crawler is " + proxy);
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
    }

    // get user's info
    private User getUserInfo(final String nick) throws IOException, UnirestException, InterruptedException, RuntimeException {

        logger.info("Use proxy: " + proxy + " for getting info of user: " + nick);
        String response = new UserInfoLoader().loadData(proxy, nick);
        if (BaseLoader.ERROR_STATUS_PAGE.equals(response)) {
            proxy = proxyFactory.getNextProxy();
            return null;
        }
        return UserInfoParser.getUserInfo(response, nick);

    }

    // get all user's friends
    private List<String> getUserFriends(final String nick) throws IOException, UnirestException, InterruptedException, RuntimeException {

        logger.info("Use proxy: " + (!allowedUser ? proxy : null) + " for getting friends of user: " + nick);
        String response = new UserFriendsLoader().loadData(!allowedUser ? proxy : null, nick);
        if (BaseLoader.ERROR_STATUS_PAGE.equals(response)) {
            return null;
        }
        return UserFriendsParser.getFriends(response);

    }

    // get all user's tags
    private Set<Tag> getUserTags(final String nick) throws IOException, UnirestException, InterruptedException, RuntimeException {

        logger.info("Use proxy: " + proxy + " for getting tags for user: " + nick);
        String response = new UserTagsLoader().loadData(proxy, nick);
        if (BaseLoader.ERROR_STATUS_PAGE.equals(response)) {
            proxy = proxyFactory.getNextProxy();
            return null;
        }
        return UserTagsParser.getTags(response, nick);

    }

    // get 25 posts by current tag
    private List<Post> getTagPosts(final String nick, final Tag tag) throws IOException, UnirestException, ParseException, InterruptedException, RuntimeException {

        String response = new TagPostLoader().loadData(!allowedUser ? proxy : null, nick, tag.getName());
        if (BaseLoader.ERROR_STATUS_PAGE.equals(response)) {
            return null;
        }
        return TagPostParser.getPosts(response, nick);

    }

    private boolean doesUserAllowPages(final String nick) throws UnirestException, InterruptedException, IOException, RuntimeException {

        logger.info("Use proxy: null for getting robots.txt of user: " + nick);
        String response = new UserRobotsLoader().loadData(null, nick);
        return !BaseLoader.ERROR_STATUS_PAGE.equals(response) && !UserRobotsParser.getDisallowPages(response).contains("/");

    }
}
