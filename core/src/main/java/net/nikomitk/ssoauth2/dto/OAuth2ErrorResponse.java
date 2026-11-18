package net.nikomitk.ssoauth2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.NonNull;

import org.jetbrains.annotations.Nullable;

/**
 * Data class representing a standardized OAuth2 error response as described in Section 5.2 of RFC6749.
 *
 * @param error            the single ASCII error code
 * @param errorDescription optional human-readable text providing additional information about the error
 * @param errorUri         optional URI identifying a human-readable web page with information about the error
 */
public record OAuth2ErrorResponse(
        @JsonProperty("error") @NonNull OAuth2ErrorCode error,
        @JsonProperty("error_description") @Nullable String errorDescription,
        @JsonProperty("error_uri") @Nullable String errorUri) {}