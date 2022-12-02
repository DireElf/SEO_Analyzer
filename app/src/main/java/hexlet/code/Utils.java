package hexlet.code;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

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
}
