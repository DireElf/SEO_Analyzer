package hexlet.code;

import java.net.MalformedURLException;
import java.net.URL;

public final class UrlValidator {
    public static String getNormalizedUrl(String url) {
        try {
            URL temp = new URL(url);
            String result = String.format("%s://%s", temp.getProtocol(), temp.getHost());
            int port = temp.getPort();
            return port > 0 ? result + ":" + port : result;
        } catch (MalformedURLException e) {
            return "";
        }
    }
}
