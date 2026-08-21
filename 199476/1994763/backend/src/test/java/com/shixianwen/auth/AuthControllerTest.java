package com.shixianwen.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthControllerTest {
    @Test
    void recognizesNativeAppPlatforms() {
        assertTrue(AuthController.isAppClient("android"));
        assertTrue(AuthController.isAppClient("ios"));
        assertTrue(AuthController.isAppClient("app"));
        assertTrue(AuthController.isAppClient(" Android "));
    }

    @Test
    void rejectsNonAppPlatforms() {
        assertFalse(AuthController.isAppClient(null));
        assertFalse(AuthController.isAppClient(""));
        assertFalse(AuthController.isAppClient("web"));
    }
}
