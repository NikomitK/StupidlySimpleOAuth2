package net.nikomitk.ssoauth2.states;

import java.util.concurrent.ScheduledFuture;

public interface ExpiringState extends OAuth2FlowState {

    void setTimeout(ScheduledFuture<?> future);

}
