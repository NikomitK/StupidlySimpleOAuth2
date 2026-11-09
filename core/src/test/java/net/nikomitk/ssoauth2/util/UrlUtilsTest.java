package net.nikomitk.ssoauth2.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class UrlUtilsTest {
    

    @Test 
    void isValidUriTest() {
        assertEquals(true, UrlUtils.isValidUri("https://www.example.com"));
        assertEquals(true, UrlUtils.isValidUri("https://www.example.com:8080"));
        assertEquals(true, UrlUtils.isValidUri("https://www.example.com/path"));
        assertEquals(true, UrlUtils.isValidUri("https://www.example.com/path?query=value"));
        assertEquals(true, UrlUtils.isValidUri("https://www.example.com/path?query=value#fragment"));
        assertEquals(true, UrlUtils.isValidUri("https://www.example.com:55697/path?query=value#fragment"));


        assertEquals(false, UrlUtils.isValidUri("https"));
        assertEquals(false, UrlUtils.isValidUri(""));
        assertEquals(false, UrlUtils.isValidUri(" "));
        assertEquals(false, UrlUtils.isValidUri("null"));

    }
}
