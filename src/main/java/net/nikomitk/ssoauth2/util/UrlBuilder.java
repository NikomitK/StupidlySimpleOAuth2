package net.nikomitk.ssoauth2.util;


import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class UrlBuilder {


    private final StringBuilder query = new StringBuilder();

    public UrlBuilder(final String basepath) {
        query.append(basepath);
        query.append("?");
    }

    public static UrlBuilder withBasePath(final String basepath) {
        return new UrlBuilder(basepath);
    }

    public UrlBuilder addQueryParameter(final String key, final String value) {
        query.append(key).append("=").append(URLEncoder.encode(value, StandardCharsets.UTF_8)).append("&");
        return this;
    }

    public String build() {
        if (!query.isEmpty() && query.charAt(query.length() - 1) == '&') {
            query.deleteCharAt(query.length() - 1);
        }
        return query.toString();
    }
}
