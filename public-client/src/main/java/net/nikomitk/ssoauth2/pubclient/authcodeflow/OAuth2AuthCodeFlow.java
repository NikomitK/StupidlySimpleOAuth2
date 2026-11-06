package net.nikomitk.ssoauth2.pubclient.authcodeflow;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.nikomitk.ssoauth2.dto.OAuth2AccessToken;
import net.nikomitk.ssoauth2.dto.OAuth2TokenResponse;
import net.nikomitk.ssoauth2.exception.OAuth2FlowInterruptedException;
import net.nikomitk.ssoauth2.exception.OAuth2FlowWrongStateException;
import net.nikomitk.ssoauth2.exception.OAuth2RequestException;
import net.nikomitk.ssoauth2.flows.OAuth2FlowBase;
import net.nikomitk.ssoauth2.flows.types.ResettableFlow;
import net.nikomitk.ssoauth2.pubclient.PubClientUtil;
import net.nikomitk.ssoauth2.pubclient.authcodeflow.states.*;
import net.nikomitk.ssoauth2.security.PKCE;
import net.nikomitk.ssoauth2.states.AuthorizedState;
import net.nikomitk.ssoauth2.util.HttpFormBodyBuilder;
import net.nikomitk.ssoauth2.util.UrlBuilder;
import net.nikomitk.ssoauth2.util.CoreUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Optional;


/**
 * Class encapsulating the OAuth2 Authorization Code flow.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class OAuth2AuthCodeFlow extends OAuth2FlowBase implements ResettableFlow {

    private final AuthCodeFlowConfiguration configuration;
    private final Object lock;

    /**
     * Create a new Instance of the OAuth2AuthCodeFlow.
     *
     * @param wellKnownEndpoint the well-known-endpoint of your OAuth2 provider. Usually ends with /
     *                          .well-known/openid-configuration
     * @param clientID          the OAuth2 clientID of your public OAuth2 client
     * @param scope             space separated strings, which should be requested from the OAuth2 provider. Example:
     *                          "string1 string2 string3"
     * @param pkce              the {@link PKCE} instance to use for the OAuth2 flow.
     */
    public OAuth2AuthCodeFlow(final String wellKnownEndpoint, final String clientID, final String scope, final PKCE pkce) {

        this(new AuthCodeFlowConfiguration(wellKnownEndpoint, clientID, scope, pkce));
    }

    /**
     * Create a new Instance of the OAuth2AuthCodeFlow.
     *
     * @param configuration an initialized configuration object
     */
    public OAuth2AuthCodeFlow(final AuthCodeFlowConfiguration configuration) {

        this(configuration, new Object());
    }

    private OAuth2AuthCodeFlow(final AuthCodeFlowConfiguration configuration, final Object lock) {

        super(lock, new Initial());
        this.lock = lock;
        this.configuration = configuration;
    }

    /**
     * Returns the URI that a user can use to authorize.
     *
     * @return the authorization URI
     */
    public String getAuthUri() {


        if (getState() instanceof final AwaitingAuthCode awaitingAuthCode) {
            return awaitingAuthCode.getUserUri();
        } else {
            throw new OAuth2FlowWrongStateException(getState().getClass()
                                                              .getSimpleName(), new String[]{"AwaitingAuthCode"});
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

        if (getState() instanceof final AwaitingAuthCode awaitingAuthCode) {
            try {
                awaitingAuthCode.waitUntilFinished();
                if (getState() instanceof Initial) {
                    throw new OAuth2FlowInterruptedException("Authorization timed out or was cancelled");
                }
            } catch (final InterruptedException e) {
                Thread.currentThread()
                      .interrupt();
                throw new RuntimeException(e);
            }
        }

        synchronized (lock) {
            if (getState() instanceof AuthCodePresent(final String authCode, final String redirectUri, final String codeVerifier)) {

                final String body = HttpFormBodyBuilder.newBuilder()
                                                       .addFormData("grant_type", "authorization_code")
                                                       .addFormData("code", authCode)
                                                       .addFormData("redirect_uri", redirectUri)
                                                       .addFormData("client_id", configuration.clientID())
                                                       .addFormData("code_verifier", codeVerifier)
                                                       .build();

                final HttpRequest request = HttpRequest.newBuilder()
                                                       .uri(URI.create(this.configuration.endpoint().getTokenEndpoint()))
                                                       .header("Content-Type", "application/x-www-form-urlencoded")
                                                       .method("POST", HttpRequest.BodyPublishers.ofString(body))
                                                       .timeout(Duration.ofSeconds(30))
                                                       .build();

                try {

                    final OAuth2TokenResponse tokenResponse = CoreUtil.requestToken(null, request, objectMapper);

                    if (tokenResponse.getRefreshToken() != null) {
                        transitionTo(new RefreshTokenPresent(tokenResponse.getRefreshToken(), new OAuth2AccessToken(tokenResponse)));
                    } else {
                        transitionTo(new AccessTokenPresent(new OAuth2AccessToken(tokenResponse)));
                    }

                    return this;
                } catch (final IOException e) {
                    transitionTo(new Initial());
                    throw new RuntimeException(e);
                } catch (final InterruptedException e) {
                    transitionTo(new Initial());
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                } catch (final OAuth2RequestException e) {
                    transitionTo(new Initial());
                    throw e;
                }

            } else {
                throw new OAuth2FlowWrongStateException(getState().getClass()
                                                                  .getSimpleName(), new String[]{"AuthCodePresent"});
            }
        }
    }

    public OAuth2AuthCodeFlow refreshAccessToken() {

        synchronized (lock) {
            if (getState() instanceof final RefreshTokenPresent refreshTokenPresent) {

                final HttpRequest request = PubClientUtil.prepareRefreshRequest(refreshTokenPresent, configuration);
                try {
                    final OAuth2TokenResponse tokenResponse = CoreUtil.requestToken(null, request, objectMapper);


                    final String refreshToken = Optional.of(tokenResponse.getRefreshToken()).orElse(refreshTokenPresent.refreshToken());

                    transitionTo(new RefreshTokenPresent(refreshToken, new OAuth2AccessToken(tokenResponse)));

                } catch (final IOException e) {
                    throw new RuntimeException(e);
                } catch (final InterruptedException e) {
                    Thread.currentThread()
                          .interrupt();
                    throw new RuntimeException(e);
                }


            } else {
                throw new OAuth2FlowWrongStateException(getState().getClass()
                                                                  .getSimpleName(), new String[]{"RefreshTokenPresent"});
            }
        }
        return this;
    }

    @Override
    public OAuth2AccessToken getAccessToken() {

        if (getState() instanceof final AuthorizedState authorizedState) {
            return authorizedState.getAccessToken();
        } else {
            throw new OAuth2FlowWrongStateException(getState().getClass()
                                                              .getSimpleName(), new String[]{"AuthorizedState"});
        }

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

            if (!(getState() instanceof Initial)) {
                throw new OAuth2FlowWrongStateException(getState().getClass()
                                                                  .getSimpleName(), new String[]{"Initial"});
            }

            final HttpServer server;

            try {
                server = CoreUtil.createHttpServer(port);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }

            final var redirectUri = buildRedirectUri(server.getAddress());
            final var codeVerifier = PKCE.generateCodeVerifier(32);
            final var stateParam = CoreUtil.generateRandomString(32);
            final var userUri = buildUrl(redirectUri, codeVerifier, stateParam);
            server.createContext("/oauth2/redirect", this::redirectExchangeHandler);
            transitionWithTimeout(new AwaitingAuthCode(server, redirectUri, stateParam, codeVerifier, userUri), Duration.ofSeconds(120), new Initial());
            server.start();

            return this;
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
                                             .addNullableQueryParameter("scope", this.configuration.scope())
                                             .addQueryParameter("state", stateParam)
                                             .addQueryParameter("code_challenge", this.configuration.pkce()
                                                                                                    .generateCodeChallenge(codeVerifier))
                                             .addQueryParameter("code_challenge_method", this.configuration.pkce()
                                                                                                           .getCodeChallengeMethod());

        return builder.build();
    }

    private void redirectExchangeHandler(final HttpExchange exchange) throws IOException {

        synchronized (lock) {
            if (getState() instanceof final AwaitingAuthCode awaitingAuthCode) {

                final String code = CoreUtil.getQueryParameter(exchange, "code");
                final String stateParam = CoreUtil.getQueryParameter(exchange, "state");

                if (stateParam == null || !MessageDigest.isEqual(awaitingAuthCode.getStateParam()
                                                                                 .getBytes(), stateParam.getBytes())) {
                    CoreUtil.sendResponse(exchange, 400, "State mismatch. This might be a CSRF attack. You can now close this window.");
                } else if (code != null && !code.isBlank()) {
                    CoreUtil.sendResponse(exchange, 200, "Authentication successful. You can now close this window.");
                    transitionTo(new AuthCodePresent(code, awaitingAuthCode.getRedirectUri(), awaitingAuthCode.getCodeVerifier()));
                } else {
                    CoreUtil.sendResponse(exchange, 500, "An error occurred. You can now close this window.");
                    transitionTo(new Initial());
                }

            } else {
                CoreUtil.sendResponse(exchange, 500, "The current state is not AwaitingAuthCode. You can now close this window.");
            }
        }
    }

    @Override
    public void reset() {

        synchronized (lock) {
            transitionTo(new Initial());
        }
    }


}
