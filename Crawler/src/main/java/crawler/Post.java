package crawler;

import java.util.ArrayList;
import java.util.List;

public class Post {

    private String title;
    private String text;
    private String date;
    private String url;
    private List<String> tags;

    public Post() {
        this.title = "";
        this.text = "";
        this.date = "";
        this.url = "";
        this.tags = new ArrayList<String>();
    }

    public Post(final String title, final String text, final String date, final String url, final List<String> tags) {
        this.title = title;
        this.text = text;
        this.date = date;
        this.url = url;
        this.tags = tags;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public String getDate() {
        return date;
    }

    public void setDate(final String date) {
        this.date = date;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTag(final String tag) {
        this.tags.add(tag);
    }

    public String writeToFile() {
        return "title : " + title + "\n" +
                "text : " + text + "\n" +
                "date : " + date + "\n" +
                "url : " + url + "\n" +
                "tags : " + tags.toString() + "\n";
    }

    @Override
    public String toString() {
        return title + text;
    }
}
