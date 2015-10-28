package crawler;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URLEncoder;
import java.sql.Date;
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
    private static final String URL_SELECTOR = "comments";
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
    private final String PATH_TO_FILE_WITH_TAGS = "Crawler" + File.separator + "src" + File.separator +
            "main" + File.separator + "resources" + File.separator + "tags" + File.separator;

    private final String PATH_TO_FILE_WITH_USERS = "Crawler" + File.separator + "src" + File.separator +
            "main" + File.separator + "resources" + File.separator + "users" + File.separator;

    // set of all users
    private Set<String> allUsers;

    // queue of users who should be considered
    private LinkedList<String> usersQueue;

    // dictionary where key=tags' name; value=count uses of this tags
    private Map<String, Integer> allTags;

    public Crawler() {
        allUsers = new HashSet<>();
        usersQueue = new LinkedList<>();
        allTags = new HashMap<>();
    }

    // @TODO add logger to handle exceptions
    public void crawl(String startUser) {

        startUser = startUser.replaceAll("_", "-");
        allUsers.add(startUser);
        usersQueue.add(startUser);

        // get users without tags
        long countUsersNoTags = 0;
        List<String> usersNoTags = new ArrayList<>();

        long countUsersNoAccess = 0;
        List<String> usersNoAccess = new ArrayList<>();

        // for all users
        while (!usersQueue.isEmpty()) {

            String user = usersQueue.poll();
            System.out.println(user);
            System.out.println(usersQueue.size() + " / " + allUsers.size());
            Set<Tag> userTags = null;
            User userInfo = null;
            try {

                getUserFriends(user);
                userInfo = getUserInfo(user);
                userTags = getUserTags(user);

            } catch (UnirestException e) {
                System.out.println("I don't know why you see this exception.");
                e.printStackTrace();
            } catch (InterruptedException | UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            if (userInfo == null) {
                countUsersNoAccess++;
                usersNoAccess.add(user);
                System.out.println("No access to user: " + user);
                continue;
            }

            if (userTags == null) {
                countUsersNoAccess++;
                usersNoAccess.add(user);
                continue;
            }

            if (userTags.isEmpty()) {
                countUsersNoTags++;
                usersNoTags.add(user);
            }

            long countTagsNoAccess = 0;
            List<Tag> tagsNoAccess = new ArrayList<>();

            long countUserTags = userTags.size();
            long countUserTagsUses = 0;
            BufferedWriter bufferedWriterTags = null;
            try {
                File fileTags = new File(PATH_TO_FILE_WITH_USERS + user + ".txt");
                if (!fileTags.exists()) {
                    fileTags.getParentFile().mkdir();
                    fileTags.createNewFile();
                }
                FileWriter fileWriterTags = new FileWriter(fileTags.getAbsoluteFile());
                bufferedWriterTags = new BufferedWriter(fileWriterTags);
                bufferedWriterTags.write(userInfo.writeToFile());
                bufferedWriterTags.write("----------------------\nTags:\n");

                for (Tag tag : userTags) {
                    List<Post> posts = null;
                    try {
                        posts = getTagPosts(user, tag);
                    } catch (UnirestException | InterruptedException | ParseException e) {
                        e.printStackTrace();
                    }

                    if (posts == null) {
                        countTagsNoAccess++;
                        tagsNoAccess.add(tag);
                        continue;
                    }

                    BufferedWriter bufferedWriterPosts = null;
                    try {
                        File filePosts = new File(PATH_TO_FILE_WITH_TAGS + tag.getName() + ".txt");
                        if (!filePosts.exists()) {
                            filePosts.getParentFile().mkdir();
                            filePosts.createNewFile();
                        }
                        FileWriter fileWriterPosts = new FileWriter(filePosts.getAbsoluteFile(), true);
                        bufferedWriterPosts = new BufferedWriter(fileWriterPosts);
                        for (Post post : posts) {
                            bufferedWriterPosts.write(post.writeToFile());
                            bufferedWriterPosts.write("-\n");
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println("Invalid name of tag: " + tag.getName());
                    } finally {
                        if (bufferedWriterPosts != null) {
                            try {
                                bufferedWriterPosts.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    Integer tagUses = tag.getUses() != null ? tag.getUses() : 0;
                    countUserTagsUses += tagUses;
                    bufferedWriterTags.write(tag.writeToFile());

                    Integer lastUses = allTags.containsKey(tag.getName()) ? allTags.get(tag.getName()) : 0;
                    allTags.put(tag.getName(), tagUses + lastUses);
                }

            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Invalid name of user: " + user);
            } finally {
                // @TODO save this to file (serialization)
                System.out.println("Count tags no access: " + countTagsNoAccess);
                if (countTagsNoAccess != 0) {
                    System.out.println("Tags no access:");
                    for (Tag tagNoAccess : tagsNoAccess) {
                        System.out.println(tagNoAccess.getName());
                    }
                }
                System.out.println("----------------------------------------");
                System.out.println(user + " have tags: " + countUserTags);
                System.out.println(user + " use tags: " + countUserTagsUses);
                System.out.println();
                if (bufferedWriterTags != null) {
                    try {
                        bufferedWriterTags.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        long countTags = allTags.keySet().size();
        long countTagsUses = 0;
        BufferedWriter bufferedWriter = null;
        try {

            File file = new File(PATH_TO_FILE_WITH_TAGS + "tags.txt");
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
            bufferedWriter = new BufferedWriter(fileWriter);

            System.out.println("++++++++++++++++++++++++++++++++++++++++++");
            System.out.println("ALL TAGS:");


            for (Map.Entry<String, Integer> tag : allTags.entrySet()) {
                countTagsUses += tag.getValue();
                bufferedWriter.write(tag.getKey() + " " + tag.getValue() + "\n");

                System.out.println(tag.getKey() + " : " + tag.getValue() + " uses");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            System.out.println();
            System.out.println("=============================================");
            System.out.println("Count tags: " + countTags);
            System.out.println("Count tags uses: " + countTagsUses);
            System.out.println("Count users without tags: " + countUsersNoTags);
            if (countUsersNoTags != 0) {
                System.out.println("Users without tags:");
                usersNoTags.forEach(System.out::println);
            }

            System.out.println("Count user no access: " + countUsersNoAccess);
            if (countUsersNoAccess != 0) {
                System.out.println("Users no access:");
                usersNoAccess.forEach(System.out::println);
            }
            if (bufferedWriter != null) {
                try {
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // get user's info
    private User getUserInfo(String nick)  throws UnirestException, InterruptedException, UnsupportedEncodingException, IllegalArgumentException {

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

        Element httpDateCreated = person.getElementsByTag(WEBLOG_SELECTOR).first();
        String dateCreated = httpDateCreated != null ? httpDateCreated.attr(DATE_CREATED_SELECTOR).replace("T", " ") : null;

        Element httpDateUpdated = person.getElementsByTag(WEBLOG_SELECTOR).first();
        String dateUpdated = httpDateUpdated != null ? httpDateUpdated.attr(DATE_LAST_UPDATED_SELECTOR).replace("T", " ") : null;

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
                birthday = null;
            }
        }

        Elements interests = person.getElementsByTag(INTEREST_SELECTOR);
        String interestsStr = !interests.isEmpty() ? "" : null;
        for (Element interest: interests) {
            interestsStr += interest.attr(ATTR_TITLE_SELECTOR) + ",";
        }

        return new User(nick, region,
                dateCreated != null ? Timestamp.valueOf(dateCreated) : null,
                dateUpdated != null ? Timestamp.valueOf(dateUpdated) : null,
                birthday != null ? Date.valueOf(birthday) : null,
                interestsStr);
    }

    // get all user's friends
    private void getUserFriends(String user) throws UnirestException, InterruptedException, UnsupportedEncodingException {

        Thread.sleep(200);  // delay
        HttpResponse<String> userFriendsResponse = Unirest.get("http://www.livejournal.com/misc/fdata.bml?user=" + URLEncoder.encode(user, "UTF-8"))
                .header("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3")
                .asString();


        String friends = userFriendsResponse.getBody()
                .replaceAll("^[^\n]*\n", "")            // delete first line
                .replaceAll("[<>] ", "")                // delete separators < >
                .replaceAll("_", "-");                  // change _ to - for Unirest response

        String[] friendsArray = friends.split("\n");

        for (String friend : friendsArray) {
            if (allUsers.add(friend)) {
                usersQueue.add(friend);
            }
        }
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
