package com.interestscsc.crawler;

import com.interestscsc.crawler.exceptions.AccessDeniedException;
import com.interestscsc.crawler.exceptions.ForbiddenPageException;
import com.interestscsc.crawler.loaders.*;
import com.interestscsc.crawler.parsers.*;
import com.interestscsc.crawler.proxy.ProxyFactory;
import com.interestscsc.data.Post;
import com.interestscsc.data.Tag;
import com.interestscsc.data.User;
import com.interestscsc.db.DBConnector;
import com.interestscsc.db.DBConnectorToCrawler;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
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
    private static final int NUMBER_TO_CHANGE_PROXY = 20;
    private static final Logger logger = Logger.getLogger(Crawler.class);

    /**
     * set of all user's post's url
     */
    public static Set<Long> userPostUrls;

    /**
     * checking: is user allowed pages?
     */
    private static Boolean allowedUser;

    /**
     * queue of users who should be considered
     */
    private Queue<String> usersQueue;

    /**
     * dictionary where key=tags' name; value=count uses of this tags
     */
    private Map<String, Integer> allTags;

    /**
     * connector to DB
     */
    private DBConnectorToCrawler db;

    /**
     * proxyFactory-factory
     */
    private ProxyFactory proxyFactory;
    private HttpHost proxy;

    /**
     * set of region
     */
    private Set<String> regions;

    /**
     * only statistics
     */
    private List<String> usersDisallow;
    private List<String> usersNoTags;
    private List<String> usersForbidden;

    /**
     * list for reconnect
     */
    private List<String> usersNoAccess;
    private List<Tag> tagsNoAccess;

    public Crawler(DBConnector.DataBase dataBase, String crawlerName) throws SQLException {
        try {
            db = new DBConnectorToCrawler(dataBase, crawlerName);
        } catch (SQLException sqle) {
            logger.error("Error connection to DB. " + sqle);
            throw sqle;
        }
        Unirest.setTimeouts(TimeUnit.SECONDS.toMillis(10), TimeUnit.SECONDS.toMillis(15));
        usersQueue = new LinkedList<>();
        allTags = new HashMap<>();
        proxyFactory = new ProxyFactory();
    }

    public void crawl(String startUser) throws SQLException {

        initCrawler(startUser);
        int numberSession = MAX_NUMBER_OF_SESSIONS;
        while (numberSession-- > 0) {
            logger.info("Start " + (MAX_NUMBER_OF_SESSIONS - numberSession) + " session...");
            initStartUsersQueue();

            /**
             * reconnect
             */
            int triesReconnect = MAX_TRIES_RECONNECT;
            while (triesReconnect-- > 0) {

                collectAllUsers();

                logger.info(Arrays.toString(usersNoAccess.toArray()));
                logger.info("Count users no access: " + usersNoAccess.size());
                if (!usersNoAccess.isEmpty()) {
                    usersQueue.addAll(usersNoAccess);
                    usersNoAccess.clear();
                } else {
                    break;
                }

                /**
                 * before reconnect crawler sleeps 10 sec, we don't want to connect on the same page too often
                 */
                sleepCrawler(10);
                logger.info("Start reconnecting...");
            }
            /**
             * logging statistics for session
             */
            logFinalStatistics();
            UserInfoParser.logStatistics();
        }
    }

    /**
     * initialization of Crawler
     * start checking proxies
     */
    private void initCrawler(final String startUser) throws SQLException {

        logger.info("Start...");

        usersQueue.add(startUser.replaceAll("_", "-"));
        db.insertRawUsers(usersQueue);
        usersQueue.clear();
        proxyFactory.setRawAllUsers(new HashSet<>(db.getRawUsers()));
        proxy = proxyFactory.getNextProxy();
    }

    /**
     * initialization of user's queue
     * add starting user and get list of users from DB
     */
    private void initStartUsersQueue() throws SQLException {
        try {
            proxy = proxyFactory.getNextProxy();
            logger.info("Starting proxy for crawler is " + proxy);
            /**
             * get regions
             */
            regions = new HashSet<>(db.getRegions());

            List<String> usersToProceed = db.getUnfinishedRawUsers();
            if (usersToProceed.isEmpty()) {
                db.reserveRawUserForCrawler(MAX_NUMBER_OF_USERS_PER_SESSION);
                usersToProceed.addAll(db.getReservedRawUsers());
            }
            usersQueue.addAll(usersToProceed);

        } catch (SQLException sqle) {
            logger.error("Error initialization of user's queue from DB. " + sqle);
            throw sqle;
        }

        usersDisallow = new ArrayList<>();
        usersNoTags = new ArrayList<>();
        usersForbidden = new ArrayList<>();
        usersNoAccess = new ArrayList<>();
    }

    private void collectAllUsers() throws SQLException {

        /**
         * for all users
         */
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
                handleException("User: " + nick + " haven't access. Unirest exception. ", e);
            } catch (InterruptedException | IllegalArgumentException | NullPointerException | IOException e) {
                handleException("User: " + nick + ". ", e);
            } catch (RuntimeException e) {
                handleException("Runtime exception from the method setProxy() for user: " + nick + " . ", e);
                logger.error("Start sleeping.");
                sleepCrawler(10);
            } catch (AccessDeniedException e) {
                handleException(e.getMessage(), e);
                usersNoAccess.add(nick);
                continue;
            } catch (ForbiddenPageException e) {
                logger.info(e.getMessage());
                usersForbidden.add(nick);
                updateUserStatusInDB(nick);
                continue;
            } finally {
                try {
                    Unirest.shutdown();
                } catch (IOException e) {
                    logger.error("User: " + nick + " " + e);
                }
            }

            if (allowedUser == null || userInfo == null || userTags == null || friends == null) {
                logger.warn("No access to user: " + nick + " or other problem. Response returns NULL!");
                continue;
            }

            if (!allowedUser) {
                usersDisallow.add(nick);
                logger.info("User: " + nick + " disallow our pages.");
            }

            logger.info("Count friends: " + friends.size());
            logger.info("Count tags: " + userTags.size());

            if (!insertionUserInfoToDB(nick, friends, userInfo, userTags))
                continue;

            if (userTags.isEmpty()) {
                usersNoTags.add(nick);
                logger.info("User: " + nick + " haven't any tags.");
                updateUserStatusInDB(nick);
                continue;
            }

            collectPosts(nick, userTags);
            updateUserStatusInDB(nick);

            /**
             * logging info about tag
             */
            logTagStatistics(nick, userTags);
        }
    }

    /**
     * collect all posts by user
     */
    private void collectPosts(final String nick, final Set<Tag> userTags) throws SQLException {
        tagsNoAccess = new ArrayList<>();
        userPostUrls = db.getAllUserPostUrls(nick);
        logger.info("Getting posts...");
        logger.info("Use proxy: " + (!allowedUser ? proxy : null) + " for getting posts of user: " + nick);
        /**
         * reconnect
         */
        int triesPostReconnect = MAX_TRIES_RECONNECT;
        while (triesPostReconnect-- > 0) {
            collectPostsByAllTags(nick, userTags);

            logger.info(Arrays.toString(tagsNoAccess.toArray()));
            logger.info("Count tags no access: " + tagsNoAccess.size());
            if (!tagsNoAccess.isEmpty()) {
                userTags.clear();
                userTags.addAll(tagsNoAccess);
                tagsNoAccess.clear();
                proxy = proxyFactory.getNextProxy();
            } else {
                break;
            }
        }
    }

    private void collectPostsByAllTags(final String nick, final Set<Tag> userTags) {
        int changeProxyCountdown = NUMBER_TO_CHANGE_PROXY;
        for (Tag tag : userTags) {
            logger.info("Tag: " + tag.getName() + " - " + (tag.getUses() != null ? tag.getUses() : 0) + " uses.");
            List<Post> posts = null;
            try {
                posts = getTagPosts(nick, tag);
            } catch (UnirestException | IOException e) {
                handleException("User: " + nick + " haven't access. Unirest exception. ", e);
                tagsNoAccess.add(tag);
                changeProxyCountdown = NUMBER_TO_CHANGE_PROXY;
                logger.info("Change proxy to: " + proxy + " for getting posts of user: " + nick);
            } catch (InterruptedException | ParseException e) {
                logger.error("User: " + nick + " " + e);
            } catch (RuntimeException e) {
                handleException("Runtime exception from the method setProxy() for user: " + nick + " . ", e);
                logger.error("Start sleeping.");
                sleepCrawler(10);
                changeProxyCountdown = NUMBER_TO_CHANGE_PROXY;
                logger.info("Change proxy to: " + proxy + " for getting posts of user: " + nick);
            } catch (AccessDeniedException e) {
                handleException(e.getMessage(), e);
                tagsNoAccess.add(tag);
                continue;
            } catch (ForbiddenPageException e) {
                logger.info(e.getMessage());
            } finally {
                try {
                    Unirest.shutdown();
                } catch (IOException e) {
                    logger.error("User: " + nick + " " + e);
                }
            }

            if (posts == null) {
                logger.warn("No access to user: " + nick + " with tag: " + tag.getName()
                        + "or other problem. Response returns NULL!");
                continue;
            }

            /**
             * every "NUMBER_TO_CHANGE_PROXY" we change proxy
             * to prevent ban and get highest speed(default 20)
             */
            changeProxyCountdown--;
            if (changeProxyCountdown == 0) {
                proxy = proxyFactory.getNextProxy();
                changeProxyCountdown = NUMBER_TO_CHANGE_PROXY;
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
    }

    private boolean insertionUserInfoToDB(final String nick,
                                          final List<String> friends,
                                          final User userInfo,
                                          final Set<Tag> userTags) {
        try {
            /**
             * add information into DB
             */
            logger.info("Insertion friends to DB...");
            db.insertRawUsers(friends);
            logger.info("Insertion user info to DB...");
            db.insertUser(userInfo);

            /**
             * if find new region, add it into DB
             */
            if (userInfo.getRegion() != null && !regions.contains(userInfo.getRegion())) {
                db.insertRegion(userInfo.getRegion());
                regions.add(userInfo.getRegion());
            }

            /**
             * if user's region don't suit us
             */
            if (userInfo.getRegion() != null && !userInfo.getRegion().equals("RU")) {
                logger.info("User: " + nick + " is not from RU. So, we ignore this user.");
                updateUserStatusInDB(nick);
                return false;
            }

            logger.info("Insertion user's tags to DB...");
            db.insertTags(userTags, nick);
        } catch (SQLException sqle) {
            logger.error("Inserting user's info into DB or working with region weren't successful!");
            logger.error("Error working with DB. " + sqle);
            return false;
        }
        return true;
    }

    /**
     * trying to update status of user
     */
    private void updateUserStatusInDB(final String nick) {
        try {
            db.updateUserFetched(nick);
        } catch (SQLException sqle) {
            logger.error("Update user fetched wasn't successful!");
            logger.error("Error working with DB. " + sqle);
        }
    }

    /**
     * sleeping
     */
    private void sleepCrawler(final long seconds) {
        try {
            Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
        } catch (InterruptedException e) {
            logger.error("Interrupt sleeping. " + e);
        }
    }

    /**
     * get user's info
     */
    private User getUserInfo(final String nick)
            throws IOException, UnirestException,
            InterruptedException, RuntimeException,
            AccessDeniedException, ForbiddenPageException {

        logger.info("Use proxy: " + proxy + " for getting info of user: " + nick);
        String response = new UserInfoLoader().loadData(proxy, nick);
        if (BaseLoader.ERROR_STATUS_PAGE.equals(response)) {
            throw new AccessDeniedException(new UserInfoLoader().getUrl() + " with user: "
                    + nick + " and proxy: " + proxy);
        }
        if (BaseLoader.FORBIDDEN_STATUS_PAGE.equals(response)) {
            throw new ForbiddenPageException(new UserInfoLoader().getUrl() + " with user: "
                    + nick + " and proxy: " + proxy);
        }
        return UserInfoParser.getUserInfo(response, nick);

    }

    /**
     * get all user's friends
     */
    private List<String> getUserFriends(final String nick)
            throws IOException, UnirestException,
            InterruptedException, RuntimeException,
            AccessDeniedException, ForbiddenPageException {

        logger.info("Use proxy: " + (!allowedUser ? proxy : null) + " for getting friends of user: " + nick);
        String response = new UserFriendsLoader().loadData(!allowedUser ? proxy : null, nick);
        if (BaseLoader.ERROR_STATUS_PAGE.equals(response)) {
            throw new AccessDeniedException(new UserInfoLoader().getUrl() + " with user: "
                    + nick + " and proxy: " + proxy);
        }
        if (BaseLoader.FORBIDDEN_STATUS_PAGE.equals(response)) {
            throw new ForbiddenPageException(new UserInfoLoader().getUrl() + " with user: "
                    + nick + " and proxy: " + proxy);
        }
        return UserFriendsParser.getFriends(response);

    }

    /**
     * get all user's tags
     */
    private Set<Tag> getUserTags(final String nick)
            throws IOException, UnirestException,
            InterruptedException, RuntimeException,
            AccessDeniedException, ForbiddenPageException {

        logger.info("Use proxy: " + proxy + " for getting tags for user: " + nick);
        String response = new UserTagsLoader().loadData(proxy, nick);
        if (BaseLoader.ERROR_STATUS_PAGE.equals(response)) {
            throw new AccessDeniedException(new UserInfoLoader().getUrl() + " with user: "
                    + nick + " and proxy: " + proxy);
        }
        if (BaseLoader.FORBIDDEN_STATUS_PAGE.equals(response)) {
            throw new ForbiddenPageException(new UserInfoLoader().getUrl() + " with user: "
                    + nick + " and proxy: " + proxy);
        }
        return UserTagsParser.getTags(response, nick);

    }

    /**
     * get 25 posts by current tag
     */
    private List<Post> getTagPosts(final String nick, final Tag tag)
            throws IOException, UnirestException,
            ParseException, InterruptedException, RuntimeException,
            AccessDeniedException, ForbiddenPageException {

        String response = new TagPostLoader().loadData(!allowedUser ? proxy : null, nick, tag.getName());
        if (BaseLoader.ERROR_STATUS_PAGE.equals(response)) {
            throw new AccessDeniedException(new UserInfoLoader().getUrl() + " with user: "
                    + nick + " ,tag: " + tag.getName() + " and proxy: " + proxy);
        }
        if (BaseLoader.FORBIDDEN_STATUS_PAGE.equals(response)) {
            throw new ForbiddenPageException(new UserInfoLoader().getUrl() + " with user: "
                    + nick + " ,tag: " + tag.getName() + " and proxy: " + proxy);
        }
        return TagPostParser.getPosts(response, nick);

    }

    private boolean doesUserAllowPages(final String nick)
            throws UnirestException, InterruptedException,
            IOException, RuntimeException {

        logger.info("Use proxy: null for getting robots.txt of user: " + nick);
        String response = new UserRobotsLoader().loadData(null, nick);
        return !BaseLoader.ERROR_STATUS_PAGE.equals(response) && !UserRobotsParser.getDisallowPages(response).contains("/");

    }

    /**
     * logging statistics by Tag
     */
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

    /**
     * logging statistics by user
     */
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
        logger.info("Count users with forbidden page: " + usersForbidden.size());
        if (!usersForbidden.isEmpty()) {
            logger.info("Users with forbidden page:");
            usersForbidden.forEach(logger::info);
        }
    }

    /**
     * handling exception
     */
    private void handleException(String message, Exception e) {
        logger.error(message + e);
        proxyFactory.setBrokenProxies(proxy);
        proxy = proxyFactory.getNextProxy();
    }
}
