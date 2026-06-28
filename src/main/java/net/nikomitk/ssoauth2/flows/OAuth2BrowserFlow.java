package net.nikomitk.ssoauth2.flows;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.nikomitk.ssoauth2.util.HttpFormBodyBuilder;
import net.nikomitk.ssoauth2.dto.OAuth2Endpoint;
import net.nikomitk.ssoauth2.OAuth2Flow;
import net.nikomitk.ssoauth2.dto.OAuth2TokenResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Class encapsulating the OAuth2 Browser Authentication flow.
 */
public class OAuth2BrowserFlow extends OAuth2Flow {

    private final AtomicReference<String> atomicAccessCode = new AtomicReference<>();
    private final OAuth2Endpoint endpoint;
    private final String clientID;
    private final String scope;
    private OAuth2TokenResponse lastOAuth2TokenResponse;
    private String refreshToken;

    /**
     * Create a new Instance of the OAuth2BrowserFlow.
     *
     * @param wellKnownEndpoint the well-known-endpoint of your OAuth2 provider. Usually ends with /.well-known/openid-configuration
     * @param clientID          the OAuth2 clientID of your public OAuth2 client
     * @param scope             space seperated strings, which should be requested from the OAuth2 provider. Example: "string1 string2 string3"
     */
    public OAuth2BrowserFlow(final String wellKnownEndpoint, final String clientID, final String scope) {

        this.endpoint = OAuth2Endpoint.queryWellKnownEndpoint(wellKnownEndpoint);
        this.clientID = clientID;
        this.scope = scope;
    }

    @Override
    public OAuth2BrowserFlow startFlow() {

        try {
            browserBasedAccessCodeFlow();
        } catch (final IOException | InterruptedException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * The main method of this class. It starts an http server, opens the created SSO url in the users browser, and blocks until it get's an http request. This is usually the redirect of the OAuth2 provider.
     *
     * @return the access code, as described in section 1.3.1 of RFC6749.
     * @throws IOException          if starting the http server throws an IOException
     * @throws InterruptedException if the http server gets interrupted
     * @throws URISyntaxException   if it can't construct a valid URI from the input parameters.
     */
    public String browserBasedAccessCodeFlow() throws IOException, InterruptedException, URISyntaxException {

        synchronized (atomicAccessCode) {
            startHttpServer();
            final String url = buildUrl();

            Runtime
                    .getRuntime()
                    .exec(new String[]{"xdg-open", url});
            atomicAccessCode.wait();
        }
        return atomicAccessCode.get();
    }

    private void startHttpServer() throws IOException {

        final HttpServer server = HttpServer.create(new java.net.InetSocketAddress(6767), 0);
        server.createContext("/oauth2/redirect", exchange -> {
            final String query = exchange
                    .getRequestURI()
                    .getQuery();
            final String[] params = query.split("&");
            String code = null;
            for (final String param : params) {
                if (param.startsWith("code=")) {
                    code = param.split("=")[1];
                    break;
                }
            }
            synchronized (atomicAccessCode) {

                if (code == null) {
                    sendResponse(exchange, 500, "An error occurred. You can now close this window.");
                    atomicAccessCode.notifyAll();
                } else if (!code.isBlank()) {
                    sendResponse(exchange, 200, "Authentication successful. You can now close this window.");
                }
                atomicAccessCode.set(code);
                atomicAccessCode.notifyAll();
            }
            server.stop(3);
        });
        server.start();
    }

    private String buildUrl() throws URISyntaxException {

        final String url = endpoint.getAuthorizationEndpoint() + "?response_type=code" + "&client_id=" + this.clientID + "&redirect_uri=http://localhost:6767/oauth2/redirect" + "&scope=" + this.scope;
        return url;
    }

    private static void sendResponse(final HttpExchange exchange, final int responseCode, final String responseBody) throws IOException {

        exchange.sendResponseHeaders(responseCode, responseBody.length());
        exchange
                .getResponseBody()
                .write(responseBody.getBytes());
        exchange
                .getResponseBody()
                .close();
        exchange.close();
    }

    /**
     * Requests an OAuth2 access token using the previously acquired access code, as described in section 4.1.3 of RFC6749.
     *
     * @return the OAuth2Interaction object, so you can chain calls
     */
    public OAuth2BrowserFlow requestAccessToken() {

        final String body = HttpFormBodyBuilder
                .newBuilder()
                .addFormData("grant_type", "authorization_code")
                .addFormData("scope", scope)
                .addFormData("code", atomicAccessCode.get())
                .addFormData("redirect_uri", "http://localhost:6767/oauth2/redirect")
                .addFormData("client_id", clientID)
                .build();

        final HttpRequest request = HttpRequest
                .newBuilder()
                .uri(URI.create(endpoint.getTokenEndpoint()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .method("POST", HttpRequest.BodyPublishers.ofString(body))
                .build();

        final HttpResponse<String> response;
        try (final HttpClient client = HttpClient.newHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            lastOAuth2TokenResponse = objectMapper.readValue(response.body(), OAuth2TokenResponse.class);
            this.refreshToken = lastOAuth2TokenResponse.getRefreshToken();
            return this;
        } catch (final IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Use the refresh token to refresh the access token.
     *
     * @return the OAuth2Interaction object, so you can chain calls
     * @throws IOException          If there is an IOException in the HttpClient.send method
     * @throws InterruptedException If the HttpClient.send method is interrupted
     */
    public OAuth2BrowserFlow refreshAccessToken() {

        final String body = HttpFormBodyBuilder
                .newBuilder()
                .addFormData("grant_type", "refresh_token")
                .addFormData("refresh_token", this.refreshToken)
                .addFormData("scope", scope)
                .addFormData("client_id", this.clientID)
                .build();
        final HttpRequest request = HttpRequest
                .newBuilder()
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

    @Override
    public String getAccessToken() {

        return this.lastOAuth2TokenResponse.getAccessToken();
    }

}
