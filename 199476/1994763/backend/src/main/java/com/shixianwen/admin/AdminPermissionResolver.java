package com.shixianwen.admin;

import org.springframework.stereotype.Component;

@Component
public class AdminPermissionResolver {
    public String resolve(String method, String path) {
        if (path.equals("/api/admin/auth/realtime-ticket")) return "CUSTOMER_SERVICE_VIEW";
        if (path.startsWith("/api/admin/auth/")) return null;
        if (path.equals("/api/admin/dashboard") && isGet(method)) return "DASHBOARD_VIEW";
        if (path.equals("/api/admin/analytics/raw") && isGet(method)) return "ANALYTICS_RAW_VIEW";
        if (path.startsWith("/api/admin/analytics/") && isGet(method)) return "ANALYTICS_VIEW";
        if (path.equals("/api/admin/platform-fee")) {
            return isGet(method) ? "PLATFORM_FEE_VIEW" : "PLATFORM_FEE_EDIT";
        }
        if (path.equals("/api/admin/inquiry-capacity")) {
            return isGet(method) ? "INQUIRY_CAPACITY_VIEW" : "INQUIRY_CAPACITY_EDIT";
        }
        if (path.equals("/api/admin/app-settings")) {
            return isGet(method) ? "APP_GLOBAL_SETTING_VIEW" : "APP_GLOBAL_SETTING_EDIT";
        }
        if (path.startsWith("/api/admin/curated-chat/applications")) {
            if (path.endsWith("/identity-review")) return "CURATED_CHAT_IDENTITY_REVIEW";
            if (path.endsWith("/job-review")) return "CURATED_CHAT_JOB_REVIEW";
            return "CURATED_CHAT_VIEW";
        }
        if (path.startsWith("/api/admin/curated-chat/members/")) {
            return "CURATED_CHAT_MEMBER_MANAGE";
        }
        if (path.equals("/api/admin/users") && isGet(method)) return "USER_VIEW";
        if (path.matches("/api/admin/users/\\d+/status") && "PATCH".equals(method)) return "USER_STATUS";


        if (path.equals("/api/admin/certifications") && isGet(method)) return "CERTIFICATION_VIEW";
        if (path.matches("/api/admin/certifications/\\d+/materials") && isGet(method)) return "CERTIFICATION_VIEW";
        if (path.matches("/api/admin/certifications/\\d+/review") && "POST".equals(method)) return "CERTIFICATION_REVIEW";
        if (path.matches("/api/admin/certifications/\\d+/media-processing/retry") && "POST".equals(method)) return "CERTIFICATION_REVIEW";
        if (path.matches("/api/admin/certifications/\\d+/enabled") && "PATCH".equals(method)) return "CERTIFICATION_TOGGLE";
        if (path.matches("/api/admin/certifications/\\d+") && "DELETE".equals(method)) return "CERTIFICATION_DELETE";

        if (path.equals("/api/admin/inquiries") && isGet(method)) return "INQUIRY_VIEW";
        if (path.startsWith("/api/admin/inquiry-disputes/risk-watch")) {
            return isGet(method) ? "RISK_WATCH_VIEW" : "RISK_WATCH_PROCESS";
        }
        if (path.equals("/api/admin/withdrawals") && isGet(method)) return "WITHDRAWAL_VIEW";
        if (path.matches("/api/admin/withdrawals/\\d+/status") && "PATCH".equals(method)) return "WITHDRAWAL_PROCESS";
        if (path.startsWith("/api/admin/withdrawals/export")) return "WITHDRAWAL_EXPORT";
        if (path.startsWith("/api/admin/permanent-ban-payouts")) {
            if (path.endsWith("/export") || path.matches(".*/export/[^/]+")) {
                return "PERMANENT_BAN_PAYOUT_EXPORT";
            }
            if (path.endsWith("/results") || path.endsWith("/retry")) {
                return "PERMANENT_BAN_PAYOUT_PROCESS";
            }
            return "PERMANENT_BAN_PAYOUT_VIEW";
        }
        if (path.startsWith("/api/admin/finance-reconciliation")) {
            if (path.endsWith("/run")) return "FINANCE_RECONCILIATION_RUN";
            if (path.endsWith("/alipay-bills") || path.endsWith("/withdrawal-results")) return "FINANCE_RECONCILIATION_IMPORT";
            if (path.matches(".*/differences/\\d+/resolve")) return "FINANCE_RECONCILIATION_RESOLVE";
            return "FINANCE_RECONCILIATION_VIEW";
        }
        if (path.startsWith("/api/admin/answer-quality")) {
            return "ANSWER_QUALITY_VIEW";
        }
        if (path.equals("/api/admin/feedback") && isGet(method)) return "FEEDBACK_VIEW";
        if (path.matches("/api/admin/feedback/\\d+/status")) return "FEEDBACK_PROCESS";
        if (path.equals("/api/admin/cooperations") && isGet(method)) return "COOPERATION_VIEW";
        if (path.matches("/api/admin/cooperations/\\d+/status")) return "COOPERATION_PROCESS";

        if (path.startsWith("/api/admin/customer-service/")) {
            if (path.endsWith("/reply") && "POST".equals(method)) return "CUSTOMER_SERVICE_REPLY";
            if (path.endsWith("/read") && "PUT".equals(method)) return "CUSTOMER_SERVICE_READ";
            return "CUSTOMER_SERVICE_VIEW";
        }
        if (path.equals("/api/admin/audit-logs")) return "AUDIT_LOG_VIEW";

        if (path.startsWith("/api/admin/announcements")) {
            if (path.endsWith("/publish")) return "ANNOUNCEMENT_PUBLISH";
            if (path.endsWith("/withdraw")) return "ANNOUNCEMENT_WITHDRAW";
            return resource(method, path, "ANNOUNCEMENT");
        }
        if (path.startsWith("/api/admin/banners")) {
            if (path.endsWith("/images")) return "BANNER_UPLOAD";
            if (path.endsWith("/enabled")) return "BANNER_TOGGLE";
            return resource(method, path, "BANNER");
        }
        if (path.startsWith("/api/admin/experience-categories")) {
            return resource(method, path, "EXPERIENCE_CATEGORY");
        }
        if (path.startsWith("/api/admin/app-versions")) {
            if (path.endsWith("/publish") || path.endsWith("/unpublish")) return "APP_VERSION_PUBLISH";
            return resource(method, path, "APP_VERSION");
        }
        if (path.startsWith("/api/admin/app-test-accounts")) return resource(method, path, "APP_TEST_ACCOUNT");
        if (path.startsWith("/api/admin/security-events")) {
            return path.endsWith("/review") ? "SECURITY_EVENT_REVIEW" : "SECURITY_EVENT_VIEW";
        }

        if (path.startsWith("/api/admin/admin-users")) {
            if (path.endsWith("/reset-password")) return "ADMIN_USER_RESET_PASSWORD";
            if (path.endsWith("/roles")) return "ADMIN_USER_ASSIGN_ROLE";
            return resource(method, path, "ADMIN_USER");
        }
        if (path.startsWith("/api/admin/roles")) {
            if (path.equals("/api/admin/roles/options")) return "ROLE_VIEW|ADMIN_USER_ASSIGN_ROLE";
            if (path.endsWith("/permissions")) return "ROLE_ASSIGN_PERMISSION";
            return resource(method, path, "ROLE");
        }
        if (path.equals("/api/admin/permissions/options") || path.equals("/api/admin/permissions/modules")) {
            return "PERMISSION_VIEW|ROLE_ASSIGN_PERMISSION";
        }
        if (path.startsWith("/api/admin/permissions")) return resource(method, path, "PERMISSION");
        return "__DENY__";
    }

    private String resource(String method, String path, String prefix) {
        if (isGet(method)) return prefix + "_VIEW";
        if ("POST".equals(method) && !path.matches(".*/\\d+/.*")) return prefix + "_CREATE";
        if ("PUT".equals(method) || "PATCH".equals(method)) return prefix + "_EDIT";
        if ("DELETE".equals(method)) return prefix + "_DELETE";
        return prefix + "_VIEW";
    }

    private boolean isGet(String method) {
        return "GET".equals(method);
    }
}
