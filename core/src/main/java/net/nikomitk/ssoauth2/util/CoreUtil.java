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
import java.net.URISyntaxException;
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

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final HttpClient DEFAULT_HTTP_CLIENT = HttpClient.newBuilder()
                                                                    .connectTimeout(Duration.ofSeconds(30))
                                                                    .build();
    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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
        // InetSocketAddress("127.0.0.1", 0);
        final HttpServer server = HttpServer.create();
        server.setExecutor(Executors.newThreadPerTaskExecutor(Thread.ofVirtual()
                                                                    .name("oauth2-redirect-", 0)
                                                                    .factory()));
        server.bind(address, 2);
        return server;
    }

    public static String getQueryParameter(final HttpExchange exchange, final String parameterName) {

        final String query = exchange.getRequestURI()
                                     .getQuery();
        if (query == null) {
            return null;
        }
        final String[] params = query.split("&");
        for (final String param : params) {
            if (param.startsWith(parameterName + "=")) {
                final String[] parts = param.split("=", 2);
                if (parts.length < 2) {
                    return "";
                }
                return parts[1];
            }
        }
        return null;
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
        secureRandom.nextBytes(bytes);
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
     * @param wellKnownEndpoint a valid URI to the well known endpoint
     * @return an initialized instance of OAuth2Endpoint
     */
    public static OAuth2Endpoint queryWellKnownEndpoint(final String wellKnownEndpoint) {

        try (final HttpClient client = HttpClient.newHttpClient()) {
            final HttpRequest request = HttpRequest.newBuilder()
                                                   .uri(java.net.URI.create(wellKnownEndpoint))
                                                   .GET()
                                                   .build();
            final var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to fetch well-known endpoint: " + response.statusCode());
            }
            return new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                                     .readValue(response.body(), OAuth2Endpoint.class);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        } catch (final InterruptedException e) {
            Thread.currentThread()
                  .interrupt();
            throw new RuntimeException(e);
        }
    }

    public static boolean isValidUri(final String uri) {

        try {
            new URI(uri);
            return true;
        } catch (final URISyntaxException e) {
            return false;
        }
    }

    public static OAuth2TokenResponse requestToken(HttpClient httpClient, @NonNull final HttpRequest request, ObjectMapper objectMapper) throws IOException, InterruptedException, OAuth2RequestException {

        if (httpClient == null) {
            httpClient = DEFAULT_HTTP_CLIENT;
        }
        if (objectMapper == null) {
            objectMapper = DEFAULT_OBJECT_MAPPER;
        }
        final var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        OAuth2RequestException.check(response);
        return objectMapper.readValue(response.body(), OAuth2TokenResponse.class);
    }

}
