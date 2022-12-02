package hexlet.code.service;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class UrlDetails {
    private final String urlName;
    private final String lastCheckTime;
    private final String statusCode;

    public UrlDetails(Url newUrl) {
        this.urlName = newUrl.getName();
        this.lastCheckTime = defineLastCheckTime(newUrl.getUrlChecks());
        this.statusCode = defineStatusCode(newUrl.getUrlChecks());
    }

    private String defineLastCheckTime(List<UrlCheck> urlChecks) {
        if (urlChecks.isEmpty()) {
            return "-";
        }
        Instant dateTime = urlChecks.get(urlChecks.size() - 1).getCreatedAt();
        return getFormattedTimeStamp(dateTime);
    }

    private String defineStatusCode(List<UrlCheck> urlChecks) {
        if (urlChecks.isEmpty()) {
            return "-";
        }
        return urlChecks.get(urlChecks.size() - 1)
                .getStatusCode() + "";
    }

    private String getFormattedTimeStamp(Instant instant) {
        final String patternFORMAT = "dd.MM.yyyy HH:mm";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(patternFORMAT)
                .withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }

    public String getUrlName() {
        return urlName;
    }

    public String getLastCheckTime() {
        return lastCheckTime;
    }

    public String getStatusCode() {
        return statusCode;
    }
}
