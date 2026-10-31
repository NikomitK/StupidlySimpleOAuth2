package net.nikomitk.ssoauth2.util;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.naming.OperationNotSupportedException;
import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;

public class Util {

    public static void openBrowser(final String url) throws OperationNotSupportedException, IOException {

        final var osName = System.getProperty("os.name");

        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {

            Desktop.getDesktop().browse(URI.create(url));
        } else if (osName != null && osName.equals("Linux")) {

            try {
                final var p = Runtime.getRuntime().exec(new String[]{"xdg-open", url});
                p.waitFor();
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new OperationNotSupportedException();
        }
    }

    public static HttpServer createHttpServer(final int port) throws IOException {

        final InetSocketAddress address = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);//new
        // InetSocketAddress("127.0.0.1", 0);
        final HttpServer server = HttpServer.create();
        server.bind(address, 2);
        return server;
    }

    public static String getQueryParameter(final HttpExchange exchange, final String parameterName) {

        final String query = exchange.getRequestURI().getQuery();
        final String[] params = query.split("&");
        String requestedParam = null;
        for (final String param : params) {
            if (param.startsWith(parameterName + "=")) {
                requestedParam = param.split("=")[1];
                break;
            }
        }
        return requestedParam;
    }

    public static void sendResponse(final HttpExchange exchange, final int responseCode, final String responseBody) throws IOException {

        exchange.sendResponseHeaders(responseCode, responseBody.length());

        exchange.getResponseBody().write(responseBody.getBytes());

        exchange.getResponseBody().close();
        exchange.close();
    }

}
