package net.nikomitk.ssoauth2.pubclient;

import net.nikomitk.ssoauth2.dto.OAuth2Endpoint;
import net.nikomitk.ssoauth2.security.PKCE;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PublicClientConfiguration {

    @NotNull
    OAuth2Endpoint endpoint();

    @NotNull
    String clientID();

    @Nullable
    String scope();

    @NotNull
    PKCE pkce();

}
