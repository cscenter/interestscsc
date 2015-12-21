package com.interestscsc.crawler.parsers;

import com.interestscsc.crawler.Crawler;
import com.interestscsc.data.Post;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TagPostParser {
    // Post's selectors
    private static final String POST_SELECTOR = "item";
    private static final String TITLE_SELECTOR = "title";
    private static final String TEXT_SELECTOR = "description";
    private static final String DATE_SELECTOR = "pubDate";
    private static final String URL_SELECTOR = "guid";
    private static final String TAG_SELECTOR = "category";
    private static final String COUNT_COMMENTS_SELECTOR = "lj:reply-count";

    public static List<Post> getPosts(String response, String nick) throws InterruptedException, ParseException {
        Document doc = Jsoup.parse(response);

        Elements selectionPosts = doc.select(POST_SELECTOR);

        List<Post> postList = new ArrayList<>();
        for (Element selectionPost : selectionPosts) {
            Elements title = selectionPost.select(TITLE_SELECTOR);
            Elements text = selectionPost.select(TEXT_SELECTOR);
            Elements date = selectionPost.select(DATE_SELECTOR);
            Elements url = selectionPost.select(URL_SELECTOR);
            Elements postTags = selectionPost.select(TAG_SELECTOR);
            Elements comments = selectionPost.getElementsByTag(COUNT_COMMENTS_SELECTOR);

            String safeText = Jsoup.clean(text.text().replaceAll("<", " <"), Whitelist.none());
            List<String> tagsList = postTags.stream().map(Element::text).map(String::toLowerCase).collect(Collectors.toList());

            String regex = "/\\d+";    // the number
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(url.text());
            Long urlNumber = null;
            if (matcher.find()) {
                urlNumber = Long.parseLong(matcher.group().replaceAll("/", ""));
            }
            Integer countComments = !Objects.equals(comments.text(), "") ? Integer.parseInt(comments.text()) : null;

            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", new Locale("en_US"));
            Timestamp timePost = new Timestamp(dateFormat.parse(date.text()).getTime());
            if (Crawler.userPostUrls.add(urlNumber)) {
                postList.add(new Post(
                        title.text(), safeText, nick,
                        timePost, urlNumber,
                        countComments, tagsList
                ));
            }
        }
        return postList;

    }
}
