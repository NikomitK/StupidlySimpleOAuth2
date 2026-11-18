package net.nikomitk.ssoauth2.pubclient;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.nikomitk.ssoauth2.dto.OAuth2FlowConfiguration;
import net.nikomitk.ssoauth2.util.HttpFormBodyBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpRequest;

import static net.nikomitk.ssoauth2.util.CoreUtil.requestBuilder;

/**
 * Utility class providing shared helpers for public OAuth2 client flows.
 */
@UtilityClass
public class PubClientUtil {

    /**
     * Builds an HTTP POST request to refresh an access token using a refresh token.
     *
     * @param refreshToken  the refresh token to use
     * @param configuration the flow configuration containing endpoint, client ID, scope, and HTTP settings
     * @return the prepared {@link HttpRequest}
     */
    @Contract("_, _ -> new")
    public static @NotNull HttpRequest prepareRefreshRequest(@NonNull final String refreshToken, @NonNull final OAuth2FlowConfiguration configuration) {

        final String body = HttpFormBodyBuilder.newBuilder()
                                               .addFormData("grant_type", "refresh_token")
                                               .addFormData("refresh_token", refreshToken)
                                               .addNullableFormData("scope", configuration.scope())
                                               .addFormData("client_id", configuration.clientID())
                                               .build();

        return requestBuilder(URI.create(configuration.endpoint()
                                                      .getTokenEndpoint()), configuration)
                                                                                          .header("Content-Type", "application/x-www-form-urlencoded")
                                                                                          .method("POST", HttpRequest.BodyPublishers.ofString(body))
                                                                                          .build();
    }


}
