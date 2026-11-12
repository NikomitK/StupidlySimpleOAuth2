package net.nikomitk.ssoauth2.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.nikomitk.ssoauth2.dto.OAuth2AccessToken;
import net.nikomitk.ssoauth2.dto.OAuth2Endpoint;
import net.nikomitk.ssoauth2.dto.OAuth2TokenResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.net.BindException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreUtilTest {

    private HttpServer server;
    private ScheduledThreadPoolExecutor scheduler;

    @AfterEach
    void cleanUp() {

        if (server != null) {
            server.stop(0);
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    // generateRandomString

    @ParameterizedTest
    @CsvSource({"0, 0", "32, 43", "96, 128"})
    void generateRandomStringReturnsUnpaddedBase64UrlOfByteCount(final int byteCount, final int expectedLength) {

        assertEquals(expectedLength, CoreUtil.generateRandomString(byteCount)
                                             .length());
    }

    @Test
    void generateRandomStringUsesOnlyUrlSafeCharacters() {

        assertTrue(CoreUtil.generateRandomString(96)
                           .matches("[A-Za-z0-9_-]*"));
    }

    @Test
    void generateRandomStringReturnsDifferentValuesOnEachCall() {

        assertNotEquals(CoreUtil.generateRandomString(32), CoreUtil.generateRandomString(32));
    }

    @Test
    void generateRandomStringPassesRandomBytesOfRequestedLengthToEncoder() {

        final String result = CoreUtil.generateRandomString(16, bytes -> "length=" + bytes.length);

        assertEquals("length=16", result);
    }

    // defaultHttpClient / createObjectMapper / requestBuilder

    @Test
    void defaultHttpClientReturnsSharedInstanceWithConnectTimeout() {

        final HttpClient client = CoreUtil.defaultHttpClient();

        assertSame(client, CoreUtil.defaultHttpClient());
        assertEquals(Optional.of(Duration.ofSeconds(30)), client.connectTimeout());
    }

    @Test
    void createObjectMapperReturnsNewInstanceOnEachCall() {

        assertNotSame(CoreUtil.createObjectMapper(), CoreUtil.createObjectMapper());
    }

    @Test
    void createObjectMapperIgnoresUnknownProperties() throws IOException {

        final ObjectMapper mapper = CoreUtil.createObjectMapper();

        final OAuth2Endpoint endpoint = mapper.readValue("{\"issuer\": \"https://issuer.example\", \"unknown\": 1}", OAuth2Endpoint.class);

        assertEquals("https://issuer.example", endpoint.getIssuer());
    }

    @Test
    void requestBuilderSetsUriDefaultTimeoutAndUserAgent() {

        final URI uri = URI.create("https://issuer.example/token");

        final HttpRequest request = CoreUtil.requestBuilder(uri)
                                            .GET()
                                            .build();

        assertEquals(uri, request.uri());
        assertEquals(Optional.of(CoreUtil.DEFAULT_HTTP_TIMEOUT), request.timeout());
        assertEquals(Optional.of(CoreUtil.DEFAULT_USER_AGENT), request.headers()
                                                                      .firstValue("User-Agent"));
    }

    @Test
    void requestBuilderRejectsNullUri() {

        assertThrows(NullPointerException.class, () -> CoreUtil.requestBuilder(null));
    }

    // createAccessToken

    @Test
    void createAccessTokenIsValidUntilReceivedAtPlusExpiresIn() {

        final OAuth2TokenResponse response = new OAuth2TokenResponse("access", "Bearer", 3600, "refresh");

        final OAuth2AccessToken token = CoreUtil.createAccessToken(response);

        assertEquals("access", token.token());
        assertEquals(response.receivedAt()
                             .plusSeconds(3600), token.validUntil());
    }

    @Test
    void createAccessTokenRejectsNullResponse() {

        assertThrows(NullPointerException.class, () -> CoreUtil.createAccessToken(null));
    }

    // createScheduler

    @Test
    void createSchedulerRunsTasksOnNamedDaemonThread() throws Exception {

        scheduler = CoreUtil.createScheduler();

        final Thread thread = scheduler.schedule(Thread::currentThread, 0, TimeUnit.MILLISECONDS)
                                       .get(5, TimeUnit.SECONDS);

        assertEquals("oauth2-timeout", thread.getName());
        assertTrue(thread.isDaemon());
    }

    @Test
    void createSchedulerRemovesCancelledTasksFromQueue() {

        scheduler = CoreUtil.createScheduler();

        assertTrue(scheduler.getRemoveOnCancelPolicy());
    }

    // createHttpServer / sendResponse

    private HttpServer startServer(final int port) throws IOException {

        final HttpServer started = CoreUtil.createHttpServer(port);
        started.start();
        return started;
    }

    private URI serve(final HttpHandler handler) throws IOException {

        server = startServer(0);
        server.createContext("/test", handler);
        return URI.create("http://127.0.0.1:" + server.getAddress()
                                                      .getPort() + "/test");
    }

    @Test
    void createHttpServerBindsToLoopbackWithAssignedPort() throws IOException {

        server = startServer(0);

        assertTrue(server.getAddress()
                         .getAddress()
                         .isLoopbackAddress());
        assertTrue(server.getAddress()
                         .getPort() > 0);
    }

    @Test
    void createHttpServerFailsIfPortIsAlreadyInUse() throws IOException {

        server = startServer(0);
        final int usedPort = server.getAddress()
                                   .getPort();

        assertThrows(BindException.class, () -> CoreUtil.createHttpServer(usedPort));
    }

    @Test
    void sendResponseWritesStatusAndBody() throws Exception {

        final URI uri = serve(exchange -> CoreUtil.sendResponse(exchange, 201, "hello"));

        final HttpResponse<String> response = CoreUtil.defaultHttpClient()
                                                      .send(HttpRequest.newBuilder(uri)
                                                                       .build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        assertEquals("hello", response.body());
    }

    @Test
    void sendResponseHandlesNonAsciiBody() throws Exception {

        final URI uri = serve(exchange -> CoreUtil.sendResponse(exchange, 200, "äöü€"));

        final HttpResponse<String> response = CoreUtil.defaultHttpClient()
                                                      .send(HttpRequest.newBuilder(uri)
                                                                       .build(), HttpResponse.BodyHandlers.ofString());

        assertEquals("äöü€", response.body());
    }

}
