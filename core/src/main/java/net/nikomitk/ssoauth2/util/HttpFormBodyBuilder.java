package net.nikomitk.ssoauth2.util;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

import static net.nikomitk.ssoauth2.util.CoreUtil.urlEncode;

public class HttpFormBodyBuilder {

    private final List<String> formData = new ArrayList<>();

    private HttpFormBodyBuilder() {

    }

    public static HttpFormBodyBuilder newBuilder() {

        return new HttpFormBodyBuilder();
    }

    public HttpFormBodyBuilder addFormData(@NonNull final String key, @NonNull final String value) {

        if (key.isBlank()) {
            throw new IllegalArgumentException("Form data key must not be blank");
        }
        formData.add(urlEncode(key) + "=" + urlEncode(value));

        return this;
    }

    public HttpFormBodyBuilder addNullableFormData(@NonNull final String key, final String value) {

        if (value != null) {
            return addFormData(key, value);
        }
        return this;
    }

    public String build() {

        return String.join("&", formData);
    }

}