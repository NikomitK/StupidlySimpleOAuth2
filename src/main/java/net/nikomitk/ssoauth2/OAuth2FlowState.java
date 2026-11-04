package net.nikomitk.ssoauth2;

public interface OAuth2FlowState {

    default OAuth2FlowState[] getPossibleNextStates() {

        return new OAuth2FlowState[0];
    }

    default OAuth2FlowState[] getPossiblePreviousStates() {

        return new OAuth2FlowState[0];
    }

}
