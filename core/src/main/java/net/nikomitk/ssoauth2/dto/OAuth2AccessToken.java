package net.nikomitk.ssoauth2.dto;

import lombok.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

/**
 * Data class for storing an OAuth2 access token.
 * @param token the token as a string
 * @param validUntil the instant describing how long the token is valid
 */
public record OAuth2AccessToken(@NotNull @NonNull String token, @NotNull @NonNull Instant validUntil) {

    @Contract(pure = true)
    public boolean isExpired() {

        return Instant.now()
                      .plusSeconds(3)
                      .isAfter(validUntil);
    }

}
