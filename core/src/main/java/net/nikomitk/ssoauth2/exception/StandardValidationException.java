package net.nikomitk.ssoauth2.exception;

/**
 * Exception for cases where the user input is not compatible with an implemented standard.
 */
public class StandardValidationException extends RuntimeException {

    /**
     * Constructs a new StandardValidationException with the specified RFC number, URL, and message.
     * @param rfcNumber the RFC number that the input does not conform to.
     * @param url the URL of the RFC specification.
     * @param message a detailed message explaining the validation error.
     */
    public StandardValidationException(final int rfcNumber, final String url, final String message) {

        super(String.format("Input is not conforming to RFC%d (%s): %s", rfcNumber, url, message));
    }

}
