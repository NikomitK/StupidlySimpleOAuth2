package net.nikomitk.ssoauth2.pubclient.authcodeflow.states;

import net.nikomitk.ssoauth2.states.OAuth2FlowState;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;

public record Initial(@Nullable Object error) implements AuthCodeFlowState {

    @Override
    @Contract(pure = true)
    public @NotNull @Unmodifiable Set<Class<? extends OAuth2FlowState>> getPossibleNextStates() {

        return Set.of(Initial.class, AwaitingAuthCode.class);
    }

    public Initial() {
        this(null);
    }
}