package net.nikomitk.ssoauth2.dto;

import java.time.Instant;

/**
 * Data class for storing an OAuth2 access token.
 * @param token the token as a string
 * @param validUntil the instant describing how long the token is valid
 */
public record OAuth2AccessToken(String token, Instant validUntil) {

    public OAuth2AccessToken(final OAuth2TokenResponse tokenResponse) {

        this(tokenResponse.getAccessToken(), tokenResponse.getReceivedAt()
                                                          .plusSeconds(tokenResponse.getExpiresIn()));
    }

}
