package net.nikomitk.ssoauth2.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

}
