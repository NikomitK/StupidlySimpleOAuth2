package net.nikomitk.ssoauth2.states;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;

/**
 * Interface representing a state in the OAuth2 flow. Each state can define possible next states that can be transitioned to.
 */
public interface OAuth2FlowState {

    /**
     * Returns the set of possible next states that can be transitioned to from this state.
     *
     * @return the set of possible next states.
     */
    @Contract(pure = true)
    @NotNull
    @Unmodifiable
    Set<Class<? extends OAuth2FlowState>> getPossibleNextStates();

}
