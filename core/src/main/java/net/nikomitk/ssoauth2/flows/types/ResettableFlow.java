package net.nikomitk.ssoauth2.flows.types;

/**
 * Interface representing a flow that can be reset. This allows the flow to be restarted or reinitialized as needed.
 */
public interface ResettableFlow extends OAuth2Flow {

    /**
     * Resets the flow, allowing it to be restarted or reinitialized.
     */
    void reset();

}
