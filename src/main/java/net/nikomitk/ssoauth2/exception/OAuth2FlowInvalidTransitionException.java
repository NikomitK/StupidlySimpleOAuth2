package net.nikomitk.ssoauth2.exception;

public class OAuth2FlowInvalidTransitionException extends RuntimeException {

    public OAuth2FlowInvalidTransitionException(String message) {

        super(message);
    }

    public OAuth2FlowInvalidTransitionException(String message, Throwable cause) {

        super(message, cause);
    }

    public OAuth2FlowInvalidTransitionException(Throwable cause) {

        super(cause);
    }

}
