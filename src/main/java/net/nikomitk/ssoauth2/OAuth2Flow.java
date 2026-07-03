package net.nikomitk.ssoauth2;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.nikomitk.ssoauth2.dto.OAuth2AccessToken;
import net.nikomitk.ssoauth2.dto.OAuth2TokenResponse;
import net.nikomitk.ssoauth2.exception.OAuth2FlowNotReadyException;

import java.time.Instant;

public abstract class OAuth2Flow {

    protected ObjectMapper objectMapper;

    protected OAuth2TokenResponse lastOAuth2TokenResponse;

    protected OAuth2Flow() {

        objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    /**
     * Initialize this Flow, in order to enable {@link OAuth2Flow#requestAccessToken}, {@link OAuth2Flow#refreshAccessToken()} and {@link OAuth2Flow#getAccessToken()}
     * @return this flow instance in order to enable chained calls
     */
    public abstract OAuth2Flow startFlow();

    /**
     * Make a call to the authorization server to get an access token
     * @return this flow instance in order to enable chained calls
     */
    public abstract OAuth2Flow requestAccessToken();

    /**
     * Refresh the access token if possible or just request a new one
     * @return this flow instance in order to enable chained calls
     */
    public abstract OAuth2Flow refreshAccessToken();

    /**
     * Get a valid {@link OAuth2AccessToken} with an {@link Instant} describing the end of validity
     * @throws OAuth2FlowNotReadyException if the flow was not initialized with {@link OAuth2Flow#startFlow()}
     */
    public OAuth2AccessToken getAccessToken() {

        if(this.lastOAuth2TokenResponse == null) {
            throw new OAuth2FlowNotReadyException("No access token has been requested!");
        }
        return new OAuth2AccessToken(this.lastOAuth2TokenResponse.getAccessToken(), Instant
                .now().plusSeconds(this.lastOAuth2TokenResponse.getExpiresIn()));
    }
}
