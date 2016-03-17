package com.interestscsc.crawler.loaders;

/**
 * Created by Maxim on 17.03.2016.
 */
public class ProxyListLoader extends BaseLoader {

    public static final String NOT_FOUND_STATUS_PAGE = "Not Found";

    @Override
    public String getUrl() {
        return "http://webanetlabs.net/freeproxy/proxylist_at_%s.txt";
    }
}
