package net.nikomitk.ssoauth2.flows;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.nikomitk.ssoauth2.exception.OAuth2FlowInvalidTransitionException;
import net.nikomitk.ssoauth2.flows.types.OAuth2Flow;
import net.nikomitk.ssoauth2.states.ClosableState;
import net.nikomitk.ssoauth2.states.ExpiringState;
import net.nikomitk.ssoauth2.states.OAuth2FlowState;
import net.nikomitk.ssoauth2.states.StartableState;
import net.nikomitk.ssoauth2.util.CoreUtil;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Abstract base class for all OAuth2 flows.
 */
@SuppressWarnings("unused")
public abstract class OAuth2FlowBase implements OAuth2Flow {

    protected final ObjectMapper objectMapper;
    private volatile OAuth2FlowState state;
    private final Object lock;
    private static final ScheduledExecutorService scheduler = CoreUtil.createScheduler();

    /**
     * Initializes common attributes like the ObjectMapper and OAuth2TokenResponse
     */
    protected OAuth2FlowBase(final Object lock, final OAuth2FlowState initialState) {

        this.lock = lock;
        objectMapper = CoreUtil.createObjectMapper();
        this.state = initialState;
    }

    protected OAuth2FlowBase(final Object lock) {

        this(lock, null);
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

        final OAuth2FlowState current = state;
        return scheduler.schedule(() -> { // maybe implement a handoff to a virtual thread, so this is not blocking the single thread of the scheduler
            synchronized (lock) {
                if (state == current)
                    transitionTo(next);
            }
        }, duration.toMillis(), TimeUnit.MILLISECONDS);
    }

}
