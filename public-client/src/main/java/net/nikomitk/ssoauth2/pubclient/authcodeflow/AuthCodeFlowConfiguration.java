package net.nikomitk.ssoauth2.pubclient.authcodeflow;

import lombok.NonNull;
import net.nikomitk.ssoauth2.dto.OAuth2Endpoint;
import net.nikomitk.ssoauth2.pubclient.PublicClientConfiguration;
import net.nikomitk.ssoauth2.security.PKCE;
import net.nikomitk.ssoauth2.util.CoreUtil;

public record AuthCodeFlowConfiguration(OAuth2Endpoint endpoint, @NonNull String clientID, String scope,
                                        @NonNull PKCE pkce) implements PublicClientConfiguration {

    public AuthCodeFlowConfiguration(final String wellKnownEndpoint, final String clientID, final String scope, final PKCE pkce) {

        this(CoreUtil.queryWellKnownEndpoint(wellKnownEndpoint), clientID, scope, pkce);
    }

}