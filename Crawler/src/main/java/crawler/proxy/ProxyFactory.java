package crawler.proxy;

import org.apache.http.HttpHost;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ProxyFactory {
    private Map<String, Integer> proxies;

    public ProxyFactory() {
        proxies = new HashMap<>();
        proxies.put("24.246.127.180", 8080);
    }

    public HttpHost getProxy() {
        Set<String> hostSet = proxies.keySet();
        String hostname = hostSet.iterator().next();
        return new HttpHost(hostname, proxies.get(hostname));
    }

    public void setProxy(String hostname, int port) {
        proxies.put(hostname, port);
    }
}
