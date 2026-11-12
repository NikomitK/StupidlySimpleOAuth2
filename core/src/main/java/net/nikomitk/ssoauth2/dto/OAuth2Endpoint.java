package net.nikomitk.ssoauth2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;

/**
 * Data class for an OAuth2 Authorization Server
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class OAuth2Endpoint {

    private String issuer;
    @JsonProperty("authorization_endpoint")
    @NonNull
    private String authorizationEndpoint;
    @NonNull
    @JsonProperty("token_endpoint")
    private String tokenEndpoint;
    @JsonProperty("introspection_endpoint")
    private String introspectionEndpoint;
    @JsonProperty("revocation_endpoint")
    private String revocationEndpoint;
    @JsonProperty("device_authorization_endpoint")
    private String deviceAuthorizationEndpoint;
    @JsonProperty("response_types_supported")
    private List<String> responseTypesSupported;
    @JsonProperty("response_modes_supported")
    private List<String> responseModesSupported;
    @JsonProperty("jwks_uri")
    private String jwksUri;
    @JsonProperty("grant_types_supported")
    private List<String> grantTypesSupported;
    @JsonProperty("token_endpoint_auth_methods_supported")
    private List<String> tokenEndpointAuthMethodsSupported;
    @JsonProperty("scopes_supported")
    private List<String> scopesSupported;
    @JsonProperty("code_challenge_methods_supported")
    private List<String> codeChallengeMethodsSupported;

}
