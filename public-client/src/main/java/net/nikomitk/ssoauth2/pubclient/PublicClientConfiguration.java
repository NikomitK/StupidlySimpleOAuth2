package net.nikomitk.ssoauth2.pubclient;

import net.nikomitk.ssoauth2.dto.OAuth2Endpoint;
import net.nikomitk.ssoauth2.security.PKCE;

public interface PublicClientConfiguration {

    OAuth2Endpoint endpoint();

    String clientID();

    String scope();

    PKCE pkce();

}
