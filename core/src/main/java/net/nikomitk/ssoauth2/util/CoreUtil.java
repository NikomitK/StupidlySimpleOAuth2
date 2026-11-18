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
import net.nikomitk.ssoauth2.dto.OAuth2FlowConfiguration;
import net.nikomitk.ssoauth2.dto.OAuth2TokenResponse;
import net.nikomitk.ssoauth2.exception.OAuth2RequestException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

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
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Function;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final public class CoreUtil {

    private static final SecureRandom DEFAULT_SECURE_RANDOM = new SecureRandom();
    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = createObjectMapper();
    /**
     * Default HTTP request timeout (15 seconds).
     */
    public static final Duration DEFAULT_HTTP_TIMEOUT = Duration.ofSeconds(15);
    /**
     * Default HTTP {@code User-Agent} header value.
     */
    public static final String DEFAULT_USER_AGENT = "StupidlySimpleOAuth2/" + Objects.requireNonNullElse(CoreUtil.class.getPackage()
                                                                                                                       .getImplementationVersion(), "dev");
    private static final Base64.Encoder DEFAULT_URL_ENCODER = Base64.getUrlEncoder()
                                                                    .withoutPadding();

    private static final class DefaultHttpClientHolder {

        static final HttpClient INSTANCE = HttpClient.newBuilder()
                                                     .connectTimeout(Duration.ofSeconds(30))
                                                     .build();

    }

    /**
     * Returns the shared, immutable HTTP client with the library's default connect timeout.
     *
     * @return the shared, immutable HTTP client with the library's default timeouts
     */
    @Contract(pure = true)
    public static @NotNull HttpClient defaultHttpClient() {

        return DefaultHttpClientHolder.INSTANCE;
    }

    /**
     * Creates a new ObjectMapper with the library's default configuration. Each call returns a new instance, so it can safely be customized.
     *
     * @return a new, default configured ObjectMapper
     */
    @Contract("-> new")
    public static @NotNull ObjectMapper createObjectMapper() {

        return new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Creates a request builder with the library's default timeout and User-Agent already set.
     *
     * @param uri the URI the request is sent to
     * @return a request builder to which the method, headers and body can be added
     */
    @Contract("_ -> new")
    public static @NotNull HttpRequest.Builder requestBuilder(@NonNull final URI uri) {

        return requestBuilder(uri, DEFAULT_HTTP_TIMEOUT, DEFAULT_USER_AGENT);
    }

    /**
     * Creates a request builder with the timeout and User-Agent from the given flow configuration.
     *
     * @param uri           the URI the request is sent to
     * @param configuration the flow configuration providing the HTTP timeout and User-Agent
     * @return a request builder to which the method, headers and body can be added
     */
    @Contract("_, _ -> new")
    public static @NotNull HttpRequest.Builder requestBuilder(@NonNull final URI uri, @NonNull final OAuth2FlowConfiguration configuration) {

        return requestBuilder(uri, configuration.httpTimeout(), configuration.userAgent());
    }

    /**
     * Creates a request builder with the given timeout and User-Agent (falling back to the library defaults when null).
     *
     * @param uri         the URI the request is sent to
     * @param httpTimeout the request timeout, or null to use {@link #DEFAULT_HTTP_TIMEOUT}
     * @param userAgent   the User-Agent header value, or null to use {@link #DEFAULT_USER_AGENT}
     * @return a request builder to which the method, headers and body can be added
     */
    @Contract("_, _, _ -> new")
    public static @NotNull HttpRequest.Builder requestBuilder(@NonNull final URI uri, @Nullable final Duration httpTimeout, @Nullable final String userAgent) {

        return HttpRequest.newBuilder(uri)
                          .timeout(Objects.requireNonNullElse(httpTimeout, DEFAULT_HTTP_TIMEOUT))
                          .header("User-Agent", Objects.requireNonNullElse(userAgent, DEFAULT_USER_AGENT));
    }

    public static void openBrowser(@NonNull final String url) throws UnsupportedOperationException, IOException {

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

    /** Create an HTTP Server that listens on the loopback address and the given port. The server will use a virtual thread executor.
     *
     * @param port the port to listen on. If the port is 0, a random free port will be used.
     * @return the created HTTP server.
     * @throws IOException If the bound port is already in use, an IOException will be thrown.
     */
    @Contract("_ -> new")
    public static @NotNull HttpServer createHttpServer(@Range(from = 0, to = 65535) final int port) throws IOException {

        final InetSocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);

        final HttpServer server = HttpServer.create(address, 2);
        server.setExecutor(Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
                                                                    .name("oauth2-redirect-", 0)
                                                                    .factory()));
        return server;
    }

    /**
     * Convenience method to send a response to an HttpExchange. The exchange will be closed after the response is sent.
     *
     * @param exchange the HttpExchange to send the response to.
     * @param responseCode the HTTP status code to send.
     * @param responseBody the response body to send. It will be encoded as UTF-8.
     * @throws IOException If an I/O error occurs while sending the response.
     */
    public static void sendResponse(@NonNull final HttpExchange exchange, final int responseCode, @NonNull final String responseBody) throws IOException {

        try (exchange) {
            final byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders()
                    .set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(responseCode, body.length);
            exchange.getResponseBody()
                    .write(body);

        }
    }

    /**
     * Generates a random string of the given byte count, encoded as a URL-safe Base64 string without padding.
     *
     * @param byteCount the number of random bytes to generate.
     * @return a random string of the given byte count, encoded as a URL-safe Base64 string without padding.
     * @throws IllegalArgumentException if byteCount is negative
     */
    public static @NotNull String generateRandomString(@Range(from = 0, to = Integer.MAX_VALUE) final int byteCount) {

        return generateRandomString(byteCount, DEFAULT_URL_ENCODER::encodeToString);
    }

    /**
     * Generates a random string of the given byte count, encoded using the provided encoder function.
     *
     * @param byteCount the number of random bytes to generate.
     * @param encoder the function to use for encoding the random bytes.
     * @return a random string of the given byte count, encoded using the provided encoder function.
     * @throws IllegalArgumentException if byteCount is negative.
     */
    public static String generateRandomString(@Range(from = 0, to = Integer.MAX_VALUE) final int byteCount, @NonNull final Function<byte[], String> encoder) {

        if (byteCount < 0) {
            throw new IllegalArgumentException("byteCount must be non-negative");
        }
        final byte[] bytes = new byte[byteCount];
        DEFAULT_SECURE_RANDOM.nextBytes(bytes);
        return encoder.apply(bytes);
    }

    /** Creates a ScheduledThreadPoolExecutor with a single thread, which is a daemon thread and has the name "oauth2-timeout". The executor is configured to remove cancelled tasks from the queue.
     *
     * @return a ScheduledThreadPoolExecutor with a single daemon thread named "oauth2-timeout".
     */
    @Contract("-> new")
    public static @NotNull ScheduledThreadPoolExecutor createScheduler() {

        final ScheduledThreadPoolExecutor s = new ScheduledThreadPoolExecutor(1, Thread.ofPlatform()
                                                                                       .name("oauth2-timeout")
                                                                                       .daemon()
                                                                                       .factory());
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
    @Contract("_, _, _ -> new")
    public static @NotNull OAuth2Endpoint queryWellKnownEndpoint(@NonNull final HttpClient client, @NonNull final String wellKnownEndpoint, @NonNull final ObjectMapper objectMapper) {

        try {
            final HttpRequest request = requestBuilder(URI.create(wellKnownEndpoint), null, null)
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
            throw new RuntimeException("Interrupted while querying well known endpoint", e);
        }
    }

    /**
     * Creates an OAuth2Endpoint by querying an OAuth2 well known endpoint.
     * @param wellKnownEndpoint a valid URI to the well known endpoint
     * @return an initialized instance of OAuth2Endpoint
     */
    @Contract("_ -> new")
    public static @NotNull OAuth2Endpoint queryWellKnownEndpoint(@NonNull final String wellKnownEndpoint) {

        return queryWellKnownEndpoint(defaultHttpClient(), wellKnownEndpoint, DEFAULT_OBJECT_MAPPER);
    }

    /**
     * Sends a token request using the default HTTP client and ObjectMapper.
     *
     * @param request the token request
     * @return the parsed token response
     */
    @Contract("_ -> new")
    public static @NotNull OAuth2TokenResponse requestToken(@NonNull final HttpRequest request) throws IOException, InterruptedException, OAuth2RequestException {

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
    @Contract("_, _, _ -> new")
    public static @NotNull OAuth2TokenResponse requestToken(@NonNull final HttpClient httpClient, @NonNull final HttpRequest request, @NonNull final ObjectMapper objectMapper) throws IOException, InterruptedException, OAuth2RequestException {

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
    @Contract("_ -> new")
    public static @NotNull OAuth2AccessToken createAccessToken(@NonNull final OAuth2TokenResponse tokenResponse) {

        final Instant validUntil;
        if (tokenResponse.expiresIn() > 0) {
            validUntil = tokenResponse.receivedAt()
                                      .plusSeconds(tokenResponse.expiresIn());
        } else {
            validUntil = Instant.MAX;
        }
        return new OAuth2AccessToken(tokenResponse.accessToken(), validUntil);
    }

    public static void checkRequestException(@NonNull final HttpResponse<String> response) throws OAuth2RequestException {

        if (response.statusCode() != 200) {
            throw new OAuth2RequestException(response);
        }
    }

    @Contract(pure = true)
    public static @NotNull String urlEncode(@NonNull final String value) {

        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

}
