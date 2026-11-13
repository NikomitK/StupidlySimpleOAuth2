package net.nikomitk.ssoauth2.util;


import com.sun.net.httpserver.HttpExchange;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

import static net.nikomitk.ssoauth2.util.CoreUtil.urlEncode;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class UrlUtils {

    /**
     * Gets the value of a query parameter from the request URI of the given HttpExchange.
     *
     * @param exchange the HttpExchange object containing the request URI.
     * @param parameterName the name of the query parameter to retrieve.
     * @return the value of the query parameter, or null if the parameter is not present in the request URI.
     */
    public static String getQueryParameter(@NonNull final HttpExchange exchange, @NonNull final String parameterName) {

        final String query = exchange.getRequestURI()
                                     .getRawQuery();
        if (query == null) {
            return null;
        }
        return getQueryParameter(query, parameterName);
    }

    /**
     * Gets the value of a query parameter from the given raw query string.
     *
     * @param rawQuery the raw query string.
     * @param parameterName the name of the query parameter to retrieve.
     * @return the value of the query parameter, or null if the parameter is not present in the raw query string.
     */
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

    /**
     * Checks if the given string is a valid URL.
     *
     * @param url the string to check.
     * @return true if the string is a valid URL, false otherwise.
     */
    public static boolean isValidUrl(final String url) {

        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            new URI(url).toURL();
            return true;
        } catch (final URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * A builder class for constructing URLs with query parameters.
     */
    public static final class Builder {

        private final StringJoiner query = new StringJoiner("&");
        private final String basepath;

        /**
         * Creates a new Builder instance with the specified base path.
         * @param basepath the basepath of the url to build. Must be a valid URL without query parameters.
         */
        public Builder(@NonNull final String basepath) {

            if (!isValidUrl(basepath)) {
                throw new IllegalArgumentException("Invalid base path: " + basepath);
            }
            this.basepath = basepath;
        }

        /**
         * Convenience method to create a new Builder instance with the specified base path.
         *
         * @param basepath the basepath of the url to build. Must be a valid URL without query parameters.
         * @return a new Builder instance with the specified base path.
         */
        public static Builder withBasePath(final String basepath) {

            return new Builder(basepath);
        }

        /**
         * Adds a query parameter to the URL being built.
         *
         * @param key the key of the query parameter to add. Must not be blank.
         * @param value the value of the query parameter to add. Must not be null, but can be blank.
         * @return this Builder instance, allowing for method chaining.
         */
        public Builder addQueryParameter(@NonNull final String key, @NonNull final String value) {

            if (key.isBlank()) {
                throw new IllegalArgumentException("Query parameter key must not be blank");
            }

            query.add(urlEncode(key) + "=" + urlEncode(value));
            return this;
        }

        /**
         * Adds a query parameter to the URL being built, if the value is not null.
         *
         * @param key the key of the query parameter to add. Must not be blank.
         * @param value the value of the query parameter to add. May be null.
         * @return this Builder instance, allowing for method chaining.
         */
        public Builder addNullableQueryParameter(@NonNull final String key, final String value) {

            if (value != null) {
                return addQueryParameter(key, value);
            }
            return this;
        }

        /**
         * Builds the final URL string with the base path and any added query parameters.
         *
         * @return the final URL string.
         */
        public String build() {

            String url = basepath;
            if (query.length() > 0) {
                url += "?" + query;
            }
            return url;
        }

    }

}
