package com.interestscsc.crawler.parsers;

import org.apache.http.HttpHost;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Maxim on 17.03.2016.
 */
public class ProxyListParser {
    public static Set<HttpHost> getProxyList(String response) {
        Set<HttpHost> rawProxies = new HashSet<>();

        String[] proxies = response.split("\r\n");
        for (String proxy : proxies) {
            String[] proxyString = proxy.split(":");
            rawProxies.add(new HttpHost(proxyString[0], Integer.parseInt(proxyString[1])));
        }

        return rawProxies;
    }
}
