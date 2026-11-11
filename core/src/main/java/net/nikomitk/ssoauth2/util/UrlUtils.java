package net.nikomitk.ssoauth2.util;


import com.sun.net.httpserver.HttpExchange;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.AccessLevel;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UrlUtils {

    public static String getQueryParameter(@NonNull final HttpExchange exchange, @NonNull final String parameterName) {

        String query = exchange.getRequestURI()
                               .getRawQuery();
        if (query == null) {
            return null;
        }
        return getQueryParameter(query, parameterName);
    }

    public static String getQueryParameter(@NonNull final String rawQuery, @NonNull final String parameterName) {

        for (final String pair : rawQuery.split("&")) {
            final String[] parts = pair.split("=", 2);
            if (URLDecoder.decode(parts[0], StandardCharsets.UTF_8)
                          .equals(parameterName)) {
                return parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            }
        }
        return null;
    }

    public static boolean isValidUri(final String uri) {

        if (uri == null || uri.isBlank()) {
            return false;
        }
        try {
            new URI(uri).toURL();
            return true;
        } catch (final URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            return false;
        }
    }

    public static final class Builder {

        private final StringJoiner query = new StringJoiner("&");
        private final String basepath;

        public Builder(@NonNull final String basepath) {

            if (!isValidUri(basepath)) {
                throw new IllegalArgumentException("Invalid base path: " + basepath);
            }
            this.basepath = basepath;
        }

        public static Builder withBasePath(final String basepath) {

            return new Builder(basepath);
        }

        public Builder addQueryParameter(@NonNull final String key, @NonNull final String value) {

            if (key.isBlank()) {
                throw new IllegalArgumentException("Query parameter key must not be blank");
            }
            String parameter = URLEncoder.encode(key, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
            query.add(parameter);
            return this;
        }

        public Builder addNullableQueryParameter(@NonNull final String key, final String value) {

            if (value != null) {
                return addQueryParameter(key, value);
            }
            return this;
        }

        public String build() {

            String url = basepath;
            if (query.length() > 0) {
                url += "?" + query;
            }
            return url;
        }

    }

}
