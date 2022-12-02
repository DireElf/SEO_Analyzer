package hexlet.code.service;

import hexlet.code.domain.Url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Utils {
    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    public static String getNormalizedUrl(String url) {
        try {
            LOGGER.info("Try to normalize URL {}", url);
            URL temp = new URL(url);
            String result = String.format("%s://%s", temp.getProtocol(), temp.getHost());
            int port = temp.getPort();
            if (port > 0) {
                result = result + ":" + port;
            }

            LOGGER.info("Received normalized URL {}", result);
            return result;
        } catch (MalformedURLException e) {
            return "";
        }
    }

    public static Map<String, UrlDetails> getUrlDetails(List<Url> urlList) {
        Map<String, UrlDetails> result = new HashMap<>();
        for (Url url : urlList) {
            result.put(url.getName(), new UrlDetails(url));
        }
        return result;
    }
}
