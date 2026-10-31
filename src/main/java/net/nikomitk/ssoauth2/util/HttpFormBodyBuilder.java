package net.nikomitk.ssoauth2.util;

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

        formData.put(key, value);
        return this;
    }

    public String build() {

        final StringBuilder builder = new StringBuilder();
        for (final Map.Entry<String, String> entry : formData.entrySet()) {
            builder.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        return builder.substring(0, builder.length() - 1);
    }

}