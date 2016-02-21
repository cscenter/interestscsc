package com.interestscsc.crawler.loaders;

public class UserInfoLoader extends BaseLoader {
    @Override
    public String getUrl() {
        return "http://users.livejournal.com/%s/data/foaf";
    }
}
