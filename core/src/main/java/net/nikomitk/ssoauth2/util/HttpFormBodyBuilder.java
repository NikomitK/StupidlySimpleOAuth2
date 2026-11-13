package net.nikomitk.ssoauth2.util;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;

import static net.nikomitk.ssoauth2.util.CoreUtil.urlEncode;

/**
 * A utility class for building HTTP form bodies in the application/x-www-form-urlencoded format.
 * This class provides a fluent API for adding form data and constructing the final form body string.
 */
public class HttpFormBodyBuilder {

    private final List<String> formData = new ArrayList<>();

    private HttpFormBodyBuilder() {

    }

    /**
     * Creates a new instance of HttpFormBodyBuilder.
     *
     * @return the created HttpFormBodyBuilder instance.
     */
    public static HttpFormBodyBuilder newBuilder() {

        return new HttpFormBodyBuilder();
    }

    /**
     * Adds a key-value pair to the form data being built. Both key and value will be URL-Encoded in the UTF-8 charset.
     *
     * @param key the key of the form data to add. Must not be blank.
     * @param value the value of the form data to add. Must not be null, but can be blank.
     * @return this HttpFormBodyBuilder instance, allowing for method chaining.
     */
    public HttpFormBodyBuilder addFormData(@NonNull final String key, @NonNull final String value) {

        if (key.isBlank()) {
            throw new IllegalArgumentException("Form data key must not be blank");
        }
        formData.add(urlEncode(key) + "=" + urlEncode(value));

        return this;
    }

    /**
     * Adds a key-value pair to the form data being built, if the value is not null. Both key and value will be URL-Encoded in the UTF-8 charset.
     * If the value is null, the key-value pair will not be added to the form data.
     *
     * @param key the key of the form data to add. Must not be blank.
     * @param value the value of the form data to add. Can be null.
     * @return this HttpFormBodyBuilder instance, allowing for method chaining.
     */
    public HttpFormBodyBuilder addNullableFormData(@NonNull final String key, final String value) {

        if (value != null) {
            return addFormData(key, value);
        }
        return this;
    }

    /**
     * Builds the final form body string in the application/x-www-form-urlencoded format.
     *
     * @return the final form body string.
     */
    public String build() {

        return String.join("&", formData);
    }

}