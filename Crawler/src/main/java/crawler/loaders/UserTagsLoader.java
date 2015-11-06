package crawler.loaders;

/**
 * Created by Maxim on 04.11.2015.
 */
public class UserTagsLoader extends BaseLoader {
    @Override
    public String getUrl() {
        return "http://users.livejournal.com/%s/tag/";
    }
}
