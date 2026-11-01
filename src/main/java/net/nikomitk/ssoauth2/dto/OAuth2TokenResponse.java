package net.nikomitk.ssoauth2.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Class used to represent the JSON response from an OAuth Authorization Server for the "AccessTokenResponse" described in Section 4.1.4 of RFC6749.
 */
@Getter
@Setter
public class OAuth2TokenResponse {

    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("token_type")
    private String tokenType;
    @JsonProperty("expires_in")
    private int expiresIn;
    @JsonProperty("refresh_token")
    private String refreshToken;
    // set when Jackson creates the instance, i.e. when the response is parsed
    @JsonIgnore
    private final Instant receivedAt = Instant.now();

}
