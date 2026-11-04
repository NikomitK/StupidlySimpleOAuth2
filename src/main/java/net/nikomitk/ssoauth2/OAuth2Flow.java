package net.nikomitk.ssoauth2;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.nikomitk.ssoauth2.dto.OAuth2AccessToken;
import net.nikomitk.ssoauth2.dto.OAuth2TokenResponse;
import net.nikomitk.ssoauth2.exception.OAuth2FlowNotReadyException;
import net.nikomitk.ssoauth2.exception.OAuth2FlowWrongStateException;
import net.nikomitk.ssoauth2.states.ClosableState;
import net.nikomitk.ssoauth2.states.StartableState;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Abstract base class for all OAuth2 flows.
 */
@SuppressWarnings("unused")
public abstract class OAuth2Flow {

    protected final ObjectMapper objectMapper;
    private volatile OAuth2FlowState state;
    protected final Object lock = new Object();
    protected static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    protected volatile OAuth2TokenResponse lastOAuth2TokenResponse;

    /**
     * Initializes common attributes like the ObjectMapper and OAuth2TokenResponse
     */
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
     * @return the last acquired OAuth2AccessToken
     */
    public OAuth2AccessToken getAccessToken() {

        if (this.lastOAuth2TokenResponse == null) {
            throw new OAuth2FlowNotReadyException("No access token has been requested!");
        }
        return new OAuth2AccessToken(this.lastOAuth2TokenResponse.getAccessToken(), this.lastOAuth2TokenResponse
                                                                                                                .getReceivedAt()
                                                                                                                .plusSeconds(this.lastOAuth2TokenResponse.getExpiresIn()));
    }


    protected void transitionTo(OAuth2FlowState newState) {

        OAuth2FlowState previousState = state;
        state = newState;
        if (previousState instanceof ClosableState closableState) {
            closableState.close();
        }
        if (newState instanceof StartableState startableState) {
            startableState.start();
        }
    }

    protected OAuth2FlowState getState() {

        return state;
    }

    protected void checkState(Class<?> expectedState) {

        if (!expectedState.isInstance(state)) {
            throw new OAuth2FlowWrongStateException(state.getClass()
                                                         .getSimpleName(), new String[]{expectedState.getSimpleName()});
        }
    }

}
