package net.nikomitk.ssoauth2.flows;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import net.nikomitk.ssoauth2.exception.OAuth2FlowInvalidTransitionException;
import net.nikomitk.ssoauth2.flows.types.OAuth2Flow;
import net.nikomitk.ssoauth2.states.ClosableState;
import net.nikomitk.ssoauth2.states.ExpiringState;
import net.nikomitk.ssoauth2.states.OAuth2FlowState;
import net.nikomitk.ssoauth2.util.CoreUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Abstract base class for all OAuth2 flows.
 */
@SuppressWarnings("unused")
public abstract class OAuth2FlowBase implements OAuth2Flow {

    protected static final ObjectMapper objectMapper = CoreUtil.createObjectMapper();
    private volatile OAuth2FlowState state;
    private final Object lock;
    private static final ScheduledExecutorService scheduler = CoreUtil.createScheduler();

    /**
     * Initializes common attributes like the ObjectMapper and OAuth2TokenResponse
     */
    protected OAuth2FlowBase(@NonNull final Object lock, @Nullable final OAuth2FlowState initialState) {

        this.lock = lock;
        this.state = initialState;
    }

    /**
     * Initializes the common lock object for synchronizing state transitions. The initial state is set to null.
     * @param lock the object to lock on.
     */
    protected OAuth2FlowBase(@NonNull final Object lock) {

        this(lock, null);
    }

    /**
     * Transitions the flow to a new state. If the transition is invalid, an exception is thrown.
     *
     * @param newState the initialized new state to transition to.
     * @throws OAuth2FlowInvalidTransitionException if the class of the new state is not in the list of possible next states of the current state.
     */
    protected void transitionTo(@NonNull final OAuth2FlowState newState) throws OAuth2FlowInvalidTransitionException {

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
        }
    }

    /**
     * Transitions the flow to a new state. The new state receives a ScheduledFuture that will transition the flow to a timeout state after the specified delay.
     * The ScheduledFuture is meant to be cancelled if the transition
     * If the transition is invalid, an exception is thrown.
     *
     * @param newState the initialized new state to transition to.
     * @param delay the duration after which the flow should transition to the timeout state.
     * @param timeoutState the state to transition to after the delay.
     * @throws OAuth2FlowInvalidTransitionException if the class of the new state is not in the list of possible next states of the current state.
     */
    protected void transitionWithTimeout(@NonNull final ExpiringState newState, @NonNull final Duration delay, @NonNull final OAuth2FlowState timeoutState) throws OAuth2FlowInvalidTransitionException {

        synchronized (lock) {
            transitionTo(newState);
            newState.setTimeout(expireAfter(delay, timeoutState));
        }
    }

    /**
     * @return the current state of the flow.
     */
    @Contract(pure = true)
    protected @Nullable OAuth2FlowState getState() {

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
