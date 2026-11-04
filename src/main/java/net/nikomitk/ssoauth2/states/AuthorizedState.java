package net.nikomitk.ssoauth2.states;

import net.nikomitk.ssoauth2.dto.OAuth2AccessToken;

public interface AuthorizedState extends OAuth2FlowState {

    OAuth2AccessToken getAccessToken();

}
