package crawler.loaders;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public abstract class BaseLoader {

    public String loadData(String... argsToEncode) throws UnsupportedEncodingException, UnirestException, InterruptedException {
        String url = getUrl();
        String[] encodedArgs = new String[argsToEncode.length];

        for (int i = 0; i < argsToEncode.length; i++) {
            encodedArgs[i] =  URLEncoder.encode(argsToEncode[i], "UTF-8");
        }

        Thread.sleep(200);
        HttpResponse<String> response = Unirest.get(String.format(url, encodedArgs))
                .header("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3")
                .asString();

        return response.getBody();
    }

    public abstract String getUrl();
}
