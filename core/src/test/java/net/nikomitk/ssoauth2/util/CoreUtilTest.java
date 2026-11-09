package net.nikomitk.ssoauth2.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CoreUtilTest {

    @Test
    void generateRandomStringTest() {
        assertEquals(0, CoreUtil.generateRandomString(0)
                                .length());
        assertEquals(43, CoreUtil.generateRandomString(32)
                                 .length());
        assertEquals(128, CoreUtil.generateRandomString(96)
                                 .length());
    }

}
