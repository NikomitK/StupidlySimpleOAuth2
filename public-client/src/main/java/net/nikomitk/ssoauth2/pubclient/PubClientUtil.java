package net.nikomitk.ssoauth2.pubclient;

import lombok.experimental.UtilityClass;
import net.nikomitk.ssoauth2.pubclient.authcodeflow.states.RefreshTokenPresent;
import net.nikomitk.ssoauth2.util.HttpFormBodyBuilder;

import java.net.URI;
import java.net.http.HttpRequest;

import static net.nikomitk.ssoauth2.util.CoreUtil.requestBuilder;

@UtilityClass
public class PubClientUtil {


    public static HttpRequest prepareRefreshRequest(final RefreshTokenPresent refreshTokenPresent, final PublicClientConfiguration configuration) {

        final String body = HttpFormBodyBuilder.newBuilder()
                                               .addFormData("grant_type", "refresh_token")
                                               .addFormData("refresh_token", refreshTokenPresent.refreshToken())
                                               .addNullableFormData("scope", configuration.scope())
                                               .addFormData("client_id", configuration.clientID())
                                               .build();

        return requestBuilder(URI.create(configuration.endpoint()
                                                      .getTokenEndpoint()))
                                                                           .header("Content-Type", "application/x-www-form-urlencoded")
                                                                           .method("POST", HttpRequest.BodyPublishers.ofString(body))
                                                                           .build();
    }


}
