package net.nikomitk.ssoauth2.states;

public interface ClosableState extends OAuth2FlowState {

    void close();

}
