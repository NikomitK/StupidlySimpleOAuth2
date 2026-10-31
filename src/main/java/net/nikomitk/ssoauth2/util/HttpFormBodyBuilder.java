package net.nikomitk.ssoauth2.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpFormBodyBuilder {

    private final Map<String, String> formData;

    private HttpFormBodyBuilder() {

        formData = new HashMap<>();
    }

    public static HttpFormBodyBuilder newBuilder() {

        return new HttpFormBodyBuilder();
    }

    public HttpFormBodyBuilder addFormData(final String key, final String value) {

        if (value != null) {
            formData.put(key, value);
        }
        return this;
    }

    public String build() {

        final StringBuilder builder = new StringBuilder();
        for (final Map.Entry<String, String> entry : formData.entrySet()) {
            builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                   .append("=")
                   .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                   .append("&");
        }
        return builder.substring(0, builder.length() - 1);
    }

}