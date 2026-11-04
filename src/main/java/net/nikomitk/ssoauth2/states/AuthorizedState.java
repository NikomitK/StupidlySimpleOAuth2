package net.nikomitk.ssoauth2.states;

import net.nikomitk.ssoauth2.dto.OAuth2AccessToken;

public interface AuthorizedState {
    OAuth2AccessToken getAccessToken();
}
