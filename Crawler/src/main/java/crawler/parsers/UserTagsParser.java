package crawler.parsers;

import data.Tag;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserTagsParser {

    private static final int MAX_TAG_USES_POW = 6; // max uses count 1e6

    public static Set<Tag> getTags(String response, String nick) {
        Document doc = Jsoup.parse(response);

        // find all tags on page
        Elements httpTags = doc.select("a[href^=http://" + nick + ".livejournal.com/tag");

        if (httpTags.size() == 0) {
            httpTags = doc.select("a[href^=http://users.livejournal.com/" + nick.replaceAll("-", "_") + "/tag");
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
            Tag tag = new Tag(tagText.toLowerCase(), tagUses);
            if (tagSet.contains(tag) && tagUses != null) {
                tagSet.remove(tag);
            }
            tagSet.add(tag);
        }
        return tagSet;
    }
}
