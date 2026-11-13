package net.nikomitk.ssoauth2.exception;

/**
 * An exception that is thrown when a method is called on the OAuth2Flow that is not allowed in the current state of the flow.
 */
public class OAuth2FlowWrongStateException extends RuntimeException {

    public OAuth2FlowWrongStateException(final String currentState, final String[] possibleStates) {

        super(String.format("The current state is %s, but the possible states are %s", currentState, String.join(", ", possibleStates)));
    }

}
