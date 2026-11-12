package net.nikomitk.ssoauth2.exception;

import lombok.Getter;

import java.net.http.HttpResponse;

/**
 * Indicates that a request to the OAuth2 authorization server has returned an error.
 */
@Getter
public class OAuth2RequestException extends RuntimeException {

    private final int statusCode;
    private final String body;

    public OAuth2RequestException(final int statusCode, final String body) {

        super(String.format("Error response from OAuth2 authorization server. Status code: %d. Error message: %s", statusCode, body));
        this.statusCode = statusCode;
        this.body = body;
    }

    public OAuth2RequestException(final HttpResponse<String> response) {

        this(response.statusCode(), response.body());
    }

}
