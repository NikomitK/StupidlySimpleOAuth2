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
