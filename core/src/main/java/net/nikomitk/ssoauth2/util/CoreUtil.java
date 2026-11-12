package net.nikomitk.ssoauth2.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import net.nikomitk.ssoauth2.dto.OAuth2AccessToken;
import net.nikomitk.ssoauth2.dto.OAuth2Endpoint;
import net.nikomitk.ssoauth2.dto.OAuth2TokenResponse;
import net.nikomitk.ssoauth2.exception.OAuth2RequestException;

import java.awt.Desktop;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Function;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CoreUtil {

    private static final SecureRandom DEFAULT_SECURE_RANDOM = new SecureRandom();
    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = createObjectMapper();
    public static final Duration DEFAULT_HTTP_TIMEOUT = Duration.ofSeconds(15);
    public static final String DEFAULT_USER_AGENT = "StupidlySimpleOAuth2/0.3";
    private static final Base64.Encoder DEFAULT_URL_ENCODER = Base64.getUrlEncoder()
                                                                    .withoutPadding();

    private static final class DefaultHttpClientHolder {

        static final HttpClient INSTANCE = HttpClient.newBuilder()
                                                     .connectTimeout(Duration.ofSeconds(30))
                                                     .build();

    }

    /**
    * @return the shared, immutable HTTP client with the library's default timeouts
    */
    public static HttpClient defaultHttpClient() {

        return DefaultHttpClientHolder.INSTANCE;
    }

    /**
     * Creates a new ObjectMapper with the library's default configuration. Each call returns a new instance, so it can safely be customized.
     *
     * @return a new, default configured ObjectMapper
     */
    public static ObjectMapper createObjectMapper() {

        return new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Creates a request builder with the library's default timeout and User-Agent already set.
     *
     * @param uri the URI the request is sent to
     * @return a request builder to which the method, headers and body can be added
     */
    public static HttpRequest.Builder requestBuilder(@NonNull final URI uri) {

        return HttpRequest.newBuilder(uri)
                          .timeout(DEFAULT_HTTP_TIMEOUT)
                          .header("User-Agent", DEFAULT_USER_AGENT);
    }

    public static void openBrowser(final String url) throws UnsupportedOperationException, IOException {

        final var osName = System.getProperty("os.name");

        if (Desktop.isDesktopSupported() && Desktop.getDesktop()
                                                   .isSupported(Desktop.Action.BROWSE)) {

            Desktop.getDesktop()
                   .browse(URI.create(url));
        } else if (osName != null && osName.equals("Linux")) {


            Runtime.getRuntime()
                   .exec(new String[]{"xdg-open", url});


        } else {
            throw new UnsupportedOperationException("Can not open browser on this platform");
        }
    }

    public static HttpServer createHttpServer(final int port) throws IOException {

        final InetSocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);

        final HttpServer server = HttpServer.create(address, 2);
        server.setExecutor(Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
                                                                    .name("oauth2-redirect-", 0)
                                                                    .factory()));
        return server;
    }

    public static void sendResponse(final HttpExchange exchange, final int responseCode, final String responseBody) throws IOException {

        try (exchange) {
            final byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseCode, body.length);

            exchange.getResponseBody()
                    .write(body);

        }
    }

    public static String generateRandomString(final int byteCount) {

        return generateRandomString(byteCount, DEFAULT_URL_ENCODER::encodeToString);
    }

    public static String generateRandomString(final int byteCount, final Function<byte[], String> encoder) {

        final byte[] bytes = new byte[byteCount];
        DEFAULT_SECURE_RANDOM.nextBytes(bytes);
        return encoder.apply(bytes);
    }

    public static ScheduledThreadPoolExecutor createScheduler() {

        final ScheduledThreadPoolExecutor s = new ScheduledThreadPoolExecutor(1, r -> {
            final Thread t = new Thread(r, "oauth2-timeout");
            t.setDaemon(true);
            return t;
        });
        s.setRemoveOnCancelPolicy(true);
        return s;
    }

    /**
     * Creates an OAuth2Endpoint by querying an OAuth2 well known endpoint.
     * @param client the HTTP client to use
     * @param wellKnownEndpoint a valid URI to the well known endpoint
     * @param objectMapper the ObjectMapper to use
     * @return an initialized instance of OAuth2Endpoint
     */
    public static OAuth2Endpoint queryWellKnownEndpoint(@NonNull final HttpClient client, @NonNull final String wellKnownEndpoint, @NonNull final ObjectMapper objectMapper) {

        try {
            final HttpRequest request = requestBuilder(URI.create(wellKnownEndpoint))
                                                                                     .GET()
                                                                                     .build();
            final var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            checkRequestException(response);
            return objectMapper
                               .readValue(response.body(), OAuth2Endpoint.class);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } catch (final InterruptedException e) {
            Thread.currentThread()
                  .interrupt();
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates an OAuth2Endpoint by querying an OAuth2 well known endpoint.
     * @param wellKnownEndpoint a valid URI to the well known endpoint
     * @return an initialized instance of OAuth2Endpoint
     */
    public static OAuth2Endpoint queryWellKnownEndpoint(final String wellKnownEndpoint) {

        return queryWellKnownEndpoint(defaultHttpClient(), wellKnownEndpoint, DEFAULT_OBJECT_MAPPER);
    }

    /**
     * Sends a token request using the default HTTP client and ObjectMapper.
     *
     * @param request the token request
     * @return the parsed token response
     */
    public static OAuth2TokenResponse requestToken(@NonNull final HttpRequest request) throws IOException, InterruptedException, OAuth2RequestException {

        return requestToken(defaultHttpClient(), request, DEFAULT_OBJECT_MAPPER);
    }

    /**
     * Sends a token request.
     *
     * @param httpClient the HTTP client to use
     * @param request the token request
     * @param objectMapper the ObjectMapper used to parse the response
     * @return the parsed token response
     */
    public static OAuth2TokenResponse requestToken(@NonNull final HttpClient httpClient, @NonNull final HttpRequest request, @NonNull final ObjectMapper objectMapper) throws IOException, InterruptedException, OAuth2RequestException {

        final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkRequestException(response);
        return objectMapper.readValue(response.body(), OAuth2TokenResponse.class);
    }

    /**
     * Creates an access token from a token response. The token is valid until the time the response was received plus the response's expiry.
     *
     * @param tokenResponse the parsed token response
     * @return the access token and the instant until it is valid
     */
    public static OAuth2AccessToken createAccessToken(@NonNull final OAuth2TokenResponse tokenResponse) {

        return new OAuth2AccessToken(tokenResponse.getAccessToken(), tokenResponse.getReceivedAt()
                                                                                  .plusSeconds(tokenResponse.getExpiresIn()));
    }

    public static void checkRequestException(final HttpResponse<String> response) throws OAuth2RequestException {

        if (response.statusCode() != 200) {
            throw new OAuth2RequestException(response);
        }
    }

    public static String urlEncode(final String value) {

        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

}
