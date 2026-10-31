package net.nikomitk.ssoauth2.flows;

import net.nikomitk.ssoauth2.OAuth2Flow;
import net.nikomitk.ssoauth2.dto.OAuth2Endpoint;
import net.nikomitk.ssoauth2.dto.OAuth2TokenResponse;
import net.nikomitk.ssoauth2.exception.OAuth2FlowNotInitializedException;
import net.nikomitk.ssoauth2.util.HttpFormBodyBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Class encapsulating authorization using the Resource Owner Password Credentials Grant described in section 4.3 of RFC6749.
 */
@SuppressWarnings("unused")
public class OAuth2UserPasswordFlow extends OAuth2Flow {

    private final OAuth2Endpoint endpoint;
    private final String clientID;
    private final String username;
    private final String password;
    private final String scope;
    private HttpRequest httpRequest;

    /**
     * Creates an instance of the OAuth2 <a href="https://www.rfc-editor.org/info/rfc6749/#section-4.3">Resource Owner Password Credentials flow</a>. This <a href="https://www.rfc-editor.org/info/rfc9700/#section-2.4">should not be used</a> for production purposes.
     * It is still implemented here for testing purposes and because some OAuth2 Authorization Server implementations (e.g. <a href="https://docs.goauthentik.io/add-secure-apps/providers/oauth2/machine_to_machine/">Authentik</a>) change the security assumptions by only allowing app passwords.
     * @param wellKnownEndpoint the well-known-endpoint of your OAuth2 provider. Usually ends with /
     *                          .well-known/openid-configuration
     * @param clientID          the OAuth2 clientID of your public OAuth2 client
     * @param scope             space separated strings, which should be requested from the OAuth2 provider. Example:
     *                         "string1 string2 string3"
     * @param username          the username to use
     * @param password          the password to use
     */
    public OAuth2UserPasswordFlow(final String wellKnownEndpoint, final String clientID, final String username, final String password, final String scope) {

        super();
        this.endpoint = OAuth2Endpoint.queryWellKnownEndpoint(wellKnownEndpoint);
        this.clientID = clientID;
        this.username = username;
        this.password = password;
        this.scope = scope;
    }

    @Override
    public OAuth2UserPasswordFlow startFlow() {

        final String body = HttpFormBodyBuilder
                .newBuilder()
                .addFormData("grant_type", "password")
                .addFormData("username", this.username)
                .addFormData("password", this.password)
                .addFormData("scope", scope)
                .addFormData("client_id", this.clientID)
                .build();

        this.httpRequest = HttpRequest
                .newBuilder()
                .uri(URI.create(endpoint.getTokenEndpoint()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .method("POST", HttpRequest.BodyPublishers.ofString(body))
                .build();

        return this;
    }

    @Override
    public OAuth2UserPasswordFlow requestAccessToken() {

        if (this.httpRequest == null) {
            throw new OAuth2FlowNotInitializedException();
        }
        try (final HttpClient client = HttpClient.newHttpClient()) {
            final var response = client.send(this.httpRequest, HttpResponse.BodyHandlers.ofString());
            lastOAuth2TokenResponse = objectMapper.readValue(response.body(), OAuth2TokenResponse.class);
        } catch (final IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @Override
    public OAuth2Flow refreshAccessToken() {

        return this.requestAccessToken();
    }

}
