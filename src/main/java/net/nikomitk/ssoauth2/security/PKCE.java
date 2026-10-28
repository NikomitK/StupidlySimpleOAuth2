package net.nikomitk.ssoauth2.security;

import lombok.Getter;
import net.nikomitk.ssoauth2.exception.StandardValidationException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.function.Function;

/**
 * Enum for implementing the PKCE Extension from RFC7636.
 * Supports both plain and S256 code challenge methods.
 *
 * @see <a href="https://www.rfc-editor.org/info/rfc7636>RFC7636</a>
 */
public enum PKCE {

    /**
     * Implementation of the plain code challenge method. It just returns the input string.
     * According to RFC7636, Section 4.2, if the client is capable of the S256 code challenge method, it MUST use S256.
     * Keep this in mind if you want to use the plain method.
     *
     * @see <a href=https://www.rfc-editor.org/info/rfc7636/#section-4.2>RFC7636 Section 4.2</a>
     */
    PLAIN(PKCE::createPlainCodeChallenge, "plain"),
    /**
     * Implementation of the S256 code challenge method. Hashes the code verifier with SHA256 before sending it to the Authorization server.
     * Your platform has to support the SHA-256 MethodDigest provider.
     * Since it is an official requirement for Java platform implementations (at least for Java 25) to support SHA-256, this should be supported.
     *
     * @see <a href=https://www.rfc-editor.org/info/rfc7636/#section-4.2>RFC7636 Section 4.2</a>
     */
    S256(PKCE::createS256CodeChallenge, "S256");

    private final Function<String, String> codeChallengeGenerator;
    @Getter
    private final String codeChallengeMethod;

    PKCE(final Function<String, String> codeChallengeGenerator, final String codeChallengeMethod) {

        this.codeChallengeGenerator = codeChallengeGenerator;
        this.codeChallengeMethod = codeChallengeMethod;
    }

    public static String generateCodeVerifier(final int randomByteCount) {

        if (randomByteCount < 32 || randomByteCount > 95) {
            throw new StandardValidationException(7636, "https://www.rfc-editor.org/info/rfc7636/#section-4.1", "The PKCE code verifier has to be between 43 characters (32 bytes) and 128 characters (95 bytes) of Base64.");
        }
        final byte[] bytes = new byte[randomByteCount];
        new SecureRandom().nextBytes(bytes);
        return Base64
                .getUrlEncoder()
                .encodeToString(bytes);
    }

    private static String createS256CodeChallenge(final String codeVerifier) {

        final byte[] asciiCodeVerifier = StandardCharsets.US_ASCII
                .encode(codeVerifier)
                .array();
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (final NoSuchAlgorithmException e) {
            // This should not happen, as according to the MessageDigest class, every implementation of the Java platform is required to support SHA-256.
            throw new RuntimeException(e);
        }
        digest.update(asciiCodeVerifier);
        final byte[] codeChallengeBytes = digest.digest();
        return Base64
                .getUrlEncoder()
                .encodeToString(codeChallengeBytes);
    }

    private static String createPlainCodeChallenge(final String codeVerifier) {

        return codeVerifier;
    }

    public String generateCodeChallenge(final String codeVerifier) {

        return this.codeChallengeGenerator.apply(codeVerifier);
    }

}
