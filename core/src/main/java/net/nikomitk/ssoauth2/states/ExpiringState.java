package net.nikomitk.ssoauth2.states;

import java.util.concurrent.ScheduledFuture;

/**
 * Interface representing a state in the OAuth2 flow that has an expiration mechanism.
 */
public interface ExpiringState extends OAuth2FlowState {

    /**
     * Sets the ScheduledFuture that will trigger the expiration.
     *
     * @param future the ScheduledFuture representing the scheduled timeout task.
     */
    void setTimeout(ScheduledFuture<?> future);

}
