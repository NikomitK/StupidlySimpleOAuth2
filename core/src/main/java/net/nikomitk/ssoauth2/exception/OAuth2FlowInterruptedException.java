package net.nikomitk.ssoauth2.exception;

/**
 * Indicates that the OAuth2Flow was interrupted.
 */
public class OAuth2FlowInterruptedException extends RuntimeException {

    public OAuth2FlowInterruptedException(final String message) {

        super(message);
    }

    public OAuth2FlowInterruptedException(final String message, final Throwable cause) {

        super(message, cause);
    }

}
