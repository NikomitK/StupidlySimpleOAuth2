package net.nikomitk.ssoauth2.util;


import com.sun.net.httpserver.HttpExchange;

import java.net.*;
import java.nio.charset.StandardCharsets;

public class UrlUtils {

    public static String getQueryParameter(final HttpExchange exchange, final String parameterName) {

        final String query = exchange.getRequestURI()
                                     .getQuery();
        if (query == null) {
            return null;
        }
        final String[] params = query.split("&");
        for (final String param : params) {
            if (param.startsWith(parameterName + "=")) {
                final String[] parts = param.split("=", 2);
                if (parts.length < 2) {
                    return "";
                }
                return parts[1];
            }
        }
        return null;
    }

    public static boolean isValidUri(final String uri) {

        if(uri == null || uri.isEmpty() || uri.isBlank()) {
            return false;
        }
        try {
            new URI(uri).toURL();
            return true;
        } catch (final URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            return false;
        }
    }

    public static class Builder {

        private final StringBuilder query = new StringBuilder();

        public Builder(final String basepath) {

            query.append(basepath);
            query.append("?");
        }

        public static Builder withBasePath(final String basepath) {

            return new Builder(basepath);
        }

        public Builder addQueryParameter(final String key, final String value) {

            query.append(key)
                 .append("=")
                 .append(URLEncoder.encode(value, StandardCharsets.UTF_8))
                 .append("&");
            return this;
        }

        public Builder addNullableQueryParameter(final String key, final String value) {

            if (value != null) {
                return addQueryParameter(key, value);
            }
            return this;
        }

        public String build() {

            if (!query.isEmpty() && query.charAt(query.length() - 1) == '&') {
                query.deleteCharAt(query.length() - 1);
            }
            return query.toString();
        }

    }

}
