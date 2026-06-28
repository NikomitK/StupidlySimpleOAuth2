package net.nikomitk.ssoauth2.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.List;

@Getter
@Setter
public class OAuth2Endpoint {

    private String issuer;
    @JsonProperty("authorization_endpoint")
    private String authorizationEndpoint;
    @JsonProperty("token_endpoint")
    private String tokenEndpoint;
    @JsonProperty("userinfo_endpoint")
    private String userinfoEndpoint;
    @JsonProperty("end_session_endpoint")
    private String endSessionEndpoint;
    @JsonProperty("introspection_endpoint")
    private String introspectionEndpoint;
    @JsonProperty("revocation_endpoint")
    private String revocationEndpoint;
    @JsonProperty("device_authorization_endpoint")
    private String deviceAuthorizationEndpoint;
    @JsonProperty("backchannel_logout_supported")
    private boolean backchannelLogoutSupported;
    @JsonProperty("backchannel_logout_session_supported")
    private boolean backchannelLogoutSessionSupported;
    @JsonProperty("response_types_supported")
    private List<String> responseTypesSupported;
    @JsonProperty("response_modes_supported")
    private List<String> responseModesSupported;
    @JsonProperty("jwks_uri")
    private String jwksUri;
    @JsonProperty("grant_types_supported")
    private List<String> grantTypesSupported;
    @JsonProperty("id_token_signing_alg_values_supported")
    private List<String> idTokenSigningAlgValuesSupported;
    @JsonProperty("subject_types_supported")
    private List<String> subjectTypesSupported;
    @JsonProperty("token_endpoint_auth_methods_supported")
    private List<String> tokenEndpointAuthMethodsSupported;
    @JsonProperty("acr_values_supported")
    private List<String> acrValuesSupported;
    @JsonProperty("scopes_supported")
    private List<String> scopesSupported;
    @JsonProperty("request_parameter_supported")
    private boolean requestParameterSupported;
    @JsonProperty("claims_supported")
    private List<String> claimsSupported;
    @JsonProperty("claims_parameter_supported")
    private boolean claimsParameterSupported;
    @JsonProperty("code_challenge_methods_supported")
    private List<String> codeChallengeMethodsSupported;

    public OAuth2Endpoint() {


    }

    public static OAuth2Endpoint queryWellKnownEndpoint(String wellKnownEndpoint) {

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder().uri(java.net.URI.create(wellKnownEndpoint)).GET().build();
            var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to fetch well-known endpoint: " + response.statusCode());
            }
            return new ObjectMapper().readValue(response.body(), OAuth2Endpoint.class);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}

