package net.nikomitk.ssoauth2.states;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;

public record StatelessFlowState() implements OAuth2FlowState {

    @Override
    @Contract(pure = true)
    public @NotNull @Unmodifiable Set<Class<? extends OAuth2FlowState>> getPossibleNextStates() {

        return Set.of();
    }

}
