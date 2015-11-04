package crawler;

import java.sql.Timestamp;
import java.util.List;

public class Post {

    private String title;
    private String text;
    private String author;
    private Timestamp date;
    private Integer url;
    private Integer countComment;
    private List<String> tags;

    public Post(
            final String title, final String text, final String author,
            final Timestamp date, final Integer url, final Integer countComment,
            final List<String> tags
    ) {
        this.title = title;
        this.text = text;
        this.author = author;
        this.date = date;
        this.url = url;
        this.countComment = countComment;
        this.tags = tags;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public String getAuthor() {
        return author;
    }

    public Timestamp getDate() {
        return date;
    }

    public Integer getUrl() {
        return url;
    }

    public Integer getCountComment() {
        return countComment;
    }

    public List<String> getTags() {
        return tags;
    }

    public String writeToFile() {
        return "title : " + title + "\n" +
                "text : " + text + "\n" +
                "author : " + author + "\n" +
                "date : " + date + "\n" +
                "url : " + url + "\n" +
                "comments : " + countComment + "\n" +
                "tags : " + tags.toString() + "\n";
    }

    @Override
    public String toString() {
        return title + text;
    }
}
