package net.nikomitk.ssoauth2;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.nikomitk.ssoauth2.dto.OAuth2AccessToken;
import net.nikomitk.ssoauth2.dto.OAuth2TokenResponse;
import net.nikomitk.ssoauth2.exception.OAuth2FlowInvalidTransitionException;
import net.nikomitk.ssoauth2.exception.OAuth2FlowNotReadyException;
import net.nikomitk.ssoauth2.states.ClosableState;
import net.nikomitk.ssoauth2.states.ExpiringState;
import net.nikomitk.ssoauth2.states.OAuth2FlowState;
import net.nikomitk.ssoauth2.states.StartableState;
import net.nikomitk.ssoauth2.util.Util;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Abstract base class for all OAuth2 flows.
 */
@SuppressWarnings("unused")
public abstract class OAuth2Flow {

    protected final ObjectMapper objectMapper;
    private volatile OAuth2FlowState state;
    private final Object lock;
    private static final ScheduledExecutorService scheduler = Util.createScheduler();

    protected volatile OAuth2TokenResponse lastOAuth2TokenResponse;

    /**
     * Initializes common attributes like the ObjectMapper and OAuth2TokenResponse
     */
    protected OAuth2Flow(final Object lock, final OAuth2FlowState initialState) {

        this.lock = lock;
        objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.state = initialState;
    }

    protected OAuth2Flow(final Object lock) {

        this(lock, null);
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


    protected void transitionTo(final OAuth2FlowState newState) throws OAuth2FlowInvalidTransitionException {

        synchronized (lock) {
            if (state != null && !state.getPossibleNextStates()
                                       .contains(newState.getClass())) {
                throw new OAuth2FlowInvalidTransitionException("Invalid transition from " + state.getClass()
                                                                                                 .getSimpleName() + " to " + newState.getClass()
                                                                                                                                     .getSimpleName());
            }

            final OAuth2FlowState previousState = state;
            state = newState;

            if (previousState instanceof final ClosableState closableState) {
                closableState.close();
            }
            if (newState instanceof final StartableState startableState) {
                startableState.start();
            }
        }
    }

    protected void transitionWithTimeout(final ExpiringState newState, final Duration delay, final OAuth2FlowState timeoutState) throws OAuth2FlowInvalidTransitionException {

        transitionTo(newState);
        newState.setTimeout(expireAfter(delay, timeoutState));
    }

    protected OAuth2FlowState getState() {

        return state;
    }

    private ScheduledFuture<?> expireAfter(final Duration duration, final OAuth2FlowState next) {

        final OAuth2FlowState current = state; // caller holds lock
        return scheduler.schedule(() -> {
            synchronized (lock) {
                if (state == current)
                    transitionTo(next);
            }
        }, duration.toMillis(), TimeUnit.MILLISECONDS);
    }

}
