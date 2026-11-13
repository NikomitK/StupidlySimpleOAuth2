package net.nikomitk.ssoauth2.exception;

/**
 * An exception that is thrown when the configuration of an OAuth2Flow is invalid.
 */
public class InvalidConfigurationException extends RuntimeException {

    public InvalidConfigurationException(final String message) {

        super(message);
    }

}
