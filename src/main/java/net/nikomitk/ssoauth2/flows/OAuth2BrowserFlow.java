package net.nikomitk.ssoauth2.flows;

import net.nikomitk.ssoauth2.security.PKCE;
import net.nikomitk.ssoauth2.util.Util;

import javax.naming.OperationNotSupportedException;
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
     *                         "string1 string2 string3"
     * @param pkce              the {@link PKCE} instance to use for the OAuth2 flow. If it is null, the flow does
     *                          not use PKCE. This is not recommended.
     */
    public OAuth2BrowserFlow(final String wellKnownEndpoint, final String clientID, final String scope,
                             final PKCE pkce) {

        super(wellKnownEndpoint, clientID, scope, pkce);
    }

    /**
     * Starts an http server on the loopback interface and a random port. Tries to open the default browser with the
     * authorization URL.
     *
     * @throws RuntimeException when the browser can not be opened
     */
    @Override
    public OAuth2AuthCodeFlow startFlow() {

        super.startFlow();
        try {
            Util.openBrowser(getAuthUri());
        } catch (final OperationNotSupportedException | IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * Starts an http server on the loopback interface and a specified port. Tries to open the default browser with
     * the authorization URL.
     *
     * @throws RuntimeException when the browser can not be opened
     */
    @Override
    public OAuth2AuthCodeFlow startFlow(final int port) {

        super.startFlow(port);
        try {
            Util.openBrowser(getAuthUri());
        } catch (final OperationNotSupportedException | IOException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

}
