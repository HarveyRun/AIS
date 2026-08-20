package com.shixianwen.admin;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminPermissionResolverTest {
    private final AdminPermissionResolver resolver = new AdminPermissionResolver();

    @Test
    void resolvesButtonLevelPermissionsForCoreResources() {
        assertThat(resolver.resolve("GET", "/api/admin/admin-users")).isEqualTo("ADMIN_USER_VIEW");
        assertThat(resolver.resolve("POST", "/api/admin/admin-users")).isEqualTo("ADMIN_USER_CREATE");
        assertThat(resolver.resolve("PUT", "/api/admin/admin-users/9")).isEqualTo("ADMIN_USER_EDIT");
        assertThat(resolver.resolve("PUT", "/api/admin/admin-users/9/roles")).isEqualTo("ADMIN_USER_ASSIGN_ROLE");
        assertThat(resolver.resolve("POST", "/api/admin/admin-users/9/reset-password")).isEqualTo("ADMIN_USER_RESET_PASSWORD");
        assertThat(resolver.resolve("DELETE", "/api/admin/admin-users/9")).isEqualTo("ADMIN_USER_DELETE");

        assertThat(resolver.resolve("PUT", "/api/admin/roles/2/permissions")).isEqualTo("ROLE_ASSIGN_PERMISSION");
        assertThat(resolver.resolve("GET", "/api/admin/roles/options")).isEqualTo("ROLE_VIEW|ADMIN_USER_ASSIGN_ROLE");
        assertThat(resolver.resolve("GET", "/api/admin/permissions/options")).isEqualTo("PERMISSION_VIEW|ROLE_ASSIGN_PERMISSION");
    }

    @Test
    void separatesExperienceLibraryFromDiscoveryCategoryPermissions() {
        assertThat(resolver.resolve("GET", "/api/admin/experience-library")).isEqualTo("EXPERIENCE_VIEW");
        assertThat(resolver.resolve("POST", "/api/admin/discovery/experiences")).isEqualTo("EXPERIENCE_CREATE");
        assertThat(resolver.resolve("PUT", "/api/admin/discovery/experiences/12")).isEqualTo("EXPERIENCE_EDIT");
        assertThat(resolver.resolve("DELETE", "/api/admin/discovery/experiences/12")).isEqualTo("EXPERIENCE_DELETE");
        assertThat(resolver.resolve("POST", "/api/admin/discovery/matters")).isEqualTo("DISCOVERY_CREATE");
    }

    @Test
    void rejectsUnregisteredAdminEndpoints() {
        assertThat(resolver.resolve("GET", "/api/admin/not-registered")).isEqualTo("__DENY__");
    }
}
