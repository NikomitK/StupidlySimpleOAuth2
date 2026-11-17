package net.nikomitk.ssoauth2.pubclient.authcodeflow.states;

import lombok.NonNull;
import net.nikomitk.ssoauth2.dto.OAuth2AccessToken;
import net.nikomitk.ssoauth2.states.AuthorizedState;
import net.nikomitk.ssoauth2.states.OAuth2FlowState;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;

public record AccessTokenPresent(
        @NonNull OAuth2AccessToken accessToken) implements AuthCodeFlowState, AuthorizedState {

    @Override
    @Contract(pure = true)
    public @NotNull OAuth2AccessToken getAccessToken() {

        return accessToken;
    }

    @Override
    @Contract(pure = true)
    public @NotNull @Unmodifiable Set<Class<? extends OAuth2FlowState>> getPossibleNextStates() {

        return Set.of(Initial.class);
    }

}