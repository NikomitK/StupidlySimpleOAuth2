package net.nikomitk.ssoauth2.states;

import net.nikomitk.ssoauth2.OAuth2FlowState;

public interface StartableState extends OAuth2FlowState {

    void start();

}
