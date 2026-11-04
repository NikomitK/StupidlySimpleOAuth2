package net.nikomitk.ssoauth2.exception;

public class OAuth2FlowWrongStateException extends RuntimeException{
    
    public OAuth2FlowWrongStateException(final String currentState, final String[] possibleStates) {
        super(String.format("The current state is %s, but the possible states are %s", currentState, String.join(", ", possibleStates)));
    }
}
