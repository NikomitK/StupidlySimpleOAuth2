package net.nikomitk.ssoauth2.pubclient.authcodeflow;

import lombok.NonNull;
import net.nikomitk.ssoauth2.security.PKCE;
import net.nikomitk.ssoauth2.util.CoreUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.io.IOException;

/**
 * Extension of the Authorization Code flow which automatically opens the default browser.
 */
@SuppressWarnings("unused")
public class OAuth2BrowserFlow extends OAuth2AuthCodeFlow {

    /**
     * Create a new Instance of the OAuth2BrowserFlow.
     *
     * @param wellKnownEndpoint the well-known-endpoint of your OAuth2 provider. Usually ends with /
     *                          .well-known/openid-configuration
     * @param clientID          the OAuth2 clientID of your public OAuth2 client
     * @param scope             space separated strings, which should be requested from the OAuth2 provider. Example:
     *                          "string1 string2 string3"
     * @param pkce              the {@link PKCE} instance to use for the OAuth2 flow.
     */
    public OAuth2BrowserFlow(@NonNull final String wellKnownEndpoint, @NonNull final String clientID, @Nullable final String scope, @NonNull final PKCE pkce) {

        super(wellKnownEndpoint, clientID, scope, pkce);
    }

    /**
     * Create a new Instance of the OAuth2BrowserFlow.
     *
     * @param configuration an initialized configuration object
     */
    public OAuth2BrowserFlow(@NonNull final AuthCodeFlowConfiguration configuration) {

        super(configuration);
    }

    /**
     * Starts an http server on the loopback interface and a specified port. Tries to open the default browser with
     * the authorization URL.
     *
     * @throws RuntimeException when the browser can not be opened
     */
    @Override
    @Contract("_ -> this")
    public @NotNull OAuth2AuthCodeFlow startFlow(@Range(from = 0, to = 65535) final int port) {

        super.startFlow(port);
        try {
            CoreUtil.openBrowser(getAuthUri());
        } catch (final UnsupportedOperationException | IOException e) {
            reset();
            throw new RuntimeException(e);
        }
        return this;
    }

}
