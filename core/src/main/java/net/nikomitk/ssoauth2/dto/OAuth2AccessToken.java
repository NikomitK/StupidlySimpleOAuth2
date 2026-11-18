package net.nikomitk.ssoauth2.dto;

import lombok.NonNull;
import org.jetbrains.annotations.Contract;

import java.time.Instant;

/**
 * Data class for storing an OAuth2 access token.
 * @param token the token as a string
 * @param validUntil the instant describing how long the token is valid
 */
public record OAuth2AccessToken(@NonNull String token, @NonNull Instant validUntil) {

    /**
     * Checks whether the access token has expired (including a 3-second safety buffer).
     *
     * @return true if the token is expired, false otherwise
     */
    @Contract(pure = true)
    public boolean isExpired() {

        return Instant.now()
                      .plusSeconds(3)
                      .isAfter(validUntil);
    }

}
