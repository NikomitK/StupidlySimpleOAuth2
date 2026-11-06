package net.nikomitk.ssoauth2.pubclient.authcodeflow.states;

import net.nikomitk.ssoauth2.dto.OAuth2AccessToken;
import net.nikomitk.ssoauth2.states.AuthorizedState;
import net.nikomitk.ssoauth2.states.OAuth2FlowState;

import java.util.Set;

public record AccessTokenPresent(
        OAuth2AccessToken accessToken) implements AuthCodeFlowState, AuthorizedState {

    @Override
    public OAuth2AccessToken getAccessToken() {

        return accessToken;
    }

    @Override
    public Set<Class<? extends OAuth2FlowState>> getPossibleNextStates() {

        return Set.of(Initial.class);
    }

}