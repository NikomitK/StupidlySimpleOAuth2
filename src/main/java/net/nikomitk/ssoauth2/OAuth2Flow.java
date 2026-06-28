package net.nikomitk.ssoauth2;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class OAuth2Flow {

    protected ObjectMapper objectMapper;

    protected OAuth2Flow() {

        objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    public abstract OAuth2Flow startFlow();

    public abstract OAuth2Flow requestAccessToken();

    public abstract OAuth2Flow refreshAccessToken();

    public abstract String getAccessToken();

}
