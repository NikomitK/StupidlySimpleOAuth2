package net.nikomitk.ssoauth2.dto;

import java.time.Instant;

/**
 * Data class for storing an OAuth2 access token.
 * @param token the token as a string
 * @param validUntil the instant describing how long the token is valid
 */
public record OAuth2AccessToken(String token, Instant validUntil) {

}
