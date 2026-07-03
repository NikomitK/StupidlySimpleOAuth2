package net.nikomitk.ssoauth2.dto;

import java.time.Instant;

public record OAuth2AccessToken(String token, Instant validUntil) {

}
