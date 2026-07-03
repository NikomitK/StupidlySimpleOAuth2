package net.nikomitk.ssoauth2.exception;

public class OAuth2FlowNotReadyException extends RuntimeException {

    public OAuth2FlowNotReadyException(String message) {

        super(message);
    }

    public OAuth2FlowNotReadyException(String message, Throwable cause) {
        super(message, cause);
    }

    public OAuth2FlowNotReadyException() {
        super("This flow is not ready for this step yet!");
    }

}
