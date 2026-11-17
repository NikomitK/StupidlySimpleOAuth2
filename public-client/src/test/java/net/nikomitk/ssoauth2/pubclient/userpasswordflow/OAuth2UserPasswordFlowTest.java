package net.nikomitk.ssoauth2.pubclient.userpasswordflow;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import net.nikomitk.ssoauth2.dto.OAuth2AccessToken;
import net.nikomitk.ssoauth2.exception.OAuth2FlowInterruptedException;
import net.nikomitk.ssoauth2.exception.OAuth2FlowWrongStateException;
import net.nikomitk.ssoauth2.exception.OAuth2RequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
class OAuth2UserPasswordFlowTest {

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

    private void enqueueWellKnown() {

        final String wellKnownJson = """
                {
                  "issuer": "%s",
                  "authorization_endpoint": "%s",
                  "token_endpoint": "%s"
                }
                """.formatted(server.url(""), server.url("/authorize"), server.url("/token"));
        server.enqueue(new MockResponse.Builder().body(wellKnownJson)
                                                 .build());
    }

    private OAuth2UserPasswordFlow createFlow(final String scope) throws InterruptedException {

        enqueueWellKnown();
        final OAuth2UserPasswordFlow flow = new OAuth2UserPasswordFlow(server.url("/.well-known/openid-configuration")
                                                                             .toString(), "my-client", "alice", "secret123", scope);
        server.takeRequest(); // consume well-known request
        return flow;
    }

    @Test
    void operationsBeforeStartFlowThrowWrongStateException() throws InterruptedException {

        final OAuth2UserPasswordFlow flow = createFlow("openid");

        assertThrows(OAuth2FlowWrongStateException.class, flow::getAccessToken);
        assertThrows(OAuth2FlowWrongStateException.class, flow::requestAccessToken);
        assertThrows(OAuth2FlowWrongStateException.class, flow::refreshAccessToken);
    }

    @Test
    void getAccessTokenBeforeRequestAccessTokenThrowsWrongStateException() throws InterruptedException {

        final OAuth2UserPasswordFlow flow = createFlow("openid").startFlow();

        assertThrows(OAuth2FlowWrongStateException.class, flow::getAccessToken);
    }

    @Test
    void constructorRejectsNullRequiredArguments() {

        enqueueWellKnown();
        enqueueWellKnown();
        enqueueWellKnown();
        final String wellKnownUrl = server.url("/.well-known/openid-configuration")
                                          .toString();

        assertThrows(NullPointerException.class, () -> new OAuth2UserPasswordFlow(null, "my-client", "alice", "secret123", "openid"));
        assertThrows(NullPointerException.class, () -> new OAuth2UserPasswordFlow(wellKnownUrl, null, "alice", "secret123", "openid"));
        assertThrows(NullPointerException.class, () -> new OAuth2UserPasswordFlow(wellKnownUrl, "my-client", null, "secret123", "openid"));
        assertThrows(NullPointerException.class, () -> new OAuth2UserPasswordFlow(wellKnownUrl, "my-client", "alice", null, "openid"));
    }

    @Test
    void requestAccessTokenSendsFormAndParsesToken() throws InterruptedException {

        final OAuth2UserPasswordFlow flow = createFlow("openid profile").startFlow();
        server.enqueue(new MockResponse.Builder().body("""
                {
                  "access_token": "token-1",
                  "token_type": "Bearer",
                  "expires_in": 3600
                }
                """)
                                                 .build());

        final OAuth2AccessToken token = flow.requestAccessToken()
                                            .getAccessToken();

        assertNotNull(token);
        assertEquals("token-1", token.token());
        assertFalse(token.isExpired());

        final RecordedRequest request = server.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/token", request.getTarget());
        assertEquals("application/x-www-form-urlencoded", request.getHeaders()
                                                                 .get("Content-Type"));
        final Set<String> formParams = Arrays.stream(request.getBody()
                                                            .utf8()
                                                            .split("&"))
                                             .collect(Collectors.toSet());
        assertEquals(Set.of("grant_type=password", "username=alice", "password=secret123", "scope=openid+profile", "client_id=my-client"), formParams);
    }

    @Test
    void refreshAccessTokenRequestsNewToken() throws InterruptedException {

        final OAuth2UserPasswordFlow flow = createFlow(null);
        server.enqueue(new MockResponse.Builder().body("{\"access_token\":\"first\",\"token_type\":\"Bearer\",\"expires_in\":3600}")
                                                 .build());
        server.enqueue(new MockResponse.Builder().body("{\"access_token\":\"second\",\"token_type\":\"Bearer\",\"expires_in\":3600}")
                                                 .build());

        flow.startFlow()
            .requestAccessToken();
        assertEquals("first", flow.getAccessToken()
                                  .token());

        flow.refreshAccessToken();
        assertEquals("second", flow.getAccessToken()
                                   .token());
    }

    @Test
    void requestAccessTokenThrowsOnErrorResponse() throws InterruptedException {

        final OAuth2UserPasswordFlow flow = createFlow("openid").startFlow();
        server.enqueue(new MockResponse.Builder().code(400)
                                                 .body("{\"error\":\"invalid_grant\"}")
                                                 .build());

        assertThrows(OAuth2RequestException.class, flow::requestAccessToken);
    }

    @Test
    void requestAccessTokenThrowsUncheckedIoExceptionOnConnectionFailure() throws InterruptedException {

        final OAuth2UserPasswordFlow flow = createFlow("openid").startFlow();
        server.close();

        assertThrows(UncheckedIOException.class, flow::requestAccessToken);
    }

    @Test
    void requestAccessTokenRestoresInterruptFlagAndThrowsFlowInterruptedException() throws InterruptedException {

        final OAuth2UserPasswordFlow flow = createFlow("openid").startFlow();
        Thread.currentThread()
              .interrupt();
        try {
            assertThrows(OAuth2FlowInterruptedException.class, flow::requestAccessToken);
            assertTrue(Thread.currentThread()
                             .isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

}
