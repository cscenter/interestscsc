package com.interestscsc.crawler.parsers;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class UserFriendsParser {
    public static List<String> getFriends(String response) {
        String friends = response
                .replaceAll("^[^\n]*\n", "")            // delete first line
                .replaceAll("[<>] ", "")                // delete separators < >
                .replaceAll("_", "-");                  // change _ to - for Unirest response

        String[] friendsArray = friends.split("\n");

        List<String> friendsList = new LinkedList<>();
        Collections.addAll(friendsList, friendsArray);
        return friendsList;
    }
}
