package net.nikomitk.ssoauth2.exception;

/**
 * An exception that is thrown when an invalid state transition occurs in the OAuth2Flow.
 */
public class OAuth2FlowInvalidTransitionException extends RuntimeException {

    public OAuth2FlowInvalidTransitionException(final String message) {

        super(message);
    }

    public OAuth2FlowInvalidTransitionException(final String message, final Throwable cause) {

        super(message, cause);
    }

    public OAuth2FlowInvalidTransitionException(final Throwable cause) {

        super(cause);
    }

}
