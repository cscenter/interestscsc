package com.interestscsc.crawler.loaders;

public class UserRobotsLoader extends BaseLoader {
    @Override
    public String getUrl() {
        return "http://users.livejournal.com/%s/robots.txt";
    }
}
