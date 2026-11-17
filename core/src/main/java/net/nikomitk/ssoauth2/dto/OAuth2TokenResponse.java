package net.nikomitk.ssoauth2.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

/**
 * Class used to represent the JSON response from an OAuth Authorization Server for the "AccessTokenResponse" described in Section 4.1.4 of RFC6749.
 */
public record OAuth2TokenResponse(

        @JsonProperty("access_token") @NonNull String accessToken,
        @JsonProperty("token_type") @NonNull String tokenType,
        @JsonProperty("expires_in") int expiresIn,
        @JsonProperty("refresh_token") @Nullable String refreshToken,
        // set when Jackson creates the instance, i.e. when the response is parsed
        @JsonIgnore @NotNull Instant receivedAt) {

    public OAuth2TokenResponse {

        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }

    public OAuth2TokenResponse(@NonNull String accessToken, @NonNull String tokenType, int expiresIn, @Nullable String refreshToken) {

        this(accessToken, tokenType, expiresIn, refreshToken, Instant.now());
    }

}
