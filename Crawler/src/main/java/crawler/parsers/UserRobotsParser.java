package crawler.parsers;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class UserRobotsParser {
    public static Set<String> getDisallowPages(String response) {
        String anyUserAgent = response
                .replaceAll("Disallow: ", "");

        String[] rawDisallow = anyUserAgent.split("User-Agent: \\*");

        Set<String> disallow = new HashSet<>();
        if (rawDisallow.length == 2) {
            String[] userAgentDisallow = rawDisallow[1].split("\n");
            Collections.addAll(disallow, userAgentDisallow);
        }
        return disallow;
    }
}

