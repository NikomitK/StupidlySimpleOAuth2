package net.nikomitk.ssoauth2.pubclient.authcodeflow.states;

import net.nikomitk.ssoauth2.states.OAuth2FlowState;

public sealed interface AuthCodeFlowState extends OAuth2FlowState
        permits Initial, AwaitingAuthCode, AuthCodePresent, AccessTokenPresent, RefreshTokenPresent {

}
