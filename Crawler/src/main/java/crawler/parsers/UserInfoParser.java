package crawler.parsers;


import data.User;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class UserInfoParser {
    // User's selectors
    private static final String PERSON_SELECTOR = "foaf:Person";
    private static final String REGION_SELECTOR = "ya:country";
    private static final String CITY_SELECTOR = "ya:city";
    private static final String POST_COUNT_SELECTOR = "ya:Posts";
    private static final String COMMENTS_COUNT_SELECTOR = "ya:Comments";
    private static final String ATTR_POSTED_SELECTOR = "ya:posted";
    private static final String ATTR_RECEIVED_SELECTOR = "ya:received";
    private static final String WEBLOG_SELECTOR = "foaf:weblog";
    private static final String DATE_CREATED_SELECTOR = "lj:dateCreated";
    private static final String DATE_LAST_UPDATED_SELECTOR = "lj:dateLastUpdated";
    private static final String BIRTHDAY_SELECTOR = "foaf:dateOfBirth";
    private static final String BIO_SELECTOR = "ya:bio";
    private static final String SCHOOL_SELECTOR = "ya:school";
    private static final String ATTR_DATE_START_SCHOOL_SELECTOR = "ya:dateStart";
    private static final String ATTR_DATE_FINISH_SCHOOL_SELECTOR = "ya:dateFinish";
    private static final String INTEREST_SELECTOR = "foaf:interest";
    private static final String ATTR_TITLE_SELECTOR = "dc:title";

    private static final Logger logger = Logger.getLogger(UserInfoParser.class);
    private static int countNullRegion = 0;
    private static int countNullCity = 0;
    private static int countNullPostPosted = 0;
    private static int countNullCommentsPosted = 0;
    private static int countNullCommentsReceived = 0;
    private static int countNullDateCreated = 0;
    private static int countNullDateUpdated = 0;
    private static int countNullBirthday = 0;
    private static int countNullBio = 0;
    private static int countNullSchool = 0;
    private static int countNullSchoolDateStart = 0;
    private static int countNullSchoolDateFinish = 0;
    private static int countNullInterests = 0;

    public static User getUserInfo(String response, String nick) {
        Document doc = Jsoup.parse(response);

        Element person = doc.getElementsByTag(PERSON_SELECTOR).first();

        Element httpRegion = person.getElementsByTag(REGION_SELECTOR).first();
        String region = null;
        if (httpRegion != null && httpRegion.hasAttr(ATTR_TITLE_SELECTOR)) {
            region = httpRegion.attr(ATTR_TITLE_SELECTOR);
        }
        if (region == null) {
            countNullRegion++;
        }

        Element httpCity = person.getElementsByTag(CITY_SELECTOR).first();
        String city = null;
        if (httpCity != null && httpCity.hasAttr(ATTR_TITLE_SELECTOR)) {
            city = httpCity.attr(ATTR_TITLE_SELECTOR);
        }
        if (city == null) {
            countNullCity++;
        }

        Element httpPostsCount = person.getElementsByTag(POST_COUNT_SELECTOR).first();
        Integer postPosted = null;
        if (httpPostsCount != null && httpPostsCount.hasAttr(ATTR_POSTED_SELECTOR)) {
            postPosted = Integer.parseInt(httpPostsCount.attr(ATTR_POSTED_SELECTOR));
        }
        if (postPosted == null) {
            countNullPostPosted++;
        }

        Element httpCommentsCount = person.getElementsByTag(COMMENTS_COUNT_SELECTOR).first();
        Integer commentPosted = null;
        if (httpCommentsCount != null && httpCommentsCount.hasAttr(ATTR_POSTED_SELECTOR)) {
            commentPosted = Integer.parseInt(httpCommentsCount.attr(ATTR_POSTED_SELECTOR));
        }
        if (commentPosted == null) {
            countNullCommentsPosted++;
        }
        Integer commentReceived = null;
        if (httpCommentsCount != null && httpCommentsCount.hasAttr(ATTR_RECEIVED_SELECTOR)) {
            commentReceived = Integer.parseInt(httpCommentsCount.attr(ATTR_RECEIVED_SELECTOR));
        }
        if (commentReceived == null) {
            countNullCommentsReceived++;
        }

        Element httpDateCreated = person.getElementsByTag(WEBLOG_SELECTOR).first();
        String dateCreated = null;
        if (httpDateCreated != null && httpDateCreated.hasAttr(DATE_CREATED_SELECTOR)) {
            dateCreated = httpDateCreated.attr(DATE_CREATED_SELECTOR).replace("T", " ");
        }
        if (dateCreated == null) {
            countNullDateCreated++;
        }

        Element httpDateUpdated = person.getElementsByTag(WEBLOG_SELECTOR).first();
        String dateUpdated = null;
        if (httpDateUpdated != null && httpDateUpdated.hasAttr(DATE_LAST_UPDATED_SELECTOR)) {
            dateUpdated = httpDateUpdated.attr(DATE_LAST_UPDATED_SELECTOR).replace("T", " ");
        }
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

        Element httpBio = person.getElementsByTag(BIO_SELECTOR).first();
        String bio = null;
        if (httpBio != null) {
            bio = httpBio.text();
        }
        if (bio == null) {
            countNullBio++;
        }

        Elements httpSchools = person.getElementsByTag(SCHOOL_SELECTOR);
        List<User.School> schools = new LinkedList<>();
        for (Element httpSchool : httpSchools) {
            String title = null;
            if (httpSchool != null && httpSchool.hasAttr(ATTR_TITLE_SELECTOR)) {
                title = httpSchool.attr(ATTR_TITLE_SELECTOR);
            }
            if (title == null) {
                countNullSchool++;
            }

            String dateStart = null;
            if (httpSchool != null && httpSchool.hasAttr(ATTR_DATE_START_SCHOOL_SELECTOR)) {
                dateStart = httpSchool.attr(ATTR_DATE_START_SCHOOL_SELECTOR);
                dateStart += "-01-01";
            }
            if (dateStart == null) {
                countNullSchoolDateStart++;
            }

            String dateFinish = null;
            if (httpSchool != null && httpSchool.hasAttr(ATTR_DATE_FINISH_SCHOOL_SELECTOR)) {
                dateFinish = httpSchool.attr(ATTR_DATE_FINISH_SCHOOL_SELECTOR);
                dateFinish += "-01-01";
            }
            if (dateFinish == null) {
                countNullSchoolDateFinish++;
            }

            schools.add(new User.School(
                    title,
                    dateStart != null ? Date.valueOf(dateStart) : null,
                    dateFinish != null ? Date.valueOf(dateFinish) : null));
        }

        Elements interests = person.getElementsByTag(INTEREST_SELECTOR);
        String interestsStr = !interests.isEmpty() ? "" : null;
        for (Element interest: interests) {
            interestsStr += interest.attr(ATTR_TITLE_SELECTOR) + ",";
        }
        if (interestsStr == null) {
            countNullInterests++;
        }

        return new User(null, nick, region,
                dateCreated != null ? Timestamp.valueOf(dateCreated) : null,
                dateUpdated != null ? Timestamp.valueOf(dateUpdated) : null,
                Timestamp.valueOf(LocalDateTime.now()),
                birthday != null ? Date.valueOf(birthday) : null,
                interestsStr, city, postPosted,
                commentPosted, commentReceived,
                bio, schools);
    }

    // logging information about the number of users without any data(region, date of creation, etc)
    public static void logStatistics() {
        logger.info("Count user with null region: " + countNullRegion);
        logger.info("Count user with null city: " + countNullCity);
        logger.info("Count user with null posts' number: " + countNullPostPosted);
        logger.info("Count user with null comments posted: " + countNullCommentsPosted);
        logger.info("Count user with null comments received: " + countNullCommentsReceived);
        logger.info("Count user with null date created: " + countNullDateCreated);
        logger.info("Count user with null date updated: " + countNullDateUpdated);
        logger.info("Count user with null birthday: " + countNullBirthday);
        logger.info("Count user with null bio: " + countNullBio);
        logger.info("Count user with null school: " + countNullSchool);
        logger.info("Count user with null school's date start: " + countNullSchoolDateStart);
        logger.info("Count user with null school's date finish: " + countNullSchoolDateFinish);
        logger.info("Count user with null interests: " + countNullInterests);
    }
}
