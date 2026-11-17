package net.nikomitk.ssoauth2.pubclient.authcodeflow;

import lombok.NonNull;
import net.nikomitk.ssoauth2.dto.OAuth2Endpoint;
import net.nikomitk.ssoauth2.pubclient.PublicClientConfiguration;
import net.nikomitk.ssoauth2.security.PKCE;
import net.nikomitk.ssoauth2.util.CoreUtil;
import org.jetbrains.annotations.Nullable;

public record AuthCodeFlowConfiguration(@NonNull OAuth2Endpoint endpoint, @NonNull String clientID,
        @Nullable String scope,
        @NonNull PKCE pkce) implements PublicClientConfiguration {

    public AuthCodeFlowConfiguration(@NonNull final String wellKnownEndpoint, @NonNull final String clientID, @Nullable final String scope, @NonNull final PKCE pkce) {

        this(CoreUtil.queryWellKnownEndpoint(wellKnownEndpoint), clientID, scope, pkce);
    }

}