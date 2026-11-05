package net.nikomitk.ssoauth2.states;

import java.util.Set;

public interface OAuth2FlowState {

    Set<Class<? extends OAuth2FlowState>> getPossibleNextStates();

}
