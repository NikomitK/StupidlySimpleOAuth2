package net.nikomitk.ssoauth2.exception;

import lombok.Getter;
import net.nikomitk.ssoauth2.dto.OAuth2ErrorResponse;
import org.jetbrains.annotations.Nullable;

import java.net.http.HttpResponse;

/**
 * Indicates that a request to the OAuth2 authorization server has returned an error.
 */
@Getter
public class OAuth2RequestException extends RuntimeException {

    /**
     * The HTTP status code returned by the authorization server.
     */
    private final int statusCode;
    /**
     * The response body returned by the authorization server.
     */
    private final String body;

    /**
     * The parsed standardized OAuth2 error response.
     */
    @Nullable
    private final OAuth2ErrorResponse errorResponse;

    /**
     * Creates a new OAuth2RequestException with the given status code, response body, and parsed error response.
     *
     * @param statusCode    the HTTP status code
     * @param body          the error response body
     * @param errorResponse the parsed OAuth2 error response, or null if unavailable
     */
    public OAuth2RequestException(final int statusCode, final String body, @Nullable final OAuth2ErrorResponse errorResponse) {

        super(String.format("Error response from OAuth2 authorization server. Status code: %d. Error message: %s", statusCode, body));
        this.statusCode = statusCode;
        this.body = body;
        this.errorResponse = errorResponse;
    }

    /**
     * Creates a new OAuth2RequestException with the given status code and response body.
     *
     * @param statusCode the HTTP status code
     * @param body       the error response body
     */
    public OAuth2RequestException(final int statusCode, final String body) {

        this(statusCode, body, null);
    }

    /**
     * Creates a new OAuth2RequestException from the given HTTP response and parsed error response.
     *
     * @param response      the HTTP response returned by the authorization server
     * @param errorResponse the parsed OAuth2 error response, or null if unavailable
     */
    public OAuth2RequestException(final HttpResponse<String> response, @Nullable final OAuth2ErrorResponse errorResponse) {

        this(response.statusCode(), response.body(), errorResponse);
    }

    /**
     * Creates a new OAuth2RequestException from the given HTTP response.
     *
     * @param response the HTTP response returned by the authorization server
     */
    public OAuth2RequestException(final HttpResponse<String> response) {

        this(response, null);
    }

}
