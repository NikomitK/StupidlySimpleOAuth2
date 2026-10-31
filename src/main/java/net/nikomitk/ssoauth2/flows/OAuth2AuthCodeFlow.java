package net.nikomitk.ssoauth2.flows;

import com.sun.net.httpserver.HttpServer;
import net.nikomitk.ssoauth2.OAuth2Flow;
import net.nikomitk.ssoauth2.dto.OAuth2Endpoint;
import net.nikomitk.ssoauth2.dto.OAuth2TokenResponse;
import net.nikomitk.ssoauth2.exception.OAuth2FlowInterruptedException;
import net.nikomitk.ssoauth2.exception.OAuth2FlowNotInitializedException;
import net.nikomitk.ssoauth2.security.PKCE;
import net.nikomitk.ssoauth2.util.HttpFormBodyBuilder;
import net.nikomitk.ssoauth2.util.UrlBuilder;
import net.nikomitk.ssoauth2.util.Util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.nikomitk.ssoauth2.util.Util.sendResponse;

/**
 * Class encapsulating the OAuth2 Authorization Code flow.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class OAuth2AuthCodeFlow extends OAuth2Flow {

    private final PKCE pkce;
    private final OAuth2Endpoint endpoint;
    private final String clientID;
    private final String scope;
    private CompletableFuture<String> futureAccessCode;
    private String refreshToken;
    private String lastCodeVerifier;

    /**
     * Returns the last URI that was used in the OAuth2 flow.
     *
     * @return the last URI
     */
    public String getAuthUri() {

        return lastUri;
    }

    private String lastUri;
    private String lastRedirectUri;

    /**
     * Create a new Instance of the OAuth2BrowserFlow.
     *
     * @param wellKnownEndpoint the well-known-endpoint of your OAuth2 provider. Usually ends with /
     *                          .well-known/openid-configuration
     * @param clientID          the OAuth2 clientID of your public OAuth2 client
     * @param scope             space separated strings, which should be requested from the OAuth2 provider. Example:
     *                          "string1 string2 string3"
     * @param pkce              the {@link PKCE} instance to use for the OAuth2 flow. If it is null, the flow does
     *                          not use PKCE. This is not recommended.
     */
    public OAuth2AuthCodeFlow(final String wellKnownEndpoint, final String clientID, final String scope, final PKCE pkce) {

        this.endpoint = OAuth2Endpoint.queryWellKnownEndpoint(wellKnownEndpoint);
        this.clientID = clientID;
        this.scope = scope;
        this.pkce = pkce;
        super();
    }

    /**
     * Starts an http server on the loopback interface and a random port. To get the URL the user has to open for
     * signing in, call the {@link #getAuthUri()} method.
     * @return the instance this was called on
     */
    @Override
    public OAuth2AuthCodeFlow startFlow() {

        try {
            startHttpServer(0);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * Requests an OAuth2 access token using the previously acquired access code, as described in section 4.1.3 of
     * RFC6749.
     * @return the instance this was called on
     */
    public OAuth2AuthCodeFlow requestAccessToken() {

        final String accessCode;
        try {

            accessCode = futureAccessCode.get(120000, TimeUnit.MILLISECONDS);

        } catch (final InterruptedException | ExecutionException | TimeoutException e) {
            throw new OAuth2FlowNotInitializedException();
        }

        final HttpFormBodyBuilder bodyBuilder = HttpFormBodyBuilder.newBuilder()
                                                                   .addFormData("grant_type", "authorization_code")
                                                                   .addFormData("scope", scope)
                                                                   .addFormData("code", accessCode)
                                                                   .addFormData("redirect_uri", lastRedirectUri)
                                                                   .addFormData("client_id", clientID);

        if (this.pkce != null) {
            bodyBuilder.addFormData("code_verifier", lastCodeVerifier);
        }
        final String body = bodyBuilder.build();

        final HttpRequest request = HttpRequest.newBuilder()
                                               .uri(URI.create(endpoint.getTokenEndpoint()))
                                               .header("Content-Type", "application/x-www-form-urlencoded")
                                               .method("POST", HttpRequest.BodyPublishers.ofString(body))
                                               .build();

        try (final HttpClient client = HttpClient.newHttpClient()) {

            final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            lastOAuth2TokenResponse = objectMapper.readValue(response.body(), OAuth2TokenResponse.class);
            this.refreshToken = lastOAuth2TokenResponse.getRefreshToken();

            return this;
        } catch (final IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    public OAuth2AuthCodeFlow refreshAccessToken() {

        if (this.refreshToken == null) {
            throw new OAuth2FlowNotInitializedException();
        }

        final String body = HttpFormBodyBuilder.newBuilder()
                                               .addFormData("grant_type", "refresh_token")
                                               .addFormData("refresh_token", this.refreshToken)
                                               .addFormData("scope", scope)
                                               .addFormData("client_id", this.clientID)
                                               .build();

        final HttpRequest request = HttpRequest.newBuilder()
                                               .uri(URI.create(endpoint.getTokenEndpoint()))
                                               .header("Content-Type", "application/x-www-form-urlencoded")
                                               .method("POST", HttpRequest.BodyPublishers.ofString(body))
                                               .build();

        try (final HttpClient client = HttpClient.newHttpClient()) {
            final var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            this.lastOAuth2TokenResponse = objectMapper.readValue(response.body(), OAuth2TokenResponse.class);
        } catch (final IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    private void startHttpServer(final int port) throws IOException {

        futureAccessCode = new CompletableFuture<>();

        final HttpServer server = Util.createHttpServer(port);

        server.createContext("/oauth2/redirect", exchange -> {

            final String code = Util.getQueryParameter(exchange, "code");

            if (code != null && !code.isBlank()) {
                sendResponse(exchange, 200, "Authentication successful. You can now close this window.");
                futureAccessCode.complete(code);
            } else {
                sendResponse(exchange, 500, "An error occurred. You can now close this window.");
                futureAccessCode.completeExceptionally(new OAuth2FlowInterruptedException("No access code found in " + "the redirect URL."));
            }

            server.stop(1);
        });
        server.start();
        lastRedirectUri = buildRedirectUri(server.getAddress());
        lastUri = buildUrl(lastRedirectUri);
    }

    private String buildRedirectUri(final InetSocketAddress address) {

        return "http://" + address.getAddress()
                                  .getHostAddress() + ":" + address.getPort() + "/oauth2/redirect";
    }

    private String buildUrl(final String redirectUri) {

        final UrlBuilder builder = UrlBuilder.withBasePath(endpoint.getAuthorizationEndpoint())
                                             .addQueryParameter("response_type", "code")
                                             .addQueryParameter("client_id", this.clientID)
                                             .addQueryParameter("redirect_uri", redirectUri)
                                             .addQueryParameter("scope", this.scope);

        if (pkce != null) {

            lastCodeVerifier = PKCE.generateCodeVerifier(32);

            builder.addQueryParameter("code_challenge", pkce.generateCodeChallenge(lastCodeVerifier))
                   .addQueryParameter("code_challenge_method", pkce.getCodeChallengeMethod());
        }

        return builder.build();
    }

    /**
     * Starts an http server on the loopback interface and a specified port. To get the URL the user has to open for
     * signing in, call the {@link #getAuthUri()} method.
     * @param port the port to start the http server on
     * @return the instance this was called on
     */
    public OAuth2AuthCodeFlow startFlow(final int port) {

        try {
            startHttpServer(port);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }


}
