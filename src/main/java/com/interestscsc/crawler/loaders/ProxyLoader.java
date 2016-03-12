package com.interestscsc.crawler.loaders;

import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.HttpHost;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.concurrent.TimeUnit;

public class ProxyLoader {
    public boolean loadData(HttpHost httpHostProxy, String... argsToEncode) throws IOException, UnirestException, InterruptedException, RuntimeException {
        String urlString = getUrl();
        String[] encodedArgs = new String[argsToEncode.length];

        for (int i = 0; i < argsToEncode.length; i++) {
            encodedArgs[i] = URLEncoder.encode(argsToEncode[i], "UTF-8");
        }

        Thread.sleep(200);
        URL url = new URL(String.format(urlString, encodedArgs));

        SocketAddress address = new InetSocketAddress(httpHostProxy.getHostName(), httpHostProxy.getPort());
        Proxy proxy = new Proxy(Proxy.Type.HTTP, address);
        URLConnection conn = url.openConnection(proxy);

        conn.setConnectTimeout(new Long(TimeUnit.SECONDS.toMillis(4)).intValue());
        conn.setReadTimeout(new Long(TimeUnit.SECONDS.toMillis(8)).intValue());
        conn.connect();
        InputStream inputStream = conn.getInputStream();
        return inputStream == null;

    }

    public String getUrl() {
        return "http://users.livejournal.com/%s/tag/";
    }
}
