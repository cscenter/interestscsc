package crawler.loaders;

public class TagPostLoader extends BaseLoader {

    @Override
    public String getUrl() {
        return "http://users.livejournal.com/%s/data/rss/?tag=%s";
    }
}
