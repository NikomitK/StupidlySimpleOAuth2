package net.nikomitk.ssoauth2.exception;

/**
 * An exception that is thrown when a method is called on the OAuth2Flow that is not allowed in the current state of the flow.
 */
public class OAuth2FlowWrongStateException extends RuntimeException {

    /**
     * Creates a new OAuth2FlowWrongStateException describing the current state and expected states.
     *
     * @param currentState   the name of the current flow state
     * @param possibleStates the names of the states in which the operation would be allowed
     */
    public OAuth2FlowWrongStateException(final String currentState, final String[] possibleStates) {

        super(String.format("The current state is %s, but the possible states are %s", currentState, String.join(", ", possibleStates)));
    }

    /**
     * Creates a new OAuth2FlowWrongStateException with a default message.
     */
    public OAuth2FlowWrongStateException() {

        super("The flow should be in a different state for that operation.");
    }

}
