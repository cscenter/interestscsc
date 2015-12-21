package com.interestscsc.crawler.loaders;

public class UserTagsLoader extends BaseLoader {
    @Override
    public String getUrl() {
        return "http://users.livejournal.com/%s/tag/";
    }
}
