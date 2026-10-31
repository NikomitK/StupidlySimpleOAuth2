package net.nikomitk.ssoauth2.exception;

/**
 * Indicates that the OAuth2Flow is not ready yet.
 */
@SuppressWarnings("unused")
public class OAuth2FlowNotReadyException extends RuntimeException {

    public OAuth2FlowNotReadyException(final String message) {

        super(message);
    }

    public OAuth2FlowNotReadyException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public OAuth2FlowNotReadyException() {
        super("This flow is not ready for this step yet!");
    }

}
