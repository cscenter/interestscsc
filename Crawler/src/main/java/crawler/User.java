package crawler;

import java.sql.Date;
import java.sql.Timestamp;

public class User {

    private String nick;
    private String region;
    private Timestamp dateCreated;
    private Timestamp dateUpdated;
    private Date birthday;
    private String interests;

    public User(
            final String nick, final String region,
            final Timestamp dateCreated, final Timestamp dateUpdated,
            final Date birthday, final String interests
    ) {
        this.nick = nick;
        this.region = region;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.birthday = birthday;
        this.interests = interests;
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

    public Date getBirthday() {
        return birthday;
    }

    public String getInterests() {
        return interests;
    }

    public String writeToFile() {
        return "nick : " + nick + "\n" +
                "region : " + region + "\n" +
                "dateCreated : " + dateCreated + "\n" +
                "dateUpdated : " + dateUpdated + "\n" +
                "birthday : " + birthday + "\n" +
                "interests : " + interests + "\n";
    }
}
