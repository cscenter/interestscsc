package crawler;

public class User {

    private String nick;
    private String region;
    private String date_created;
    private String date_updated;
    private String birthday;
    private String url;

    public User(
            final String nick, final String region,
            final String date_created, final String date_updated,
            final String birthday, final String url
    ) {
        this.nick = nick;
        this.region = region;
        this.date_created = date_created;
        this.date_updated = date_updated;
        this.birthday = birthday;
        this.url = url;
    }

    public String getNick() {
        return nick;
    }

    public String getRegion() {
        return region;
    }

    public String getDate_created() {
        return date_created;
    }

    public String getDate_updated() {
        return date_updated;
    }

    public String getBirthday() {
        return birthday;
    }

    public String getUrl() {
        return url;
    }
}
