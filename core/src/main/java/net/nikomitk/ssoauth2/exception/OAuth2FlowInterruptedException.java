package net.nikomitk.ssoauth2.exception;

/**
 * Indicates that the OAuth2Flow was interrupted.
 */
public class OAuth2FlowInterruptedException extends RuntimeException {

    /**
     * Creates a new OAuth2FlowInterruptedException with the given detail message.
     *
     * @param message the detail message
     */
    public OAuth2FlowInterruptedException(final String message) {

        super(message);
    }

    /**
     * Creates a new OAuth2FlowInterruptedException with the given detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the interruption
     */
    public OAuth2FlowInterruptedException(final String message, final Throwable cause) {

        super(message, cause);
    }

}
