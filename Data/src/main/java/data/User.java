package data;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;

public class User {
    public static class School {
        private String title;
        private Date start;
        private Date end;

        public School(String title, Date start, Date end) {
            this.title = title;
            this.start = start;
            this.end = end;
        }

        public String getTitle() {
            return title;
        }

        public Date getStart() {
            return start;
        }

        public Date getEnd() {
            return end;
        }
    }

    private Long id;
    private String nick;
    private String region;
    private Timestamp dateCreated;
    private Timestamp dateUpdated;
    private Timestamp dateFetched;
    private Date birthday;
    private String interests;
    private String customCity;
    private Integer postsNum;
    private Integer commentsPosted;
    private Integer commentsReceived;
    private String biography;
    private List<School> schools;

    public User(Long id, String nick, String region,
                Timestamp dateCreated, Timestamp dateUpdated,
                Timestamp dateFetched, Date birthday,
                String interests, String customCity,
                Integer postsNum, Integer commentsPosted,
                Integer commentsReceived,
                String biography, List<School> schools) {
        if(schools == null)
            throw new IllegalArgumentException("List of schools can't be null");
        this.id = id;
        this.nick = nick;
        this.region = region;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.dateFetched = dateFetched;
        this.birthday = birthday;
        this.interests = interests;
        this.customCity = customCity;
        this.postsNum = postsNum;
        this.commentsPosted = commentsPosted;
        this.commentsReceived = commentsReceived;
        this.biography = biography;
        this.schools = schools;
    }

    public User(
            final String nick, final String region,
            final Timestamp dateCreated, final Timestamp dateUpdated,
            final Timestamp dateFetched, final Date birthday,
            final String interests
    ) {
        this.nick = nick;
        this.region = region;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.dateFetched = dateFetched;
        this.birthday = birthday;
        this.interests = interests;
        this.schools = new LinkedList<>();
    }

    public Long getId() {
        return id;
    }

    public String getNick() {
        return nick;
    }

    public String getRegion() {
        return region;
    }

    public Timestamp getDateCreated() {
        return dateCreated;
    }

    public Timestamp getDateUpdated() {
        return dateUpdated;
    }

    public Timestamp getDateFetched() {
        return dateFetched;
    }

    public Date getBirthday() {
        return birthday;
    }

    public String getInterests() {
        return interests;
    }

    public String getCustomCity() {
        return customCity;
    }

    public Integer getPostsNum() {
        return postsNum;
    }

    public Integer getCommentsPosted() {
        return commentsPosted;
    }

    public Integer getCommentsReceived() {
        return commentsReceived;
    }

    public String getBiography() {
        return biography;
    }

    public List<School> getSchools() {
        return schools;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", nick='" + nick + '\'' +
                ", region='" + region + '\'' +
                ", dateCreated=" + dateCreated +
                ", dateUpdated=" + dateUpdated +
                ", dateFetched=" + dateFetched +
                ", birthday=" + birthday +
                ", interests='" + interests + '\'' +
                ", customCity='" + customCity + '\'' +
                ", postsNum=" + postsNum +
                ", commentsPosted=" + commentsPosted +
                ", commentsReceived=" + commentsReceived +
                ", biography='" + biography + '\'' +
                ", schools=" + schools +
                '}';
    }
}
