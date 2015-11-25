package data;

import java.sql.Date;
import java.sql.Timestamp;
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

    private final Long id;
    private final String nick;
    private final String region;
    private final Timestamp dateCreated;
    private final Timestamp dateUpdated;
    private final Timestamp dateFetched;
    private final Date birthday;
    private final String interests;
    private final String customCity;
    private final Integer postsNum;
    private final Integer commentsPosted;
    private final Integer commentsReceived;
    private final String biography;
    private final List<School> schools;

    public static class UserBuilder {
        // not null
        private final String nick;

        // maybe null
        private Long id = null;
        private String region = null;
        private Timestamp dateCreated = null;
        private Timestamp dateUpdated = null;
        private Timestamp dateFetched = null;
        private Date birthday = null;
        private String interests = null;
        private String customCity = null;
        private Integer postsNum = null;
        private Integer commentsPosted = null;
        private Integer commentsReceived = null;
        private String biography = null;
        private List<School> schools = null;

        public UserBuilder(final String nick) {
            this.nick = nick;
        }

        public UserBuilder setId(Long id) {
            this.id = id;
            return this;
        }

        public UserBuilder setRegion(String region) {
            this.region = region;
            return this;
        }

        public UserBuilder setDateCreated(Timestamp dateCreated) {
            this.dateCreated = dateCreated;
            return this;
        }

        public UserBuilder setDateUpdated(Timestamp dateUpdated) {
            this.dateUpdated = dateUpdated;
            return this;
        }

        public UserBuilder setDateFetched(Timestamp dateFetched) {
            this.dateFetched = dateFetched;
            return this;
        }

        public UserBuilder setBirthday(Date birthday) {
            this.birthday = birthday;
            return this;
        }

        public UserBuilder setInterests(String interests) {
            this.interests = interests;
            return this;
        }

        public UserBuilder setCustomCity(String customCity) {
            this.customCity = customCity;
            return this;
        }

        public UserBuilder setPostsNum(Integer postsNum) {
            this.postsNum = postsNum;
            return this;
        }

        public UserBuilder setCommentsPosted(Integer commentsPosted) {
            this.commentsPosted = commentsPosted;
            return this;
        }

        public UserBuilder setCommentsReceived(Integer commentsReceived) {
            this.commentsReceived = commentsReceived;
            return this;
        }

        public UserBuilder setBiography(String biography) {
            this.biography = biography;
            return this;
        }

        public UserBuilder setSchools(List<School> schools) {
            this.schools = schools;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }

    public User(UserBuilder builder) {
        if(builder.schools == null)
            throw new IllegalArgumentException("List of schools can't be null");
       id = builder.id;
       nick = builder.nick;
       region = builder.region;
       dateCreated = builder.dateCreated;
       dateUpdated = builder.dateUpdated;
       dateFetched = builder.dateFetched;
       birthday = builder.birthday;
       interests = builder.interests;
       customCity = builder.customCity;
       postsNum = builder.postsNum;
       commentsPosted = builder.commentsPosted;
       commentsReceived = builder.commentsReceived;
       biography = builder.biography;
       schools = builder.schools;
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
