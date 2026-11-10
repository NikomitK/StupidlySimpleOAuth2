package net.nikomitk.ssoauth2.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import net.nikomitk.ssoauth2.dto.OAuth2Endpoint;
import net.nikomitk.ssoauth2.dto.OAuth2TokenResponse;
import net.nikomitk.ssoauth2.exception.OAuth2RequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.http.HttpRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreUtilHttpTest {

    private static final String WELL_KNOWN_JSON = """
            {
              "issuer": "https://issuer.example",
              "authorization_endpoint": "https://issuer.example/authorize",
              "token_endpoint": "https://issuer.example/token",
              "some_unknown_field": "must be ignored"
            }
            """;
    private static final String TOKEN_JSON = """
            {
              "access_token": "access",
              "token_type": "Bearer",
              "expires_in": 3600,
              "refresh_token": "refresh"
            }
            """;

    private MockWebServer server;

    @BeforeEach
    void startServer() throws IOException {

        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void stopServer() {

        server.close();
    }

    private String wellKnownUrl() {

        return server.url("/.well-known/openid-configuration")
                     .toString();
    }

    private HttpRequest tokenRequest() {

        return CoreUtil.requestBuilder(server.url("/token")
                                             .uri())
                       .header("Content-Type", "application/x-www-form-urlencoded")
                       .POST(HttpRequest.BodyPublishers.ofString("grant_type=password"))
                       .build();
    }

    @Test
    void queryWellKnownEndpointParsesEndpointAndIgnoresUnknownFields() {

        server.enqueue(new MockResponse.Builder().body(WELL_KNOWN_JSON).build());

        final OAuth2Endpoint endpoint = CoreUtil.queryWellKnownEndpoint(wellKnownUrl());

        assertEquals("https://issuer.example", endpoint.getIssuer());
        assertEquals("https://issuer.example/authorize", endpoint.getAuthorizationEndpoint());
        assertEquals("https://issuer.example/token", endpoint.getTokenEndpoint());
    }

    @Test
    void queryWellKnownEndpointSendsGetWithDefaultUserAgent() throws InterruptedException {

        server.enqueue(new MockResponse.Builder().body(WELL_KNOWN_JSON).build());

        CoreUtil.queryWellKnownEndpoint(wellKnownUrl());

        final RecordedRequest request = server.takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/.well-known/openid-configuration", request.getTarget());
        assertEquals(CoreUtil.DEFAULT_USER_AGENT, request.getHeaders().get("User-Agent"));
    }

    @ParameterizedTest
    @ValueSource(ints = {204, 301, 404, 500})
    void queryWellKnownEndpointRejectsNon200Status(final int status) {

        server.enqueue(new MockResponse.Builder().code(status).build());

        final RuntimeException exception = assertThrows(RuntimeException.class, () -> CoreUtil.queryWellKnownEndpoint(wellKnownUrl()));

        assertTrue(exception.getMessage()
                            .contains(String.valueOf(status)));
    }

    @Test
    void queryWellKnownEndpointWrapsInvalidJson() {

        server.enqueue(new MockResponse.Builder().body("this is not json").build());

        final RuntimeException exception = assertThrows(RuntimeException.class, () -> CoreUtil.queryWellKnownEndpoint(wellKnownUrl()));

        assertInstanceOf(JsonProcessingException.class, exception.getCause());
    }

    @Test
    void queryWellKnownEndpointWrapsConnectionFailure() {

        final String url = wellKnownUrl();
        server.close();

        final RuntimeException exception = assertThrows(RuntimeException.class, () -> CoreUtil.queryWellKnownEndpoint(url));

        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    void queryWellKnownEndpointRejectsNullArguments() {

        assertThrows(NullPointerException.class, () -> CoreUtil.queryWellKnownEndpoint(null, wellKnownUrl(), CoreUtil.createObjectMapper()));
        assertThrows(NullPointerException.class, () -> CoreUtil.queryWellKnownEndpoint(CoreUtil.defaultHttpClient(), null, CoreUtil.createObjectMapper()));
        assertThrows(NullPointerException.class, () -> CoreUtil.queryWellKnownEndpoint(CoreUtil.defaultHttpClient(), wellKnownUrl(), null));
    }

    @Test
    void requestTokenParsesTokenResponse() throws Exception {

        server.enqueue(new MockResponse.Builder().body(TOKEN_JSON).build());

        final OAuth2TokenResponse response = CoreUtil.requestToken(tokenRequest());

        assertEquals("access", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600, response.getExpiresIn());
        assertEquals("refresh", response.getRefreshToken());
        assertNotNull(response.getReceivedAt());
    }

    @Test
    void requestTokenSendsRequestUnchanged() throws Exception {

        server.enqueue(new MockResponse.Builder().body(TOKEN_JSON).build());

        CoreUtil.requestToken(tokenRequest());

        final RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/token", request.getTarget());
        assertEquals("application/x-www-form-urlencoded", request.getHeaders().get("Content-Type"));
        assertEquals(CoreUtil.DEFAULT_USER_AGENT, request.getHeaders().get("User-Agent"));
        assertEquals("grant_type=password", request.getBody()
                                                   .utf8());
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 500})
    void requestTokenThrowsRequestExceptionWithStatusAndBodyOnError(final int status) {

        server.enqueue(new MockResponse.Builder().code(status)
                                                 .body("{\"error\":\"invalid_grant\"}")
                                                 .build());

        final OAuth2RequestException exception = assertThrows(OAuth2RequestException.class, () -> CoreUtil.requestToken(tokenRequest()));

        assertTrue(exception.getMessage()
                            .contains(String.valueOf(status)));
        assertTrue(exception.getMessage()
                            .contains("invalid_grant"));
    }

    @Test
    void requestTokenThrowsJsonProcessingExceptionOnInvalidJson() {

        server.enqueue(new MockResponse.Builder().body("this is not json").build());

        assertThrows(JsonProcessingException.class, () -> CoreUtil.requestToken(tokenRequest()));
    }

    @Test
    void requestTokenThrowsIoExceptionOnConnectionFailure() {

        final HttpRequest request = tokenRequest();
        server.close();

        assertThrows(IOException.class, () -> CoreUtil.requestToken(request));
    }

    @Test
    void requestTokenRejectsNullArguments() {

        final HttpRequest request = tokenRequest();

        assertThrows(NullPointerException.class, () -> CoreUtil.requestToken(null, request, CoreUtil.createObjectMapper()));
        assertThrows(NullPointerException.class, () -> CoreUtil.requestToken(CoreUtil.defaultHttpClient(), null, CoreUtil.createObjectMapper()));
        assertThrows(NullPointerException.class, () -> CoreUtil.requestToken(CoreUtil.defaultHttpClient(), request, null));
    }

}
