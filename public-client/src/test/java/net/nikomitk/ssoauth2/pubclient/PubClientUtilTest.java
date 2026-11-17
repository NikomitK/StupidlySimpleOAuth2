package net.nikomitk.ssoauth2.pubclient;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import net.nikomitk.ssoauth2.dto.OAuth2Endpoint;
import net.nikomitk.ssoauth2.pubclient.authcodeflow.AuthCodeFlowConfiguration;
import net.nikomitk.ssoauth2.security.PKCE;
import net.nikomitk.ssoauth2.util.CoreUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PubClientUtilTest {

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

    private OAuth2Endpoint createEndpoint() {

        return new OAuth2Endpoint("https://issuer.example", server.url("/authorize")
                                                                  .toString(), server.url("/token")
                                                                                     .toString(), null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    void prepareRefreshRequestBuildsPostWithScope() throws Exception {

        server.enqueue(new MockResponse.Builder().body("{}")
                                                 .build());
        final PublicClientConfiguration config = new AuthCodeFlowConfiguration(createEndpoint(), "my-client", "openid profile", PKCE.S256);

        final HttpRequest request = PubClientUtil.prepareRefreshRequest("my-refresh-token", config);
        CoreUtil.defaultHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        final RecordedRequest recorded = server.takeRequest();
        assertEquals("POST", recorded.getMethod());
        assertEquals("/token", recorded.getTarget());
        assertEquals("application/x-www-form-urlencoded", recorded.getHeaders()
                                                                  .get("Content-Type"));

        final Set<String> formParams = Arrays.stream(recorded.getBody()
                                                             .utf8()
                                                             .split("&"))
                                             .collect(Collectors.toSet());
        assertEquals(Set.of("grant_type=refresh_token", "refresh_token=my-refresh-token", "scope=openid+profile", "client_id=my-client"), formParams);
    }

    @Test
    void prepareRefreshRequestOmitsNullScope() throws Exception {

        server.enqueue(new MockResponse.Builder().body("{}")
                                                 .build());
        final PublicClientConfiguration config = new AuthCodeFlowConfiguration(createEndpoint(), "my-client", null, PKCE.S256);

        final HttpRequest request = PubClientUtil.prepareRefreshRequest("my-refresh-token", config);
        CoreUtil.defaultHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        final RecordedRequest recorded = server.takeRequest();
        final Set<String> formParams = Arrays.stream(recorded.getBody()
                                                             .utf8()
                                                             .split("&"))
                                             .collect(Collectors.toSet());
        assertEquals(Set.of("grant_type=refresh_token", "refresh_token=my-refresh-token", "client_id=my-client"), formParams);
    }

}
