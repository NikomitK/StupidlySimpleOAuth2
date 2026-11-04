package net.nikomitk.ssoauth2.states;

import net.nikomitk.ssoauth2.OAuth2FlowState;

import java.util.concurrent.ScheduledFuture;

public interface ExpiringState extends OAuth2FlowState {

    void setTimeout(ScheduledFuture<?> future);

}
