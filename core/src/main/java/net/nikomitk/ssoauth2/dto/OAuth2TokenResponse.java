package net.nikomitk.ssoauth2.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Class used to represent the JSON response from an OAuth Authorization Server for the "AccessTokenResponse" described in Section 4.1.4 of RFC6749.
 */
public record OAuth2TokenResponse(

        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") int expiresIn,
        @JsonProperty("refresh_token") String refreshToken,
        // set when Jackson creates the instance, i.e. when the response is parsed
        @JsonIgnore Instant receivedAt) {

    public OAuth2TokenResponse {

        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }

    public OAuth2TokenResponse(String accessToken, String tokenType, int expiresIn, String refreshToken) {

        this(accessToken, tokenType, expiresIn, refreshToken, Instant.now());
    }

}
