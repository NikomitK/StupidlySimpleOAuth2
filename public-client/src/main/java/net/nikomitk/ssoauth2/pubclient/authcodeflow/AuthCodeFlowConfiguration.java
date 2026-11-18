package net.nikomitk.ssoauth2.pubclient.authcodeflow;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import net.nikomitk.ssoauth2.dto.OAuth2Endpoint;
import net.nikomitk.ssoauth2.dto.OAuth2FlowConfiguration;
import net.nikomitk.ssoauth2.security.PKCE;
import net.nikomitk.ssoauth2.util.CoreUtil;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/**
 * Configuration for the OAuth2 Authorization Code Flow, extending {@link OAuth2FlowConfiguration} with PKCE and authorization timeout settings.
 */
@Accessors(fluent = true)
@Getter
@SuperBuilder
public class AuthCodeFlowConfiguration extends OAuth2FlowConfiguration {

    @NonNull
    private final PKCE pkce;
    @Builder.Default
    @NonNull
    private final Duration authTimeout = Duration.ofSeconds(120);

    /**
     * Creates a new configuration using an initialized {@link OAuth2Endpoint} and default HTTP/timeout settings.
     *
     * @param endpoint the OAuth2 authorization server endpoint metadata
     * @param clientID the OAuth2 client ID
     * @param scope    optional space-separated OAuth2 scopes
     * @param pkce     the {@link PKCE} code challenge method to use
     */
    public AuthCodeFlowConfiguration(@NonNull final OAuth2Endpoint endpoint, @NonNull final String clientID, @Nullable final String scope, @NonNull final PKCE pkce) {

        this(builder()
                      .endpoint(endpoint)
                      .clientID(clientID)
                      .scope(scope)
                      .pkce(pkce));
    }

    /**
     * Creates a new configuration by querying the given OAuth2/OIDC well-known discovery endpoint.
     *
     * @param wellKnownEndpoint the URL of the well-known discovery endpoint
     * @param clientID          the OAuth2 client ID
     * @param scope             optional space-separated OAuth2 scopes
     * @param pkce              the {@link PKCE} code challenge method to use
     */
    public AuthCodeFlowConfiguration(@NonNull final String wellKnownEndpoint, @NonNull final String clientID, @Nullable final String scope, @NonNull final PKCE pkce) {

        this(CoreUtil.queryWellKnownEndpoint(wellKnownEndpoint), clientID, scope, pkce);
    }

}