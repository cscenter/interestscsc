package crawler.loaders;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.HttpHost;
import org.apache.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public abstract class BaseLoader {
    public static final String ERROR_STATUS_PAGE = "ERROR";
    private static Object lockObject = new Object();

    public String loadData(HttpHost proxy, String... argsToEncode) throws UnsupportedEncodingException, UnirestException, InterruptedException, RuntimeException {
        String url = getUrl();
        String[] encodedArgs = new String[argsToEncode.length];

        for (int i = 0; i < argsToEncode.length; i++) {
            encodedArgs[i] =  URLEncoder.encode(argsToEncode[i], "UTF-8");
        }

        HttpResponse<String> response;
        synchronized (lockObject) {
            Thread.sleep(200);
            Unirest.setProxy(proxy);
            response = Unirest.get(String.format(url, encodedArgs))
                    .header("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3")
                    .asString();
        }

        if (!"OK".equals(response.getStatusText())) {
            return ERROR_STATUS_PAGE;
        }
        return response.getBody();
    }

    public abstract String getUrl();
}
