package net.nikomitk.ssoauth2.exception;

import java.net.http.HttpResponse;

/**
 * Indicates that a request to the OAuth2 authorization server has returned an error.
 */
public class OAuth2RequestException extends RuntimeException {

    public OAuth2RequestException(final int statusCode, final String message) {

        super(String.format("Error response from OAuth2 authorization server. Status code: %d. Error message: %s", statusCode, message));
    }

    public OAuth2RequestException(final HttpResponse<String> response) {

        this(response.statusCode(), response.body());
    }

    public static void check(final HttpResponse<String> response) {

        if (response.statusCode() != 200) {
            throw new OAuth2RequestException(response);
        }
    }

}
