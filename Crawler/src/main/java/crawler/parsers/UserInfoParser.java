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
import java.util.Locale;

public class UserInfoParser {
    // User's selectors
    private static final String PERSON_SELECTOR = "foaf:Person";
    private static final String REGION_SELECTOR = "ya:country";
    private static final String WEBLOG_SELECTOR = "foaf:weblog";
    private static final String DATE_CREATED_SELECTOR = "lj:dateCreated";
    private static final String DATE_LAST_UPDATED_SELECTOR = "lj:dateLastUpdated";
    private static final String BIRTHDAY_SELECTOR = "foaf:dateOfBirth";
    private static final String INTEREST_SELECTOR = "foaf:interest";
    private static final String ATTR_TITLE_SELECTOR = "dc:title";

    private static final Logger logger = Logger.getLogger(UserInfoParser.class);
    private static int countNullRegion = 0;
    private static int countNullDateCreated = 0;
    private static int countNullDateUpdated = 0;
    private static int countNullBirthday = 0;
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
        for (Element interest: interests) {
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

    // logging information about the number of users without any data(region, date of creation, etc)
    public static void logStatistics() {
        logger.info("Count user with null region: " + countNullRegion);
        logger.info("Count user with null date created: " + countNullDateCreated);
        logger.info("Count user with null date updated: " + countNullDateUpdated);
        logger.info("Count user with null birthday: " + countNullBirthday);
        logger.info("Count user with null interests: " + countNullInterests);
    }
}
