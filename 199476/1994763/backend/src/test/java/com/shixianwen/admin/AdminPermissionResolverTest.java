package com.shixianwen.admin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

    @ParameterizedTest
    @CsvSource(delimiter = ';', textBlock = """
        GET;/api/admin/dashboard;DASHBOARD_VIEW
        GET;/api/admin/platform-fee;PLATFORM_FEE_VIEW
        PUT;/api/admin/platform-fee;PLATFORM_FEE_EDIT
        GET;/api/admin/invitation-campaign;INVITATION_CAMPAIGN_VIEW
        PUT;/api/admin/invitation-campaign;INVITATION_CAMPAIGN_EDIT
        GET;/api/admin/invitations;INVITATION_REVIEW_VIEW
        GET;/api/admin/invitations/8/identity-materials;INVITATION_REVIEW_VIEW
        GET;/api/admin/invitations/8/invitee-handheld-material;INVITATION_REVIEW_VIEW
        POST;/api/admin/invitations/8/review;INVITATION_REVIEW
        GET;/api/admin/users;USER_VIEW
        PATCH;/api/admin/users/8/status;USER_STATUS
        GET;/api/admin/jobs;JOB_VIEW
        GET;/api/admin/job-options;JOB_VIEW|OFFLINE_APPOINTMENT_PROCESS
        POST;/api/admin/jobs;JOB_CREATE
        PUT;/api/admin/jobs/8;JOB_EDIT
        DELETE;/api/admin/jobs/8;JOB_DELETE
        GET;/api/admin/certifications;CERTIFICATION_VIEW
        GET;/api/admin/certifications/8/materials;CERTIFICATION_VIEW
        POST;/api/admin/certifications/8/review;CERTIFICATION_REVIEW
        PATCH;/api/admin/certifications/8/enabled;CERTIFICATION_TOGGLE
        PUT;/api/admin/certifications/8;CERTIFICATION_EDIT
        DELETE;/api/admin/certifications/8;CERTIFICATION_DELETE
        GET;/api/admin/job-certification-appointments;OFFLINE_APPOINTMENT_VIEW
        GET;/api/admin/job-certification-appointments/8/materials;OFFLINE_APPOINTMENT_VIEW
        POST;/api/admin/job-certification-appointments/8/process;OFFLINE_APPOINTMENT_PROCESS
        GET;/api/admin/inquiries;INQUIRY_VIEW
        GET;/api/admin/withdrawals;WITHDRAWAL_VIEW
        PATCH;/api/admin/withdrawals/8/status;WITHDRAWAL_PROCESS
        POST;/api/admin/withdrawals/export;WITHDRAWAL_EXPORT
        GET;/api/admin/withdrawals/export/BATCH1;WITHDRAWAL_EXPORT
        GET;/api/admin/feedback;FEEDBACK_VIEW
        PATCH;/api/admin/feedback/8/status;FEEDBACK_PROCESS
        GET;/api/admin/cooperations;COOPERATION_VIEW
        PATCH;/api/admin/cooperations/8/status;COOPERATION_PROCESS
        GET;/api/admin/customer-service/conversations;CUSTOMER_SERVICE_VIEW
        PUT;/api/admin/customer-service/users/8/read;CUSTOMER_SERVICE_READ
        POST;/api/admin/customer-service/users/8/reply;CUSTOMER_SERVICE_REPLY
        GET;/api/admin/audit-logs;AUDIT_LOG_VIEW
        GET;/api/admin/discovery;DISCOVERY_VIEW
        POST;/api/admin/discovery/categories;DISCOVERY_CREATE
        PUT;/api/admin/discovery/categories/8;DISCOVERY_EDIT
        DELETE;/api/admin/discovery/categories/8;DISCOVERY_DELETE
        GET;/api/admin/experience-library;EXPERIENCE_VIEW
        GET;/api/admin/discovery/experiences/8/users;EXPERIENCE_VIEW
        PATCH;/api/admin/discovery/certifications/8/experience;EXPERIENCE_RELATE_USER
        POST;/api/admin/discovery/experiences;EXPERIENCE_CREATE
        PUT;/api/admin/discovery/experiences/8;EXPERIENCE_EDIT
        DELETE;/api/admin/discovery/experiences/8;EXPERIENCE_DELETE
        GET;/api/admin/announcements;ANNOUNCEMENT_VIEW
        POST;/api/admin/announcements;ANNOUNCEMENT_CREATE
        PUT;/api/admin/announcements/8;ANNOUNCEMENT_EDIT
        POST;/api/admin/announcements/8/publish;ANNOUNCEMENT_PUBLISH
        POST;/api/admin/announcements/8/withdraw;ANNOUNCEMENT_WITHDRAW
        DELETE;/api/admin/announcements/8;ANNOUNCEMENT_DELETE
        GET;/api/admin/banners;BANNER_VIEW
        POST;/api/admin/banners/images;BANNER_UPLOAD
        POST;/api/admin/banners;BANNER_CREATE
        PUT;/api/admin/banners/8;BANNER_EDIT
        PATCH;/api/admin/banners/8/enabled;BANNER_TOGGLE
        DELETE;/api/admin/banners/8;BANNER_DELETE
        GET;/api/admin/app-versions;APP_VERSION_VIEW
        POST;/api/admin/app-versions;APP_VERSION_CREATE
        PUT;/api/admin/app-versions/8;APP_VERSION_EDIT
        POST;/api/admin/app-versions/8/publish;APP_VERSION_PUBLISH
        DELETE;/api/admin/app-versions/8;APP_VERSION_DELETE
        GET;/api/admin/app-test-accounts;APP_TEST_ACCOUNT_VIEW
        POST;/api/admin/app-test-accounts;APP_TEST_ACCOUNT_CREATE
        PUT;/api/admin/app-test-accounts/8;APP_TEST_ACCOUNT_EDIT
        DELETE;/api/admin/app-test-accounts/8;APP_TEST_ACCOUNT_DELETE
        GET;/api/admin/security-events;SECURITY_EVENT_VIEW
        PATCH;/api/admin/security-events/8/review;SECURITY_EVENT_REVIEW
        POST;/api/admin/auth/realtime-ticket;CUSTOMER_SERVICE_VIEW
        """)
    void resolvesEveryAdminBusinessBoundary(String method, String path, String permission) {
        assertThat(resolver.resolve(method, path)).isEqualTo(permission);
    }
}
