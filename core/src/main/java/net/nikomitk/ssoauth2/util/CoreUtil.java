package net.nikomitk.ssoauth2.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.NonNull;
import net.nikomitk.ssoauth2.dto.OAuth2Endpoint;
import net.nikomitk.ssoauth2.dto.OAuth2TokenResponse;
import net.nikomitk.ssoauth2.exception.OAuth2RequestException;

import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Function;

public class CoreUtil {

    private static final SecureRandom DEFAULT_SECURE_RANDOM = new SecureRandom();
    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = createObjectMapper();
    public static final Duration DEFAULT_HTTP_TIMEOUT = Duration.ofSeconds(15);
    public static final String DEFAULT_USER_AGENT = "StupidlySimpleOAuth2/0.3";

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

        final InetSocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);//new

        final HttpServer server = HttpServer.create();
        server.setExecutor(Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
                                                                    .name("oauth2-redirect-", 0)
                                                                    .factory()));
        server.bind(address, 2);
        return server;
    }

    public static void sendResponse(final HttpExchange exchange, final int responseCode, final String responseBody) throws IOException {

        exchange.sendResponseHeaders(responseCode, responseBody.length());

        exchange.getResponseBody()
                .write(responseBody.getBytes());

        exchange.getResponseBody()
                .close();
        exchange.close();
    }

    public static String generateRandomString(final int byteCount) {

        return generateRandomString(byteCount, Base64.getUrlEncoder()
                                                     .withoutPadding()::encodeToString);
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
            final HttpRequest request = HttpRequest.newBuilder()
                                                   .uri(java.net.URI.create(wellKnownEndpoint))
                                                   .GET()
                                                   .build();
            final var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to fetch well-known endpoint: " + response.statusCode());
            }
            return objectMapper
                               .readValue(response.body(), OAuth2Endpoint.class);
        } catch (final IOException e) {
            throw new RuntimeException(e);
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
        OAuth2RequestException.check(response);
        return objectMapper.readValue(response.body(), OAuth2TokenResponse.class);
    }

}
