package net.nikomitk.ssoauth2.security;

import net.nikomitk.ssoauth2.exception.StandardValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PKCETest {

    // Example from RFC 7636, Appendix B
    private static final String RFC_CODE_VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
    private static final String RFC_S256_CODE_CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

    @Test
    void s256CodeChallengeMatchesRfcExample() {

        assertEquals(RFC_S256_CODE_CHALLENGE, PKCE.S256.generateCodeChallenge(RFC_CODE_VERIFIER));
    }

    @Test
    void plainCodeChallengeEqualsCodeVerifier() {

        assertEquals(RFC_CODE_VERIFIER, PKCE.PLAIN.generateCodeChallenge(RFC_CODE_VERIFIER));
    }

    @ParameterizedTest
    @CsvSource({"PLAIN, plain", "S256, S256"})
    void codeChallengeMethodMatchesRfcName(final PKCE pkce, final String expectedMethod) {

        assertEquals(expectedMethod, pkce.getCodeChallengeMethod());
    }

    @ParameterizedTest
    @CsvSource({"32, 43", "64, 86", "96, 128"})
    void generateCodeVerifierReturnsBase64UrlOfByteCount(final int byteCount, final int expectedLength) {

        assertEquals(expectedLength, PKCE.generateCodeVerifier(byteCount)
                                         .length());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 31, 97})
    void generateCodeVerifierRejectsByteCountOutsideRfcRange(final int byteCount) {

        assertThrows(StandardValidationException.class, () -> PKCE.generateCodeVerifier(byteCount));
    }

    @Test
    void generateCodeVerifierUsesOnlyUnreservedCharacters() {

        assertTrue(PKCE.generateCodeVerifier(95)
                       .matches("[A-Za-z0-9_-]+"));
    }

}
