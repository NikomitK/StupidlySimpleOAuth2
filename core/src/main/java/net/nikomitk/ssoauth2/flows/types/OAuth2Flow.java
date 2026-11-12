package net.nikomitk.ssoauth2.flows.types;

import net.nikomitk.ssoauth2.dto.OAuth2AccessToken;

import java.time.Instant;

public interface OAuth2Flow {

    /**
    * Initialize this Flow, in order to enable {@link OAuth2Flow#requestAccessToken}, {@link OAuth2Flow#refreshAccessToken()} and {@link OAuth2Flow#getAccessToken()}
    * @return this flow instance in order to enable chained calls
    */
    OAuth2Flow startFlow();

    /**
     * Make a call to the authorization server to get an access token
     * @return this flow instance in order to enable chained calls
     */
    OAuth2Flow requestAccessToken();

    /**
     * Refresh the access token if possible or just request a new one
     * @return this flow instance in order to enable chained calls
     */
    OAuth2Flow refreshAccessToken();


    /**
     * Get a valid {@link OAuth2AccessToken} with an {@link Instant} describing the end of validity
     * @throws {@link net.nikomitk.ssoauth2.exception.OAuth2FlowWrongStateException} if the flow was not initialized with {@link OAuth2Flow#startFlow()}
     * @return the last acquired OAuth2AccessToken
     */
    OAuth2AccessToken getAccessToken();

}
