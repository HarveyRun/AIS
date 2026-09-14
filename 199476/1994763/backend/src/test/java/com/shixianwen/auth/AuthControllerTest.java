package com.shixianwen.auth;

import com.shixianwen.common.BusinessException;
import com.shixianwen.network.ClientNetworkService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void logoutRequiresBearerToken() {
        AuthController controller = new AuthController(mock(AuthService.class), mock(ClientNetworkService.class));

        BusinessException error = assertThrows(BusinessException.class, () -> controller.logout(null));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatus());
    }

    @Test
    void logoutAuthenticatesTokenBeforeDeletingSession() {
        AuthService authService = mock(AuthService.class);
        when(authService.authenticate("valid-token")).thenReturn(new com.shixianwen.user.User());
        AuthController controller = new AuthController(authService, mock(ClientNetworkService.class));

        controller.logout("Bearer valid-token");

        verify(authService).authenticate("valid-token");
        verify(authService).logout("valid-token");
    }
}
