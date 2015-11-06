package crawler.loaders;

public class UserFriendsLoader extends BaseLoader {
    @Override
    public String getUrl() {
        return "http://www.livejournal.com/misc/fdata.bml?user=%s";
    }
}
