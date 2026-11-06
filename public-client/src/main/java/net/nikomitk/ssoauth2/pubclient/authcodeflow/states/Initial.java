package net.nikomitk.ssoauth2.pubclient.authcodeflow.states;

import net.nikomitk.ssoauth2.states.OAuth2FlowState;

import java.util.Set;

public record Initial() implements AuthCodeFlowState {

    @Override
    public Set<Class<? extends OAuth2FlowState>> getPossibleNextStates() {

        return Set.of(Initial.class, AwaitingAuthCode.class);
    }

}