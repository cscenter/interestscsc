package crawler.loaders;

/**
 * Created by Maxim on 04.11.2015.
 */
public class UserFriendsLoader extends BaseLoader {
    @Override
    public String getUrl() {
        return "http://www.livejournal.com/misc/fdata.bml?user=%s";
    }
}
