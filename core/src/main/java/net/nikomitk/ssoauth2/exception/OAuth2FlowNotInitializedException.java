package net.nikomitk.ssoauth2.exception;

/**
 * Indicates that the OAuth2Flow is not initialized yet. It should be started with OAuth2Flow.startFlow().
 */
@SuppressWarnings("unused")
public class OAuth2FlowNotInitializedException extends OAuth2FlowNotReadyException {

    public OAuth2FlowNotInitializedException(final String message) {

        super(message);
    }

    public OAuth2FlowNotInitializedException(final String message, final Throwable cause) {

        super(message, cause);
    }

    public OAuth2FlowNotInitializedException() {

        super("This flow has not been initialized yet! Try using OAuth2Flow.startFlow()!");
    }

}
