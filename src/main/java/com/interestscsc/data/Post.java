package com.interestscsc.data;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;

public class Post {

    private Long id;
    private String title;
    private String text;
    private String author;
    private Timestamp date;
    private Long url;
    private Integer countComment;
    private List<String> tags;

    public Post(
            final String title, final String text, final String author,
            final Timestamp date, final Long url, final Integer countComment,
            final List<String> tags
    ) {
        if(tags == null)
            throw new IllegalArgumentException("List of tags can't be null");
        this.title = title;
        this.text = text;
        this.author = author;
        this.date = date;
        this.url = url;
        this.countComment = countComment;
        this.tags = tags;
    }

    public Post(Long id, String title, String text) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.tags = new LinkedList<>();
    }

    public Long getId() {
        return id;
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

    public Long getUrl() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Post post = (Post) o;

        return url.equals(post.url);

    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }
}
