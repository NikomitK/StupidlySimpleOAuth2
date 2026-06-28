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

    public HttpFormBodyBuilder addFormData(String key, String value) {

        formData.put(key, value);
        return this;
    }

    public String build() {

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : formData.entrySet()) {
            builder.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
        }
        return builder.substring(0, builder.length() - 1);
    }

}