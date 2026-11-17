package net.nikomitk.ssoauth2.pubclient.authcodeflow;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import net.nikomitk.ssoauth2.dto.OAuth2AccessToken;
import net.nikomitk.ssoauth2.dto.OAuth2Endpoint;
import net.nikomitk.ssoauth2.exception.OAuth2FlowInterruptedException;
import net.nikomitk.ssoauth2.exception.OAuth2FlowWrongStateException;
import net.nikomitk.ssoauth2.exception.OAuth2RequestException;
import net.nikomitk.ssoauth2.security.PKCE;
import net.nikomitk.ssoauth2.util.CoreUtil;
import net.nikomitk.ssoauth2.util.UrlUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuth2AuthCodeFlowTest {

    private MockWebServer server;
    private OAuth2AuthCodeFlow flow;

    @BeforeEach
    void setUp() throws IOException {

        server = new MockWebServer();
        server.start();

        final OAuth2Endpoint endpoint = new OAuth2Endpoint("https://issuer.example", server.url("/authorize")
                                                                                           .toString(), server.url("/token")
                                                                                                              .toString(), null, null, null, null, null, null, null, null, null, null);
        final AuthCodeFlowConfiguration config = new AuthCodeFlowConfiguration(endpoint, "test-client", "openid profile", PKCE.S256);
        flow = new OAuth2AuthCodeFlow(config);
    }

    @AfterEach
    void tearDown() {

        if (flow != null) {
            flow.reset();
        }
        server.close();
    }

    private HttpResponse<String> sendRedirectCallback(final String redirectUri, final String query) throws Exception {

        final URI uri = URI.create(redirectUri + (query != null ? "?" + query : ""));
        return CoreUtil.defaultHttpClient()
                       .send(HttpRequest.newBuilder(uri)
                                        .GET()
                                        .build(), HttpResponse.BodyHandlers.ofString());
    }

    private Map<String, String> parseFormBody(final String body) {

        return Arrays.stream(body.split("&"))
                     .map(pair -> pair.split("=", 2))
                     .collect(Collectors.toMap(parts -> parts[0], parts -> parts.length > 1 ? parts[1] : ""));
    }

    @Test
    void operationsInInitialStateThrowWrongStateException() {

        assertThrows(OAuth2FlowWrongStateException.class, () -> flow.getAuthUri());
        assertThrows(OAuth2FlowWrongStateException.class, () -> flow.getAccessToken());
        assertThrows(OAuth2FlowWrongStateException.class, () -> flow.requestAccessToken());
        assertThrows(OAuth2FlowWrongStateException.class, () -> flow.refreshAccessToken());
    }

    @Test
    void startFlowBuildsAuthorizationUrlWithPkceAndState() {

        flow.startFlow();

        final String authUri = flow.getAuthUri();
        final String rawQuery = URI.create(authUri)
                                   .getRawQuery();

        assertTrue(authUri.startsWith(server.url("/authorize")
                                            .toString() + "?"));
        assertEquals("code", UrlUtils.getQueryParameter(rawQuery, "response_type"));
        assertEquals("test-client", UrlUtils.getQueryParameter(rawQuery, "client_id"));
        assertEquals("openid profile", UrlUtils.getQueryParameter(rawQuery, "scope"));
        assertEquals("S256", UrlUtils.getQueryParameter(rawQuery, "code_challenge_method"));
        assertNotNull(UrlUtils.getQueryParameter(rawQuery, "state"));
        assertNotNull(UrlUtils.getQueryParameter(rawQuery, "code_challenge"));
        assertTrue(UrlUtils.getQueryParameter(rawQuery, "redirect_uri")
                           .matches("http://127\\.0\\.0\\.1:\\d+/oauth2/redirect"));
    }

    @Test
    void startFlowThrowsWhenAlreadyStartedUntilReset() {

        flow.startFlow();

        assertThrows(OAuth2FlowWrongStateException.class, () -> flow.startFlow());

        flow.reset();
        flow.startFlow();
        assertNotNull(flow.getAuthUri());
    }

    @Test
    void redirectCallbackRejectsMismatchedStateAndStaysInAwaitingAuthCode() throws Exception {

        flow.startFlow();
        final String rawQuery = URI.create(flow.getAuthUri())
                                   .getRawQuery();
        final String redirectUri = UrlUtils.getQueryParameter(rawQuery, "redirect_uri");

        final HttpResponse<String> badResponse = sendRedirectCallback(redirectUri, "code=abc&state=wrong-state");

        assertEquals(400, badResponse.statusCode());
        // still awaiting valid callback
        assertNotNull(flow.getAuthUri());
    }

    @Test
    void redirectCallbackWithMissingCodeResetsToInitialAndInterruptsWaitingCaller() throws Exception {

        flow.startFlow();
        final String rawQuery = URI.create(flow.getAuthUri())
                                   .getRawQuery();
        final String redirectUri = UrlUtils.getQueryParameter(rawQuery, "redirect_uri");
        final String state = UrlUtils.getQueryParameter(rawQuery, "state");

        Thread.ofVirtual()
              .start(() -> {
                  try {
                      sendRedirectCallback(redirectUri, "error=access_denied&state=" + state);
                  } catch (final Exception ignored) {
                  }
              });

        assertThrows(OAuth2FlowInterruptedException.class, () -> flow.requestAccessToken());
        assertThrows(OAuth2FlowWrongStateException.class, () -> flow.getAuthUri());
    }

    @Test
    void redirectCallbackRejectsNonGetMethodWithoutAbortingFlow() throws Exception {

        flow.startFlow();
        final String rawQuery = URI.create(flow.getAuthUri())
                                   .getRawQuery();
        final String redirectUri = UrlUtils.getQueryParameter(rawQuery, "redirect_uri");
        final String state = UrlUtils.getQueryParameter(rawQuery, "state");

        final HttpResponse<String> postResponse = CoreUtil.defaultHttpClient()
                                                          .send(HttpRequest.newBuilder(URI.create(redirectUri + "?state=" + state))
                                                                           .POST(HttpRequest.BodyPublishers.noBody())
                                                                           .build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(405, postResponse.statusCode());
        // Flow must still be in AwaitingAuthCode so the real GET redirect can succeed
        assertNotNull(flow.getAuthUri());
    }

    @Test
    void fullAuthCodeFlowWithRefreshTokenAndRefresh() throws Exception {

        flow.startFlow();
        final String rawQuery = URI.create(flow.getAuthUri())
                                   .getRawQuery();
        final String redirectUri = UrlUtils.getQueryParameter(rawQuery, "redirect_uri");
        final String state = UrlUtils.getQueryParameter(rawQuery, "state");
        final String expectedChallenge = UrlUtils.getQueryParameter(rawQuery, "code_challenge");

        final HttpResponse<String> callbackResponse = sendRedirectCallback(redirectUri, "code=auth-code-123&state=" + state);
        assertEquals(200, callbackResponse.statusCode());

        server.enqueue(new MockResponse.Builder().body("""
                {
                  "access_token": "access-1",
                  "token_type": "Bearer",
                  "expires_in": 3600,
                  "refresh_token": "refresh-1"
                }
                """)
                                                 .build());

        flow.requestAccessToken();

        final OAuth2AccessToken token1 = flow.getAccessToken();
        assertEquals("access-1", token1.token());

        final RecordedRequest tokenRequest = server.takeRequest();
        final Map<String, String> tokenForm = parseFormBody(tokenRequest.getBody()
                                                                        .utf8());
        assertEquals("authorization_code", tokenForm.get("grant_type"));
        assertEquals("auth-code-123", tokenForm.get("code"));
        assertEquals("test-client", tokenForm.get("client_id"));
        assertEquals(expectedChallenge, PKCE.S256.generateCodeChallenge(tokenForm.get("code_verifier")));

        // Refresh without new refresh_token in response preserves existing refresh_token
        server.enqueue(new MockResponse.Builder().body("""
                {
                  "access_token": "access-2",
                  "token_type": "Bearer",
                  "expires_in": 3600
                }
                """)
                                                 .build());

        flow.refreshAccessToken();
        assertEquals("access-2", flow.getAccessToken()
                                     .token());

        final RecordedRequest refreshRequest1 = server.takeRequest();
        final Map<String, String> refreshForm1 = parseFormBody(refreshRequest1.getBody()
                                                                              .utf8());
        assertEquals("refresh_token", refreshForm1.get("grant_type"));
        assertEquals("refresh-1", refreshForm1.get("refresh_token"));

        // Second refresh still uses preserved "refresh-1"
        server.enqueue(new MockResponse.Builder().body("""
                {
                  "access_token": "access-3",
                  "token_type": "Bearer",
                  "expires_in": 3600,
                  "refresh_token": "refresh-2"
                }
                """)
                                                 .build());

        flow.refreshAccessToken();
        assertEquals("access-3", flow.getAccessToken()
                                     .token());
    }

    @Test
    void authCodeFlowWithoutRefreshTokenTransitionsToAccessTokenPresent() throws Exception {

        flow.startFlow();
        final String rawQuery = URI.create(flow.getAuthUri())
                                   .getRawQuery();
        final String redirectUri = UrlUtils.getQueryParameter(rawQuery, "redirect_uri");
        final String state = UrlUtils.getQueryParameter(rawQuery, "state");

        sendRedirectCallback(redirectUri, "code=auth-code-123&state=" + state);

        server.enqueue(new MockResponse.Builder().body("""
                {
                  "access_token": "only-access",
                  "token_type": "Bearer",
                  "expires_in": 3600
                }
                """)
                                                 .build());

        flow.requestAccessToken();

        assertEquals("only-access", flow.getAccessToken()
                                        .token());
        assertThrows(OAuth2FlowWrongStateException.class, () -> flow.refreshAccessToken());
    }

    @Test
    void requestAccessTokenResetsToInitialOnTokenEndpointError() throws Exception {

        flow.startFlow();
        final String rawQuery = URI.create(flow.getAuthUri())
                                   .getRawQuery();
        final String redirectUri = UrlUtils.getQueryParameter(rawQuery, "redirect_uri");
        final String state = UrlUtils.getQueryParameter(rawQuery, "state");

        sendRedirectCallback(redirectUri, "code=bad-code&state=" + state);

        server.enqueue(new MockResponse.Builder().code(400)
                                                 .body("{\"error\":\"invalid_grant\"}")
                                                 .build());

        assertThrows(OAuth2RequestException.class, () -> flow.requestAccessToken());
        // Should be back in Initial state so startFlow() is allowed again
        flow.startFlow();
        assertNotNull(flow.getAuthUri());
    }

    @Test
    void refreshAccessTokenResetsToInitialOnTokenEndpointError() throws Exception {

        flow.startFlow();
        final String rawQuery = URI.create(flow.getAuthUri())
                                   .getRawQuery();
        final String redirectUri = UrlUtils.getQueryParameter(rawQuery, "redirect_uri");
        final String state = UrlUtils.getQueryParameter(rawQuery, "state");

        sendRedirectCallback(redirectUri, "code=auth-code-123&state=" + state);
        server.enqueue(new MockResponse.Builder().body("""
                {
                  "access_token": "access-1",
                  "token_type": "Bearer",
                  "expires_in": 3600,
                  "refresh_token": "refresh-1"
                }
                """)
                                                 .build());
        flow.requestAccessToken();

        server.enqueue(new MockResponse.Builder().code(400)
                                                 .body("{\"error\":\"invalid_grant\"}")
                                                 .build());

        assertThrows(OAuth2RequestException.class, () -> flow.refreshAccessToken());
        assertThrows(OAuth2FlowWrongStateException.class, () -> flow.getAccessToken());
        flow.startFlow();
        assertNotNull(flow.getAuthUri());
    }

}
