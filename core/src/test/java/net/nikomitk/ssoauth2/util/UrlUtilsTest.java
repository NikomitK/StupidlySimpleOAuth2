package net.nikomitk.ssoauth2.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

class UrlUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {"https://www.example.com/path?query2=value2&query=value",
            "https://www.example.com/path?query=value#fragment",
            "https://www.example.com/path?query=value#fragment"})
    void getQueryParameterReturnsValueOfRequestedParameter(final String url) {
        assertEquals("value", UrlUtils.getQueryParameter(java.net.URI.create(url).getQuery(), "query"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://www.example.com",
            "https://www.example.com:8080",
            "https://www.example.com/path",
            "https://www.example.com/path?query=value",
            "https://www.example.com/path?query=value#fragment",
            "https://www.example.com:55697/path?query=value#fragment"})
    void isValidUriAcceptsWellFormedUris(final String uri) {
        assertTrue(UrlUtils.isValidUri(uri));
    }

    @ParameterizedTest
    @ValueSource(strings = {"https", "", " ", "null"})
    void isValidUriRejectsMalformedAndBlankUris(final String uri) {
        assertFalse(UrlUtils.isValidUri(uri));
    }

    static Stream<Arguments> builderCases() {
        return Stream.of(
                Arguments.of("https://www.example.com/path", Map.of("query", "value"), "https://www.example.com/path?query=value"),
                Arguments.of("https://www.example.com/path", orderedParameters("a", "1", "b", "2", "c", "3"), "https://www.example.com/path?a=1&b=2&c=3"),
                Arguments.of("https://www.example.com", Map.of("q", "hello world"), "https://www.example.com?q=hello+world"),
                Arguments.of("https://www.example.com", Map.of("q", "a&b=c"), "https://www.example.com?q=a%26b%3Dc"),
                Arguments.of("https://www.example.com", Map.of("redirect_uri", "http://localhost:8080/cb"),
                             "https://www.example.com?redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fcb"));
    }

    private static Map<String, String> orderedParameters(final String... keysAndValues) {
        final Map<String, String> parameters = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            parameters.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return parameters;
    }

    @ParameterizedTest
    @MethodSource("builderCases")
    void builderBuildsUrlWithEncodedQueryParameters(final String basePath, final Map<String, String> parameters, final String expectedUrl) {
        final UrlUtils.Builder builder = UrlUtils.Builder.withBasePath(basePath);
        parameters.forEach(builder::addQueryParameter);

        assertEquals(expectedUrl, builder.build());
    }

    @Test
    void builderRejectsNullBasePath() {
        assertThrows(NullPointerException.class, () -> UrlUtils.Builder.withBasePath(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "https", "not a url", "https://exa mple.com", "://www.example.com"})
    void builderRejectsInvalidBasePaths(final String basePath) {
        assertThrows(IllegalArgumentException.class, () -> UrlUtils.Builder.withBasePath(basePath));
    }

    @Test
    void builderRejectsNullQueryParameterKey() {
        final UrlUtils.Builder builder = UrlUtils.Builder.withBasePath("https://www.example.com");

        assertThrows(NullPointerException.class, () -> builder.addQueryParameter(null, "value"));
    }

    @Test
    void builderRejectsNullQueryParameterValue() {
        final UrlUtils.Builder builder = UrlUtils.Builder.withBasePath("https://www.example.com");

        assertThrows(NullPointerException.class, () -> builder.addQueryParameter("key", null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void builderRejectsBlankQueryParameterKeys(final String key) {
        final UrlUtils.Builder builder = UrlUtils.Builder.withBasePath("https://www.example.com");

        assertThrows(IllegalArgumentException.class, () -> builder.addQueryParameter(key, "value"));
    }

    @ParameterizedTest
    @CsvSource({"redirect_uri, redirect_uri",
            "a-b.c_d*e, a-b.c_d*e",
            "'a b', a+b",
            "a&b, a%26b",
            "a=b, a%3Db",
            "a#b, a%23b",
            "a?b, a%3Fb",
            "a/b, a%2Fb",
            "äö, %C3%A4%C3%B6"})
    void builderEncodesQueryParameterKeys(final String key, final String expectedEncodedKey) {
        final String url = UrlUtils.Builder.withBasePath("https://www.example.com")
                                           .addQueryParameter(key, "value")
                                           .build();

        assertEquals("https://www.example.com?" + expectedEncodedKey + "=value", url);
    }

    @Test
    void builderEncodesKeyOfNullableQueryParameter() {
        final String url = UrlUtils.Builder.withBasePath("https://www.example.com")
                                           .addNullableQueryParameter("a&b", "value")
                                           .build();

        assertEquals("https://www.example.com?a%26b=value", url);
    }

}
