package crawler;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import crawler.loaders.TagPostLoader;
import crawler.loaders.UserFriendsLoader;
import crawler.loaders.UserInfoLoader;
import crawler.loaders.UserTagsLoader;
import crawler.parsers.TagPostParser;
import crawler.parsers.UserFriendsParser;
import crawler.parsers.UserInfoParser;
import crawler.parsers.UserTagsParser;
import data.Post;
import data.Tag;
import data.User;
import db.DBConnector;
import org.apache.log4j.Logger;

import java.io.*;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

public class Crawler {

    private final int MAX_TRIES_RECONNECT = 5;
    private final int MAX_NUMBER_OF_SESSIONS = 5;
    private final int MAX_NUMBER_OF_USERS_PER_SESSION = 25;

    // queue of users who should be considered
    private LinkedList<String> usersQueue;

    // dictionary where key=tags' name; value=count uses of this tags
    private Map<String, Integer> allTags;

    // connector to DB
    private DBConnector db;

    // set of region
    private HashSet<String> regions;

    // only statistics
    private List<String> usersNoTags;
    private List<Tag> tagsNoAccess;

    // users haven't access
    private List<String> usersNoAccess;

    private static final Logger logger = Logger.getLogger(Crawler.class);

    public Crawler() throws SQLException {
        try {
            db = new DBConnector("sssmaximusss-pc");
        } catch (SQLException sqle) {
            logger.error("Error connection to DB. " + sqle);
            throw sqle;
        }
        Unirest.setTimeouts(10000, 60000);
        usersQueue = new LinkedList<>();
        allTags = new HashMap<>();
    }

    public void crawl(String startUser) throws SQLException {

        logger.info("Start...");
        //
        int numberSession = MAX_NUMBER_OF_SESSIONS;
        while (numberSession-- > 0) {
            logger.info("Start " + (MAX_NUMBER_OF_SESSIONS - numberSession) + " session...");
            try {
                initStartUsersQueue(startUser.replaceAll("_", "-"));
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
                    LinkedList<String> friends = new LinkedList<>();
                    try {

                        friends = getUserFriends(nick);
                        userInfo = getUserInfo(nick);
                        userTags = getUserTags(nick);

                    } catch (UnirestException e) {
                        logger.warn("User: " + nick + " haven't access. Uniress exception.");
                        logger.error("User: " + nick + " haven't access. " + e);
                    } catch (InterruptedException | UnsupportedEncodingException | IllegalArgumentException | NullPointerException e) {
                        logger.error("User: " + nick + " " + e);
                    }

                    if (userInfo == null || userTags == null || friends == null) {
                        usersNoAccess.add(nick);
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
                        //db.insertRawUsers(friends);
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

                    logger.info("Getting posts...");
                    for (Tag tag : userTags) {
                        logger.info("Tag: " + tag.getName() + " - " + (tag.getUses() != null ? tag.getUses() : 0) + " uses.");
                        List<Post> posts = null;
                        try {
                            posts = getTagPosts(nick, tag);
                        } catch (UnirestException | InterruptedException | ParseException | UnsupportedEncodingException e) {
                            logger.error("User: " + nick + " " + e);
                        }

                        if (posts == null) {
                            tagsNoAccess.add(tag);
                            logger.warn("No access to user: " + nick + " with tag: " + tag.getName());
                            continue;
                        }

                        try {
                            db.insertPosts(posts);
                            db.updateUserFetched(nick);
                        } catch (SQLException sqle) {
                            logger.error("Inserting posts into DB wasn't successful!");
                            logger.error("Error working with DB. " + sqle);
                        }

                        Integer tagUses = tag.getUses() != null ? tag.getUses() : 0;
                        Integer lastUses = allTags.containsKey(tag.getName()) ? allTags.get(tag.getName()) : 0;
                        allTags.put(tag.getName(), tagUses + lastUses);
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
        long countTagsUses = 0;

        logger.info("++++++++++++++++++++++++++++++++++++++++++");
        logger.info("ALL TAGS:");


        for (Map.Entry<String, Integer> tag : allTags.entrySet()) {
            countTagsUses += tag.getValue();

            logger.info(tag.getKey() + " : " + tag.getValue() + " uses");
        }

        logger.info("=============================================");
        logger.info("Count tags: " + countTags);
        logger.info("Count tags uses: " + countTagsUses);
        logger.info("Count users without tags: " + usersNoTags.size());
        if (!usersNoTags.isEmpty()) {
            logger.info("Users without tags:");
            usersNoTags.forEach(logger::info);
        }
    }

    // initialization of user's queue
    // add starting user and get list of users from DB
    private void initStartUsersQueue(final String startUser) throws SQLException {
        usersQueue.add(startUser);

        //get regions
        regions = new HashSet<>(db.getRegions());
        db.insertRawUsers(usersQueue);
        usersQueue.clear();

        LinkedList<String> usersToProceed = db.getUnfinishedRawUsers();
        usersToProceed.addAll(db.getReservedRawUsers());

        if (usersToProceed.size() == 0) {
            db.reserveRawUserForCrawler(MAX_NUMBER_OF_USERS_PER_SESSION);
            usersToProceed = db.getReservedRawUsers();
        }

        usersQueue.addAll(usersToProceed);

        // get users without tags
        usersNoTags = new ArrayList<>();
        usersNoAccess = new ArrayList<>();
    }

    // get user's info
    private User getUserInfo(final String nick) throws UnirestException, InterruptedException, UnsupportedEncodingException, IllegalArgumentException {

        return UserInfoParser.getUserInfo(new UserInfoLoader().loadData(nick), nick);

    }

    // get all user's friends
    private LinkedList<String> getUserFriends(final String nick) throws UnirestException, InterruptedException, UnsupportedEncodingException {

        return UserFriendsParser.getFriends(new UserFriendsLoader().loadData(nick));

    }

    // get all user's tags
    private Set<Tag> getUserTags(final String nick) throws UnirestException, InterruptedException, UnsupportedEncodingException {

        return UserTagsParser.getTags(new UserTagsLoader().loadData(nick), nick);

    }

    // get 25 posts by current tag
    private List<Post> getTagPosts(final String nick, final Tag tag) throws UnirestException, InterruptedException, UnsupportedEncodingException, ParseException {

        return TagPostParser.getPosts(new TagPostLoader().loadData(nick, tag.getName()), nick);

    }
}
