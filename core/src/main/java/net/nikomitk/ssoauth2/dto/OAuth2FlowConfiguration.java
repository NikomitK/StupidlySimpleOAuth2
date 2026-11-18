package net.nikomitk.ssoauth2.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import net.nikomitk.ssoauth2.util.CoreUtil;
import org.jetbrains.annotations.Nullable;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Base configuration class for OAuth2 flows, holding common settings such as endpoint metadata, client ID, scope, and HTTP client options.
 */
@Accessors(fluent = true)
@Getter
@SuperBuilder
public abstract class OAuth2FlowConfiguration {

    @NonNull
    private final OAuth2Endpoint endpoint;
    @NonNull
    private final String clientID;
    @Nullable
    private final String scope;

    @Builder.Default
    @NonNull
    private final HttpClient httpClient = CoreUtil.defaultHttpClient();

    @Builder.Default
    @NonNull
    private final String userAgent = CoreUtil.DEFAULT_USER_AGENT;

    @Builder.Default
    @NonNull
    private final Duration httpTimeout = CoreUtil.DEFAULT_HTTP_TIMEOUT;

}