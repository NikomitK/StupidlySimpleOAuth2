package net.nikomitk.ssoauth2.states;

/**
 * Interface representing a state in the OAuth2 flow that can be closed. This state should be closed when it is no longer needed.
 */
public interface ClosableState extends OAuth2FlowState {

    /**
     * Closes the state, releasing any resources or performing any necessary cleanup.
     */
    void close();

}
