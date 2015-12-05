package crawler.loaders;

import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.HttpHost;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.concurrent.TimeUnit;

public class ProxyLoader {
    public int loadData(HttpHost httpHostProxy, String... argsToEncode) throws IOException, UnirestException, InterruptedException, RuntimeException {
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

        conn.setConnectTimeout(new Long(TimeUnit.SECONDS.toMillis(6)).intValue());
        conn.setReadTimeout(new Long(TimeUnit.SECONDS.toMillis(10)).intValue());
        conn.connect();
        InputStream inputStream = conn.getInputStream();
        if (inputStream == null) {
            return 1;
        }

        return 0;
    }

    public String getUrl() {
        return "http://www.livejournal.com/misc/fdata.bml?user=%s";
    }
}
