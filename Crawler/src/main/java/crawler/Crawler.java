package crawler;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import data.Post;
import data.Tag;
import data.User;
import db.DBConnector;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URLEncoder;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Crawler {

    // Post's selectors
    private static final String POST_SELECTOR = "item";
    private static final String TITLE_SELECTOR = "title";
    private static final String TEXT_SELECTOR = "description";
    private static final String DATE_SELECTOR = "pubDate";
    private static final String URL_SELECTOR = "guid";
    private static final String TAG_SELECTOR = "category";
    private static final String COUNT_COMMENTS_SELECTOR = "lj:reply-count";

    // User's selectors
    private static final String PERSON_SELECTOR = "foaf:Person";
    private static final String REGION_SELECTOR = "ya:country";
    private static final String WEBLOG_SELECTOR = "foaf:weblog";
    private static final String DATE_CREATED_SELECTOR = "lj:dateCreated";
    private static final String DATE_LAST_UPDATED_SELECTOR = "lj:dateLastUpdated";
    private static final String BIRTHDAY_SELECTOR = "foaf:dateOfBirth";
    private static final String INTEREST_SELECTOR = "foaf:interest";
    private static final String ATTR_TITLE_SELECTOR = "dc:title";

    private final int MAX_TAG_USES_POW = 6; // max uses count 1e6
    private final int MAX_TRIES = 5;
    // set of all users
    private Set<String> allUsers;

    // queue of users who should be considered
    private LinkedList<String> usersQueue;

    // dictionary where key=tags' name; value=count uses of this tags
    private Map<String, Integer> allTags;

    // connector to DB
    private DBConnector db;

    private static final Logger logger = Logger.getLogger(Crawler.class);
    private static int countNullRegion = 0;
    private static int countNullDateCreated = 0;
    private static int countNullDateUpdated = 0;
    private static int countNullBirthday = 0;
    private static int countNullInterests = 0;

    public Crawler() throws SQLException {
        try {
            db = new DBConnector("sssmaximusss-pc");
        } catch (SQLException sqle) {
            logger.error("Error connection to DB. " + sqle);
            throw sqle;
        }
        allUsers = new HashSet<>();
        usersQueue = new LinkedList<>();
        allTags = new HashMap<>();
    }

    public void crawl(String startUser) throws SQLException, FileNotFoundException {

        Unirest.setTimeouts(10000, 60000);
        logger.info("Start...");
        startUser = startUser.replaceAll("_", "-");
        usersQueue.add(startUser);

        //get regions
        HashSet<String> regions = new HashSet<>(db.getRegions());
        db.insertRawUsers(usersQueue);
        usersQueue.clear();

        LinkedList<String> usersToProceed = db.getUnfinishedRawUsers();
        usersToProceed.addAll(db.getReservedRawUsers());

        if (usersToProceed.size() == 0) {
            db.reserveRawUserForCrawler(25);
            usersToProceed = db.getReservedRawUsers();
        }

        allUsers.addAll(usersToProceed);
        usersQueue.addAll(usersToProceed);

        // get users without tags
        long countUsersNoTags = 0;
        List<String> usersNoTags = new ArrayList<>();

        long countUsersNoAccess = 0;
        List<String> usersNoAccess = new ArrayList<>();

        boolean retryResponse = true;
        int tries = MAX_TRIES;
        // for all users
        while (!usersQueue.isEmpty()) {

            String user = usersQueue.poll();
            logger.info(user);
            logger.info(usersQueue.size() + " / " + allUsers.size());
            Set<Tag> userTags = null;
            User userInfo = null;
            LinkedList<String> friends = new LinkedList<>();
            try {

                friends = getUserFriends(user);
                userInfo = getUserInfo(user);
                userTags = getUserTags(user);

            } catch (UnirestException e) {
                logger.warn("User: " + user + " haven't access. Uniress exception.");
                logger.error("User: " + user + " haven't access. " + e);
            } catch (InterruptedException | UnsupportedEncodingException | IllegalArgumentException | NullPointerException e) {
                logger.error("User: " + user + " " + e);
            }

            if (userInfo == null || userTags == null || friends == null) {
                countUsersNoAccess++;
                usersNoAccess.add(user);
                logger.warn("No access to user: " + user);
                continue;
            }

            if (userTags.isEmpty()) {
                countUsersNoTags++;
                usersNoTags.add(user);
                logger.info("User: " + user + " haven't any tags.");
            }

            // add friends to DB
            //db.insertRawUsers(friends);
            db.insertUser(userInfo);
            db.insertTags(userTags, user);

            // if find new region, add it to DB
            if (userInfo.getRegion() != null && !regions.contains(userInfo.getRegion())) {
                db.insertRegion(userInfo.getRegion());
                regions.add(userInfo.getRegion());
            }

            // if user's region don't suit us
            if (userInfo.getRegion() != null && !userInfo.getRegion().equals("RU")) {
                db.updateUserFetched(user);
                continue;
            }

            long countTagsNoAccess = 0;
            List<Tag> tagsNoAccess = new ArrayList<>();

            long countUserTags = userTags.size();
            long countUserTagsUses = 0;

            logger.info("Getting posts...");
            for (Tag tag : userTags) {
                logger.info("Tag: " + tag.getName() + " - " + (tag.getUses() != null ? tag.getUses() : 0) + " uses.");
                List<Post> posts = null;
                try {
                    posts = getTagPosts(user, tag);
                } catch (UnirestException | InterruptedException | ParseException | UnsupportedEncodingException e) {
                    logger.error("User: " + user + " " + e);
                }

                if (posts == null) {
                    countTagsNoAccess++;
                    tagsNoAccess.add(tag);
                    logger.warn("No access to user: " + user + " with tag: " + tag.getName());
                    continue;
                }

                db.insertPosts(posts);
                db.updateUserFetched(user);
                countUserTags += userTags.size();
                Integer tagUses = tag.getUses() != null ? tag.getUses() : 0;
                countUserTagsUses += tagUses;

                Integer lastUses = allTags.containsKey(tag.getName()) ? allTags.get(tag.getName()) : 0;
                allTags.put(tag.getName(), tagUses + lastUses);
            }
            logger.info("Count tags no access: " + countTagsNoAccess);
            if (countTagsNoAccess != 0) {
                logger.info("Tags no access:");
                for (Tag tagNoAccess : tagsNoAccess) {
                    logger.info(tagNoAccess.getName());
                }
            }
            logger.info("----------------------------------------");
            logger.info(user + " have tags: " + countUserTags);
            logger.info(user + " use tags: " + countUserTagsUses);

            logger.info(Arrays.toString(usersNoAccess.toArray()));
            logger.info("Count no access: " + countUsersNoAccess);
            logger.info("Count user with null region: " + countNullRegion);
            logger.info("Count user with null date created: " + countNullDateCreated);
            logger.info("Count user with null date updated: " + countNullDateUpdated);
            logger.info("Count user with null birthday: " + countNullBirthday);
            logger.info("Count user with null interests: " + countNullInterests);

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
            logger.info("Count users without tags: " + countUsersNoTags);
            if (countUsersNoTags != 0) {
                logger.info("Users without tags:");
                usersNoTags.forEach(logger::info);
            }

            logger.info("Count user no access: " + countUsersNoAccess);
            if (countUsersNoAccess != 0) {
                logger.info("Users no access:");
                usersNoAccess.forEach(logger::info);
                if (retryResponse && tries > 0) {
                    usersQueue.addAll(usersNoAccess);
                    --tries;
                }
            } else {
                retryResponse = false;
            }
        }
    }

    // get user's info
    private User getUserInfo(String nick) throws UnirestException, InterruptedException, UnsupportedEncodingException, IllegalArgumentException {

        Thread.sleep(200);  // delay
        HttpResponse<String> userInfoResponse = Unirest.get("http://users.livejournal.com/" +
                URLEncoder.encode(nick, "UTF-8") + "/data/foaf")
                .header("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3")
                .asString();


        Document doc = Jsoup.parse(userInfoResponse.getBody());

        Element person = doc.getElementsByTag(PERSON_SELECTOR).first();

        Element httpRegion = person.getElementsByTag(REGION_SELECTOR).first();
        String region = null;
        if (httpRegion != null && httpRegion.hasAttr(ATTR_TITLE_SELECTOR)) {
            region = httpRegion.attr(ATTR_TITLE_SELECTOR);
        }
        if (region == null) {
            countNullRegion++;
        }

        Element httpDateCreated = person.getElementsByTag(WEBLOG_SELECTOR).first();
        String dateCreated = httpDateCreated != null ? httpDateCreated.attr(DATE_CREATED_SELECTOR).replace("T", " ") : null;
        if (dateCreated == null) {
            countNullDateCreated++;
        }

        Element httpDateUpdated = person.getElementsByTag(WEBLOG_SELECTOR).first();
        String dateUpdated = httpDateUpdated != null ? httpDateUpdated.attr(DATE_LAST_UPDATED_SELECTOR).replace("T", " ") : null;
        if (dateUpdated == null) {
            countNullDateUpdated++;
        }

        Element httpBirthday = person.getElementsByTag(BIRTHDAY_SELECTOR).first();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", new Locale("en_US"));

        String birthday = null;
        if (httpBirthday != null) {
            birthday = httpBirthday.text();
            if (birthday.length() == 4) {
                birthday += "-01-01";
            }
            try {
                dateFormat.parse(birthday);
            } catch (ParseException | NullPointerException e) {
                logger.warn("User: " + nick + e);
                birthday = null;
            }
        }
        if (birthday == null) {
            countNullBirthday++;
        }

        Elements interests = person.getElementsByTag(INTEREST_SELECTOR);
        String interestsStr = !interests.isEmpty() ? "" : null;
        for (Element interest : interests) {
            interestsStr += interest.attr(ATTR_TITLE_SELECTOR) + ",";
        }
        if (interestsStr == null) {
            countNullInterests++;
        }

        return new User(nick, region,
                dateCreated != null ? Timestamp.valueOf(dateCreated) : null,
                dateUpdated != null ? Timestamp.valueOf(dateUpdated) : null,
                birthday != null ? Date.valueOf(birthday) : null,
                interestsStr);
    }

    // get all user's friends
    private LinkedList<String> getUserFriends(String user) throws UnirestException, InterruptedException, UnsupportedEncodingException {

        Thread.sleep(200);  // delay
        HttpResponse<String> userFriendsResponse = Unirest.get("http://www.livejournal.com/misc/fdata.bml?user=" + URLEncoder.encode(user, "UTF-8"))
                .header("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3")
                .asString();


        String friends = userFriendsResponse.getBody()
                .replaceAll("^[^\n]*\n", "")            // delete first line
                .replaceAll("[<>] ", "")                // delete separators < >
                .replaceAll("_", "-");                  // change _ to - for Unirest response

        String[] friendsArray = friends.split("\n");

        LinkedList<String> friendsList = new LinkedList<>();
        for (String friend : friendsArray) {
            if (allUsers.add(friend)) {
                friendsList.add(friend);
            }
        }
        return friendsList;
    }

    // get all user's tags
    private Set<Tag> getUserTags(String user) throws UnirestException, InterruptedException, UnsupportedEncodingException {

        Thread.sleep(200);  // delay
        HttpResponse<String> userTagsResponse = Unirest.get("http://users.livejournal.com/" + URLEncoder.encode(user, "UTF-8") + "/tag/")
                .header("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3")
                .asString();

        Document doc = Jsoup.parse(userTagsResponse.getBody());

        // find all tags on page
        Elements httpTags = doc.select("a[href^=http://" + user + ".livejournal.com/tag");

        if (httpTags.size() == 0) {
            httpTags = doc.select("a[href^=http://users.livejournal.com/" + user.replaceAll("-", "_") + "/tag");
        }

        String regex = "\\d+";    // the number
        Pattern pattern = Pattern.compile(regex);
        Set<Tag> tagSet = new HashSet<>();

        for (Element httpTag : httpTags) {
            String tagText = httpTag.text();

            // delete unused attributes
            httpTag = httpTag.attr("href", "")
                    .attr("style", "")
                    .attr("target", "")
                    .attr("rel", "")
                    .attr("class", "")
                    .text("");

            String strUses = "";

            // find count of uses into reference
            Matcher matcher = pattern.matcher(httpTag.outerHtml());
            if (matcher.find()) {
                strUses = matcher.group();
            } else {
                // find count of uses into parent
                matcher = pattern.matcher(httpTag.parent().outerHtml());
                if (matcher.find()) {
                    strUses = matcher.group();
                }
            }

            Integer tagUses = null;
            // if don't find uses or find irregular result
            if (!strUses.isEmpty() && strUses.length() < MAX_TAG_USES_POW) {
                tagUses = Integer.parseInt(strUses);
            }
            Tag tag = new Tag(tagText, tagUses);
            if (tagSet.contains(tag) && tagUses != null) {
                tagSet.remove(tag);
            }
            tagSet.add(tag);
        }
        return tagSet;
    }

    // get 25 posts by current tag
    private List<Post> getTagPosts(final String user, final Tag tag) throws UnirestException, InterruptedException, UnsupportedEncodingException, ParseException {

        Thread.sleep(200);  // delay
        HttpResponse<String> tagPostResponse = Unirest.get("http://users.livejournal.com/" +
                URLEncoder.encode(user, "UTF-8") + "/data/rss/?tag=" + URLEncoder.encode(tag.getName(), "UTF-8"))
                .header("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3")
                .asString();

        Document doc = Jsoup.parse(tagPostResponse.getBody());

        Elements selectionPosts = doc.select(POST_SELECTOR);

        List<Post> postList = new ArrayList<>();
        for (Element selectionPost : selectionPosts) {
            Elements title = selectionPost.select(TITLE_SELECTOR);
            Elements text = selectionPost.select(TEXT_SELECTOR);
            Elements date = selectionPost.select(DATE_SELECTOR);
            Elements url = selectionPost.select(URL_SELECTOR);
            Elements postTags = selectionPost.select(TAG_SELECTOR);
            Elements comments = selectionPost.getElementsByTag(COUNT_COMMENTS_SELECTOR);

            String safeText = Jsoup.clean(text.text(), Whitelist.none());
            List<String> tagsList = postTags.stream().map(Element::text).collect(Collectors.toList());

            String regex = "/\\d+";    // the number
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(url.text());
            Integer urlNumber = null;
            if (matcher.find()) {
                urlNumber = Integer.parseInt(matcher.group().replaceAll("/", ""));
            }
            Integer countComments = !Objects.equals(comments.text(), "") ? Integer.parseInt(comments.text()) : null;

            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", new Locale("en_US"));
            Timestamp timePost = new Timestamp(dateFormat.parse(date.text()).getTime());
            postList.add(new Post(
                    title.text(), safeText, user,
                    timePost, urlNumber,
                    countComments, tagsList
            ));
        }
        return postList;
    }
}
