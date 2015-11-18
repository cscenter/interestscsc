package crawler.parsers;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class UserRobotsParser {
    public static Set<String> getDisallowPages(String response) {
        String anyUserAgent = response
                .replaceAll("Disallow: ", "");

        String[] rawDisallow = anyUserAgent.split("User-Agent: \\*");

        final int SECOND_PART_AFTER_SPLIT = 1;
        Set<String> disallow = new HashSet<>();
        if (rawDisallow.length > SECOND_PART_AFTER_SPLIT) {
            String[] userAgentDisallow = rawDisallow[SECOND_PART_AFTER_SPLIT].split("\n");
            Collections.addAll(disallow, userAgentDisallow);
        }
        return disallow;
    }
}

