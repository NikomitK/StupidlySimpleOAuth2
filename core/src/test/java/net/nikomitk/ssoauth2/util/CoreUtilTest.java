package net.nikomitk.ssoauth2.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoreUtilTest {

    @ParameterizedTest
    @CsvSource({"0, 0", "32, 43", "96, 128"})
    void generateRandomStringReturnsUnpaddedBase64UrlOfByteCount(final int byteCount, final int expectedLength) {

        assertEquals(expectedLength, CoreUtil.generateRandomString(byteCount)
                                             .length());
    }

}
