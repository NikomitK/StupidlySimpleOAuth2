package net.nikomitk.ssoauth2.flows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.NonNull;
import net.nikomitk.ssoauth2.OAuth2Flow;
import net.nikomitk.ssoauth2.OAuth2FlowState;
import net.nikomitk.ssoauth2.dto.OAuth2AccessToken;
import net.nikomitk.ssoauth2.dto.OAuth2Endpoint;
import net.nikomitk.ssoauth2.dto.OAuth2TokenResponse;
import net.nikomitk.ssoauth2.exception.OAuth2FlowWrongStateException;
import net.nikomitk.ssoauth2.exception.OAuth2RequestException;
import net.nikomitk.ssoauth2.security.PKCE;
import net.nikomitk.ssoauth2.states.AuthorizedState;
import net.nikomitk.ssoauth2.util.HttpFormBodyBuilder;
import net.nikomitk.ssoauth2.util.UrlBuilder;
import net.nikomitk.ssoauth2.util.Util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;

import static net.nikomitk.ssoauth2.util.Util.sendResponse;

/**
 * Class encapsulating the OAuth2 Authorization Code flow.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class OAuth2AuthCodeFlow extends OAuth2Flow {

    public record Configuration(OAuth2Endpoint endpoint, String clientID, String scope, @NonNull PKCE pkce) {

        public Configuration(final String wellKnownEndpoint, final String clientID, final String scope, final PKCE pkce) {

            this(OAuth2Endpoint.queryWellKnownEndpoint(wellKnownEndpoint), clientID, scope, pkce);
        }

    }

    public sealed interface AuthCodeFlowState extends OAuth2FlowState {

        record Initial() implements AuthCodeFlowState {}

        record AwaitingAuthCode(HttpServer server, String redirectUri, String stateParam, String codeVerifier,
                String userUri) implements AuthCodeFlowState {}

        record AuthCodePresent(String authCode, String redirectUri, String codeVerifier) implements AuthCodeFlowState {}

        record AwaitingAccessToken(String authCode, String redirectUri,
                String codeVerifier) implements AuthCodeFlowState {}

        record AccessTokenPresent(OAuth2AccessToken accessToken) implements AuthCodeFlowState, AuthorizedState {

            @Override
            public OAuth2AccessToken getAccessToken() {

                return accessToken;
            }

        }

        record RefreshTokenPresent(String refreshToken,
                OAuth2AccessToken accessToken) implements AuthCodeFlowState, AuthorizedState {

            @Override
            public OAuth2AccessToken getAccessToken() {

                return accessToken;
            }

        }

        record RefreshingAccessToken(String refreshToken) implements AuthCodeFlowState {}

    }

    private final Configuration configuration;
    private volatile AuthCodeFlowState state;
    private final Object lock = new Object();

    public OAuth2AuthCodeFlow(final Configuration configuration) {

        this.configuration = configuration;
        this.state = new AuthCodeFlowState.Initial();
    }

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

        this.configuration = new Configuration(wellKnownEndpoint, clientID, scope, pkce);
        super();
    }

    /**
     * Returns the last URI that was used in the OAuth2 flow.
     *
     * @return the last URI
     */
    public String getAuthUri() {

        synchronized (lock) {
            if (state instanceof AuthCodeFlowState.AwaitingAuthCode awaitingAuthCode) {
                return awaitingAuthCode.userUri();
            } else {
                throw new OAuth2FlowWrongStateException(state.getClass()
                                                             .getSimpleName(), new String[]{"AwaitingAuthCode"});
            }
        }
    }

    /**
     * Starts an http server on the loopback interface and a random port. To get the URL the user has to open for
     * signing in, call the {@link #getAuthUri()} method.
     *
     * @return the instance this was called on
     */
    @Override
    public OAuth2AuthCodeFlow startFlow() {

        return startFlow(0);
    }

    /**
     * Requests an OAuth2 access token using the previously acquired access code, as described in section 4.1.3 of
     * RFC6749.
     *
     * @return the instance this was called on
     */
    public OAuth2AuthCodeFlow requestAccessToken() {

        synchronized (lock) {
            if (state instanceof AuthCodeFlowState.AuthCodePresent authCodePresent) {

                final String body = HttpFormBodyBuilder.newBuilder()
                                                       .addFormData("grant_type", "authorization_code")
                                                       .addFormData("scope", this.configuration.scope())
                                                       .addFormData("code", authCodePresent.authCode)
                                                       .addFormData("redirect_uri", authCodePresent.redirectUri)
                                                       .addFormData("client_id", configuration.clientID())
                                                       .addFormData("code_verifier", authCodePresent.codeVerifier())
                                                       .build();

                final HttpRequest request = HttpRequest.newBuilder()
                                                       .uri(URI.create(this.configuration.endpoint.getTokenEndpoint()))
                                                       .header("Content-Type", "application/x-www-form-urlencoded")
                                                       .method("POST", HttpRequest.BodyPublishers.ofString(body))
                                                       .build();

                try {

                    final OAuth2TokenResponse tokenResponse = requestToken(request, configuration, objectMapper);

                    if (tokenResponse.getRefreshToken() != null) {
                        state = new AuthCodeFlowState.RefreshTokenPresent(tokenResponse.getRefreshToken(), new OAuth2AccessToken(tokenResponse));
                    } else {
                        state = new AuthCodeFlowState.AccessTokenPresent(new OAuth2AccessToken(tokenResponse));
                    }

                    return this;
                } catch (final IOException | InterruptedException e) {
                    state = new AuthCodeFlowState.Initial();
                    throw new RuntimeException(e);
                }
            } else {
                throw new OAuth2FlowWrongStateException(state.getClass()
                                                             .getSimpleName(), new String[]{"AuthCodePresent"});
            }
        }
    }

    public OAuth2AuthCodeFlow refreshAccessToken() {

        synchronized (lock) {
            if (state instanceof AuthCodeFlowState.RefreshTokenPresent refreshTokenPresent) {

                final HttpRequest request = prepareRefreshRequest(refreshTokenPresent, configuration);
                try {
                    final OAuth2TokenResponse tokenResponse = requestToken(request, configuration, objectMapper);


                    String refreshToken = tokenResponse.getRefreshToken();
                    if (refreshToken == null) {
                        refreshToken = refreshTokenPresent.refreshToken();
                    }
                    state = new AuthCodeFlowState.RefreshTokenPresent(refreshToken, new OAuth2AccessToken(tokenResponse));

                } catch (final IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }


            } else {
                throw new OAuth2FlowWrongStateException(state.getClass()
                                                             .getSimpleName(), new String[]{"RefreshTokenPresent"});
            }
        }
        return this;
    }

    private static OAuth2TokenResponse requestToken(final HttpRequest request, final Configuration configuration, final ObjectMapper objectMapper) throws IOException, InterruptedException {

        try (final HttpClient client = HttpClient.newHttpClient()) {
            final var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            OAuth2RequestException.check(response);
            return objectMapper.readValue(response.body(), OAuth2TokenResponse.class);
        }
    }

    private static HttpRequest prepareRefreshRequest(final AuthCodeFlowState.RefreshTokenPresent refreshTokenPresent, final Configuration configuration) {

        final String body = HttpFormBodyBuilder.newBuilder()
                                               .addFormData("grant_type", "refresh_token")
                                               .addFormData("refresh_token", refreshTokenPresent.refreshToken())
                                               .addFormData("scope", configuration.scope())
                                               .addFormData("client_id", configuration.clientID())
                                               .build();

        return HttpRequest.newBuilder()
                          .uri(URI.create(configuration.endpoint.getTokenEndpoint()))
                          .header("Content-Type", "application/x-www-form-urlencoded")
                          .method("POST", HttpRequest.BodyPublishers.ofString(body))
                          .build();
    }

    private void redirectExchangeHandler(final HttpExchange exchange) throws IOException {

        synchronized (lock) {
            if (state instanceof AuthCodeFlowState.AwaitingAuthCode awaitingAuthCode) {

                final String code = Util.getQueryParameter(exchange, "code");
                final String stateParam = Util.getQueryParameter(exchange, "state");

                if (stateParam == null || !MessageDigest.isEqual(awaitingAuthCode.stateParam()
                                                                                 .getBytes(), stateParam.getBytes())) {
                    sendResponse(exchange, 400, "State mismatch. This might be a CSRF attack. You can now close this window.");
                    return;
                } else if (code != null && !code.isBlank()) {
                    sendResponse(exchange, 200, "Authentication successful. You can now close this window.");
                    state = new AuthCodeFlowState.AuthCodePresent(code, awaitingAuthCode.redirectUri(), awaitingAuthCode.codeVerifier());
                } else {
                    sendResponse(exchange, 500, "An error occurred. You can now close this window.");
                    state = new AuthCodeFlowState.Initial();
                }
                awaitingAuthCode.server()
                                .stop(0);

            } else {
                sendResponse(exchange, 500, "The current state is not AwaitingAuthCode. You can now close this window.");
                return;
            }
        }
    }

    private String buildRedirectUri(final InetSocketAddress address) {

        return "http://" + address.getAddress()
                                  .getHostAddress() + ":" + address.getPort() + "/oauth2/redirect";
    }

    private String buildUrl(final String redirectUri, final String codeVerifier, final String stateParam) {

        final UrlBuilder builder = UrlBuilder.withBasePath(this.configuration.endpoint()
                                                                             .getAuthorizationEndpoint())
                                             .addQueryParameter("response_type", "code")
                                             .addQueryParameter("client_id", this.configuration.clientID())
                                             .addQueryParameter("redirect_uri", redirectUri)
                                             .addQueryParameter("scope", this.configuration.scope())
                                             .addQueryParameter("state", stateParam)
                                             .addQueryParameter("code_challenge", this.configuration.pkce()
                                                                                                    .generateCodeChallenge(codeVerifier))
                                             .addQueryParameter("code_challenge_method", this.configuration.pkce()
                                                                                                           .getCodeChallengeMethod());

        return builder.build();
    }

    /**
     * Starts an http server on the loopback interface and a specified port. To get the URL the user has to open for
     * signing in, call the {@link #getAuthUri()} method.
     *
     * @param port the port to start the http server on
     * @return the instance this was called on
     */
    public OAuth2AuthCodeFlow startFlow(final int port) {

        synchronized (lock) {

            if (!(state instanceof AuthCodeFlowState.Initial)) {
                throw new OAuth2FlowWrongStateException(state.getClass()
                                                             .getSimpleName(), new String[]{"Initial"});
            }

            final HttpServer server;

            try {
                server = Util.createHttpServer(port);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }

            var redirectUri = buildRedirectUri(server.getAddress());
            var codeVerifier = PKCE.generateCodeVerifier(32);
            var stateParam = Util.generateRandomString(32);
            var userUri = buildUrl(redirectUri, codeVerifier, stateParam);
            server.createContext("/oauth2/redirect", this::redirectExchangeHandler);
            state = new AuthCodeFlowState.AwaitingAuthCode(server, redirectUri, stateParam, codeVerifier, userUri);
            server.start();

            return this;
        }
    }

    @Override
    public OAuth2AccessToken getAccessToken() {

        synchronized (lock) {
            if (state instanceof AuthorizedState authorizedState) {
                return authorizedState.getAccessToken();
            } else {
                throw new OAuth2FlowWrongStateException(state.getClass()
                                                             .getSimpleName(), new String[]{"AuthorizedState"});
            }
        }
    }

}
