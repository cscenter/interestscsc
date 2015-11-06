package crawler.parsers;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Set;

public class UserFriendsParser {
    public static LinkedList<String> getFriends(String response) {
        String friends = response
                .replaceAll("^[^\n]*\n", "")            // delete first line
                .replaceAll("[<>] ", "")                // delete separators < >
                .replaceAll("_", "-");                  // change _ to - for Unirest response

        String[] friendsArray = friends.split("\n");

        LinkedList<String> friendsList = new LinkedList<>();
        Collections.addAll(friendsList, friendsArray);
        return friendsList;
    }
}
