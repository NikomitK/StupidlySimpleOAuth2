package net.nikomitk.ssoauth2.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.Function;

public class Util {

    private static final SecureRandom secureRandom = new SecureRandom();

    public static void openBrowser(final String url) throws UnsupportedOperationException, IOException {

        final var osName = System.getProperty("os.name");

        if (Desktop.isDesktopSupported() && Desktop.getDesktop()
                                                   .isSupported(Desktop.Action.BROWSE)) {

            Desktop.getDesktop()
                   .browse(URI.create(url));
        } else if (osName != null && osName.equals("Linux")) {

            try {
                final var p = Runtime.getRuntime()
                                     .exec(new String[]{"xdg-open", url});
                p.waitFor();
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
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
        String requestedParam = null;
        for (final String param : params) {
            if (param.startsWith(parameterName + "=")) {
                final String[] parts = param.split("=");
                if (parts.length < 2) {
                    return "";
                } else if (parts.length > 2) {
                    return parts[1];
                }
                requestedParam = param.split("=")[1];
                break;
            }
        }
        return requestedParam;
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

}
