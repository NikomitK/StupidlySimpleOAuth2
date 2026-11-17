package net.nikomitk.ssoauth2.pubclient.authcodeflow;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import net.nikomitk.ssoauth2.dto.OAuth2Endpoint;
import net.nikomitk.ssoauth2.security.PKCE;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthCodeFlowConfigurationTest {

    @Test
    void constructorRejectsNullClientIdAndPkce() {

        final OAuth2Endpoint endpoint = new OAuth2Endpoint("https://issuer.example", "https://issuer.example/authorize", "https://issuer.example/token", null, null, null, null, null, null, null, null, null, null);

        assertThrows(NullPointerException.class, () -> new AuthCodeFlowConfiguration(endpoint, null, "openid", PKCE.S256));
        assertThrows(NullPointerException.class, () -> new AuthCodeFlowConfiguration(endpoint, "client-id", "openid", null));
    }

    @Test
    void wellKnownConstructorQueriesEndpoint() throws IOException {

        try (final MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse.Builder().body("""
                    {
                      "issuer": "https://issuer.example",
                      "authorization_endpoint": "https://issuer.example/authorize",
                      "token_endpoint": "https://issuer.example/token"
                    }
                    """)
                                                     .build());

            final AuthCodeFlowConfiguration config = new AuthCodeFlowConfiguration(server.url("/.well-known/openid-configuration")
                                                                                         .toString(), "client-id", "openid", PKCE.S256);

            assertEquals("https://issuer.example/authorize", config.endpoint()
                                                                   .getAuthorizationEndpoint());
            assertEquals("https://issuer.example/token", config.endpoint()
                                                               .getTokenEndpoint());
            assertEquals("client-id", config.clientID());
            assertEquals("openid", config.scope());
            assertEquals(PKCE.S256, config.pkce());
        }
    }

}
