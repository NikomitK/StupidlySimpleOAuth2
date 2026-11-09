package net.nikomitk.ssoauth2.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpFormBodyBuilderTest {

    @ParameterizedTest
    @CsvSource({"grant_type, authorization_code, grant_type=authorization_code", "a-b.c_d*e, a-b.c_d*e, a-b.c_d*e=a-b.c_d*e", "q, 'hello world', q=hello+world", "q, a&b=c, q=a%26b%3Dc", "q, a#b?c/d, q=a%23b%3Fc%2Fd", "redirect_uri, http://localhost:8080/cb, redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fcb", "q, äö, q=%C3%A4%C3%B6", "'a b', value, a+b=value", "a&b, value, a%26b=value", "a=b, value, a%3Db=value"})
    void builderBuildsEncodedFormBodyForSingleEntry(final String key, final String value, final String expectedBody) {

        final String body = HttpFormBodyBuilder.newBuilder()
                                               .addFormData(key, value)
                                               .build();

        assertEquals(expectedBody, body);
    }

    @Test
    void builderBuildsFormBodyContainingAllEntries() {

        final String body = HttpFormBodyBuilder.newBuilder()
                                               .addFormData("grant_type", "authorization_code")
                                               .addFormData("code", "abc 123")
                                               .addFormData("redirect_uri", "http://localhost:8080/cb")
                                               .build();

        final Set<String> expectedPairs = Set.of("grant_type=authorization_code", "code=abc+123", "redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fcb");
        assertEquals(expectedPairs, Arrays.stream(body.split("&"))
                                          .collect(Collectors.toSet()));
    }

    @Test
    void builderBuildsEmptyBodyWithoutEntries() {

        assertEquals("", HttpFormBodyBuilder.newBuilder()
                                            .build());
    }

    @Test
    void builderRejectsNullFormDataKey() {

        final HttpFormBodyBuilder builder = HttpFormBodyBuilder.newBuilder();

        assertThrows(NullPointerException.class, () -> builder.addFormData(null, "value"));
    }

    @Test
    void builderRejectsNullFormDataValue() {

        final HttpFormBodyBuilder builder = HttpFormBodyBuilder.newBuilder();

        assertThrows(NullPointerException.class, () -> builder.addFormData("key", null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void builderRejectsBlankFormDataKeys(final String key) {

        final HttpFormBodyBuilder builder = HttpFormBodyBuilder.newBuilder();

        assertThrows(IllegalArgumentException.class, () -> builder.addFormData(key, "value"));
    }

    @Test
    void builderAddsNullableFormDataWithValue() {

        final String body = HttpFormBodyBuilder.newBuilder()
                                               .addNullableFormData("a&b", "hello world")
                                               .build();

        assertEquals("a%26b=hello+world", body);
    }

    @Test
    void builderSkipsNullableFormDataWithoutValue() {

        final String body = HttpFormBodyBuilder.newBuilder()
                                               .addFormData("a", "1")
                                               .addNullableFormData("b", null)
                                               .build();

        assertEquals("a=1", body);
    }

    @Test
    void builderRejectsNullKeyOfNullableFormData() {

        final HttpFormBodyBuilder builder = HttpFormBodyBuilder.newBuilder();

        assertThrows(NullPointerException.class, () -> builder.addNullableFormData(null, "value"));
    }

}
