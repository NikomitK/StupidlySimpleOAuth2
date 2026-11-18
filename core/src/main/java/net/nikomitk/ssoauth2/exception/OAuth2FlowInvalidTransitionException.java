package net.nikomitk.ssoauth2.exception;

/**
 * An exception that is thrown when an invalid state transition occurs in the OAuth2Flow.
 */
public class OAuth2FlowInvalidTransitionException extends RuntimeException {

    /**
     * Creates a new OAuth2FlowInvalidTransitionException with the given detail message.
     *
     * @param message the detail message
     */
    public OAuth2FlowInvalidTransitionException(final String message) {

        super(message);
    }

    /**
     * Creates a new OAuth2FlowInvalidTransitionException with the given detail message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause
     */
    public OAuth2FlowInvalidTransitionException(final String message, final Throwable cause) {

        super(message, cause);
    }

    /**
     * Creates a new OAuth2FlowInvalidTransitionException with the given cause.
     *
     * @param cause the underlying cause
     */
    public OAuth2FlowInvalidTransitionException(final Throwable cause) {

        super(cause);
    }

}
