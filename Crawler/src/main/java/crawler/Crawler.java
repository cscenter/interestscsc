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

    private final int MAX_TAG_USES_POW = 6; // max uses count 1e6
    private final String PATH_TO_FILE_WITH_TAGS = "Crawler" + File.separator + "src" + File.separator +
            "main" + File.separator + "resources" + File.separator;

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
            List<Tag> userTags = null;
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

            BufferedWriter bufferedWriter = null;
            try {
                File file = new File(PATH_TO_FILE_WITH_USERS + user + ".txt");
                if (!file.exists()) {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
                FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
                bufferedWriter = new BufferedWriter(fileWriter);

                System.out.println(user);
                System.out.println(usersQueue.size() + " / " + allUsers.size());
                System.out.println("Tags:");

                Integer countUserTags = userTags.size();
                Integer countUserTagsUses = 0;
                for (Tag tag : userTags) {
                    countUserTagsUses += tag.getUses();
                    bufferedWriter.write(tag.getTag() + " : " + tag.getUses() + "\n");

                    System.out.println(tag.getTag() + " : " + tag.getUses() + " uses");

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

                if (bufferedWriter != null) {
                    try {
                        bufferedWriter.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        try {

            File file = new File(PATH_TO_FILE_WITH_TAGS + "tags.txt");
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fileWriter = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

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

            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
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
    private List<Tag> getUserTags(String user) throws UnirestException, InterruptedException {

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
        List<Tag> tagList = new ArrayList<Tag>();

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
                tagList.add(new Tag(tagText, tagUses));
            }
        }

        return tagList;
    }
}
