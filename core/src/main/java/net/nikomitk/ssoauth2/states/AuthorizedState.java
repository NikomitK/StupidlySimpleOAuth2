package net.nikomitk.ssoauth2.states;

import net.nikomitk.ssoauth2.dto.OAuth2AccessToken;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Interface representing a state in the OAuth2 flow where the user has been authorized and an access token is available. This state allows retrieval of the access token.
 */
public interface AuthorizedState extends OAuth2FlowState {

    /**
     * Retrieves the access token associated with this authorized state.
     *
     * @return the OAuth2AccessToken representing the access token.
     */
    @Contract(pure = true)
    @NotNull
    OAuth2AccessToken getAccessToken();

}
