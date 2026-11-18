package net.nikomitk.ssoauth2.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Enum representing standardized OAuth2 error codes from RFC6749, RFC7009, and RFC8628.
 */
public enum OAuth2ErrorCode {

    /**
     * The request is missing a required parameter, includes an unsupported parameter value, repeats a parameter, or is otherwise malformed.
     */
    INVALID_REQUEST,
    /**
     * Client authentication failed.
     */
    INVALID_CLIENT,
    /**
     * The provided authorization grant or refresh token is invalid, expired, revoked, or does not match the redirection URI.
     */
    INVALID_GRANT,
    /**
     * The authenticated client is not authorized to use this authorization grant type.
     */
    UNAUTHORIZED_CLIENT,
    /**
     * The authorization grant type is not supported by the authorization server.
     */
    UNSUPPORTED_GRANT_TYPE,
    /**
     * The authorization server does not support obtaining an authorization code using this method.
     */
    UNSUPPORTED_RESPONSE_TYPE,
    /**
     * The requested scope is invalid, unknown, malformed, or exceeds the scope granted by the resource owner.
     */
    INVALID_SCOPE,
    /**
     * The resource owner or authorization server denied the request.
     */
    ACCESS_DENIED,
    /**
     * The authorization server encountered an unexpected condition that prevented it from fulfilling the request.
     */
    SERVER_ERROR,
    /**
     * The authorization server is currently unable to handle the request due to a temporary overloading or maintenance.
     */
    TEMPORARILY_UNAVAILABLE,

    /**
     * The authorization server does not support the revocation of the presented token type (RFC7009 Section 2.2.1).
     */
    UNSUPPORTED_TOKEN_TYPE,

    /**
     * The authorization request is still pending as the end user hasn't yet completed the user-interaction steps (RFC8628 Section 3.5).
     */
    AUTHORIZATION_PENDING,
    /**
     * Polling is occurring too frequently and the interval must be increased (RFC8628 Section 3.5).
     */
    SLOW_DOWN,
    /**
     * The {@code device_code} has expired and the device authorization session has concluded (RFC8628 Section 3.5).
     */
    EXPIRED_TOKEN,

    /**
     * Fallback value for error codes not defined in the standard OAuth2 RFCs.
     */
    NOT_STANDARDIZED_ERROR;

    /**
     * Resolves an {@link OAuth2ErrorCode} from its wire-format string representation, falling back to {@link #NOT_STANDARDIZED_ERROR} for unknown or blank values.
     *
     * @param value the error code string from the authorization server
     * @return the matching {@link OAuth2ErrorCode}, or {@link #NOT_STANDARDIZED_ERROR}
     */
    @JsonCreator
    @Contract(pure = true)
    public static @NotNull OAuth2ErrorCode fromString(@Nullable final String value) {

        if (value == null || value.isBlank()) {
            return NOT_STANDARDIZED_ERROR;
        }
        try {
            return OAuth2ErrorCode.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException e) {
            return NOT_STANDARDIZED_ERROR;
        }
    }

}
