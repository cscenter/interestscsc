package data;

import java.sql.Date;
import java.sql.Timestamp;

public class User {

    private String nick;
    private String region;
    private Timestamp dateCreated;
    private Timestamp dateUpdated;
    private Timestamp dateFetched;
    private Date birthday;
    private String interests;

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

    public String toString() {
        return "nick : " + nick + "\n" +
                "region : " + region + "\n" +
                "dateCreated : " + dateCreated + "\n" +
                "dateUpdated : " + dateUpdated + "\n" +
                "dateFetched : " + dateFetched + "\n" +
                "birthday : " + birthday + "\n" +
                "interests : " + interests + "\n";
    }
}
