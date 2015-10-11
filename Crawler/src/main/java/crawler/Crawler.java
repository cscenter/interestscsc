package crawler;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Crawler {

    // selectors
    private static final String POST_SELECTOR = "item";
    private static final String TITLE_SELECTOR = "title";
    private static final String TEXT_SELECTOR = "description";
    private static final String DATE_SELECTOR = "pubDate";
    private static final String URL_SELECTOR = "comments";
    private static final String TAG_SELECTOR = "category";

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
        allUsers = new HashSet<String>();
        usersQueue = new LinkedList<String>();
        allTags = new HashMap<String, Integer>();
    }

    // @TODO add logger to handle exceptions
    public void crawl(String startUser) {

        allUsers.add(startUser);
        usersQueue.add(startUser);

        // get users without tags
        Integer countUsersNoTags = 0;
        List<String> usersNoTags = new ArrayList<String>();

        // for all users
        while (!usersQueue.isEmpty()) {

            String user = usersQueue.poll();
            Set<Tag> userTags = null;
            try {

                getUserFriends(user);
                userTags = getUserTags(user);

            } catch (UnirestException e) {
                System.out.println("I don't know why you see this exception.");
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (userTags.isEmpty()) {
                countUsersNoTags++;
                usersNoTags.add(user);
            }

            BufferedWriter bufferedWriterTags = null;
            try {
                File fileTags = new File(PATH_TO_FILE_WITH_USERS + user + ".txt");
                if (!fileTags.exists()) {
                    fileTags.getParentFile().mkdir();
                    fileTags.createNewFile();
                }
                FileWriter fileWriterTags = new FileWriter(fileTags.getAbsoluteFile());
                bufferedWriterTags = new BufferedWriter(fileWriterTags);

                System.out.println(user);
                System.out.println(usersQueue.size() + " / " + allUsers.size());
                System.out.println("Tags:");

                Integer countUserTags = userTags.size();
                Integer countUserTagsUses = 0;
                for (Tag tag : userTags) {
                    List<Post> posts = null;
                    try {
                        posts = getTagPosts(user, tag);
                    } catch (UnirestException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    BufferedWriter bufferedWriterPosts = null;
                    try {
                        File filePosts = new File(PATH_TO_FILE_WITH_TAGS + tag.getTag() + ".txt");
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
                    } finally {
                        if (bufferedWriterPosts != null) {
                            try {
                                bufferedWriterPosts.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    countUserTagsUses += tag.getUses();
                    bufferedWriterTags.write(tag.writeToFile());

                    System.out.print(tag.writeToFile());

                    Integer lastUses = allTags.containsKey(tag.getTag()) ? allTags.get(tag.getTag()) : 0;
                    allTags.put(tag.getTag(), tag.getUses() + lastUses);
                }

                System.out.println("----------------------------------------");
                System.out.println(user + " have tags: " + countUserTags);
                System.out.println(user + " use tags: " + countUserTagsUses);
                System.out.println();

            } catch (IOException e) {
                e.printStackTrace();
            } finally {

                if (bufferedWriterTags != null) {
                    try {
                        bufferedWriterTags.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

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

            Integer countTags = allTags.keySet().size();
            Integer countTagsUses = 0;
            for (Map.Entry<String, Integer> tag : allTags.entrySet()) {
                countTags += tag.getValue();
                bufferedWriter.write(tag.getKey() + " " + tag.getValue() + "\n");

                System.out.println(tag.getKey() + " : " + tag.getValue() + " uses");
            }

            System.out.println();
            System.out.println("=============================================");
            System.out.println("Count tags: " + countTags);
            System.out.println("Count tags uses: " + countTagsUses);
            System.out.println("Count user without tags: " + countUsersNoTags);
            for (String userNoTags : usersNoTags) {
                System.out.println(userNoTags);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedWriter != null) {
                try {
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // get all user's friends
    private void getUserFriends(String user) throws UnirestException, InterruptedException {

        HttpResponse<String> userFriendsResponse = Unirest.get("http://www.livejournal.com/misc/fdata.bml?user={user_name}")
                .routeParam("user_name", user)
                .header("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3")
                .asString();
        Thread.sleep(200);  // delay


        String friends = userFriendsResponse.getBody()
                .replaceAll("^[^\n]*\n", "")            // delete first line
                .replaceAll("[<>] ", "")                // delete separators < >
                .replaceAll("_", "-")                   // change _ to - for Unirest response
                .replaceAll("\n.*-\n", "\n")            // delete users with name like "user_", "user-"
                .replaceAll("\n-.*\n", "\n");           // delete users with name like "_user", "-user"

        String[] friendsArray = friends.split("\n");

        for (String friend : friendsArray) {
            if (allUsers.add(friend)) {
                usersQueue.add(friend);
            }
        }
    }

    // get all user's tags
    private Set<Tag> getUserTags(String user) throws UnirestException, InterruptedException {

        HttpResponse<String> userTagsResponse = Unirest.get("http://{user_name}.livejournal.com/tag")
                .routeParam("user_name", user)
                .header("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3")
                .asString();
        Thread.sleep(1000);  // delay

        Document doc = Jsoup.parse(userTagsResponse.getBody());

        // find all tags on page
        Elements httpTags = doc.select("a[href^=http://" + user + ".livejournal.com/tag");

        String regex = "\\d+";    // the number
        Pattern pattern = Pattern.compile(regex);
        Set<Tag> tagSet = new HashSet<Tag>();

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

            // if don't find uses or find irregular result
            if (!strUses.isEmpty() && strUses.length() < MAX_TAG_USES_POW) {
                Integer tagUses = Integer.parseInt(strUses);
                tagSet.add(new Tag(tagText, tagUses));
            }
        }

        return tagSet;
    }

    // get 25 posts by current tag
    private List<Post> getTagPosts(final String user, final Tag tag) throws UnirestException, InterruptedException {

        HttpResponse<String> liveJournalResponse = Unirest.get("http://" + user + ".livejournal.com/data/rss/?tag=" + tag.getTag().replaceAll(" ", "%20"))
                .header("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3")
                .asString();
        Thread.sleep(200);  // delay

        Document doc = Jsoup.parse(liveJournalResponse.getBody());

        Elements selectionPosts = doc.select(POST_SELECTOR);

        List<Post> postList = new ArrayList<Post>();
        for (Element selectionPost : selectionPosts) {
            Elements title = selectionPost.select(TITLE_SELECTOR);
            Elements text = selectionPost.select(TEXT_SELECTOR);
            Elements date = selectionPost.select(DATE_SELECTOR);
            Elements url = selectionPost.select(URL_SELECTOR);
            Elements postTags = selectionPost.select(TAG_SELECTOR);

            List<String> tagsList = new ArrayList<String>();
            for (Element postTag : postTags) {
                tagsList.add(postTag.text());
            }

            postList.add(new Post(title.text(), text.text(), date.text(), url.text(), tagsList));
        }
        return postList;
    }
}
