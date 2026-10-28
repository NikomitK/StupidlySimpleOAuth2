package net.nikomitk.ssoauth2.exception;

/**
 * Exception for cases where the user input is not compatible with an implemented standard.
 */
public class StandardValidationException extends RuntimeException {

    public StandardValidationException(final int rfcNumber, final String url, final String message) {
        final String msg = String.format("Input is not conforming to RFC%d (%s): %s",rfcNumber, url, message);
        super(msg);
    }

}
