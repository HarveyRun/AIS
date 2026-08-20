import { useCallback, useEffect, useState } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { adminApi, token } from '../api/adminApi.js';
import AdminLayout from '../components/layout/AdminLayout.jsx';
import LoginPage from '../pages/auth/LoginPage.jsx';
import DashboardPage from '../pages/dashboard/DashboardPage.jsx';
import UsersPage from '../pages/users/UsersPage.jsx';
import RecordsPage from '../pages/records/RecordsPage.jsx';
import AuditPage from '../pages/audit/AuditPage.jsx';
import CustomerServicePage from '../pages/customerService/CustomerServicePage.jsx';
import DiscoveryManagementPage from '../pages/discovery/DiscoveryManagementPage.jsx';
import JobsPage from '../pages/jobs/JobsPage.jsx';
import ExperiencesPage from '../pages/experiences/ExperiencesPage.jsx';
import AppTestAccountPage from '../pages/appTestAccount/AppTestAccountPage.jsx';
import AppVersionPage from '../pages/appVersion/AppVersionPage.jsx';
import AnnouncementsPage from '../pages/announcements/AnnouncementsPage.jsx';
import BannersPage from '../pages/banners/BannersPage.jsx';
import SecurityEventsPage from '../pages/security/SecurityEventsPage.jsx';
import PlatformFeePage from '../pages/platformFee/PlatformFeePage.jsx';
import AdminUsersPage from '../pages/adminUsers/AdminUsersPage.jsx';
import AdminRolesPage from '../pages/adminRoles/AdminRolesPage.jsx';
import AdminPermissionsPage from '../pages/adminPermissions/AdminPermissionsPage.jsx';
import GlobalLoading from '../components/feedback/GlobalLoading.jsx';
import useAdminRealtimeConnection from '../hooks/useAdminRealtimeConnection.js';
import { useAdminAccess } from './AdminAccessContext.jsx';
export default function App() {
  const [authed, setAuthed] = useState(() => Boolean(token.get()));
  const [admin, setAdmin] = useState(null);
  const [customerServiceEvent, setCustomerServiceEvent] = useState(null);
  const [customerServiceUnread, setCustomerServiceUnread] = useState(0);

  const handleRealtimeEvent = useCallback((event) => {
    if (event.type === 'CONNECTED') {
      adminApi.customerServiceConversations({ silent: true })
        .then((items) => setCustomerServiceUnread(
          items.reduce((total, item) => total + Number(item.unread || 0), 0),
        ))
        .catch(() => {});
      return;
    }
    if (event.type === 'CUSTOMER_SERVICE_MESSAGE') {
      setCustomerServiceEvent(event.payload);
      setCustomerServiceUnread((current) => current + 1);
    }
  }, []);

  const permissions = new Set(admin?.permissions || []);
  const canUseCustomerService = permissions.has('*') || permissions.has('CUSTOMER_SERVICE_VIEW');
  useAdminRealtimeConnection(authed && canUseCustomerService, handleRealtimeEvent);
  useEffect(() => {
    if (!authed) {
      setAdmin(null);
      return;
    }
    adminApi.me().then(setAdmin).catch(() => {});
  }, [authed]);
  useEffect(() => {
    const unauthorized = () => setAuthed(false);
    window.addEventListener('shixianwen-admin-unauthorized', unauthorized);
    return () => window.removeEventListener('shixianwen-admin-unauthorized', unauthorized);
  }, []);
  return (
    <>
      <Routes>
      <Route
        path="/login"
        element={
          authed ? (
            <Navigate to="/dashboard" replace />
          ) : (
            <LoginPage onAuthenticated={() => setAuthed(true)} />
          )
        }
      />
      <Route
        element={
          authed ? (
            <AdminLayout
              adminData={admin}
              onLoggedOut={() => setAuthed(false)}
              customerServiceUnread={customerServiceUnread}
            />
          ) : (
            <Navigate to="/login" replace />
          )
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<Guard permission="DASHBOARD_VIEW"><DashboardPage /></Guard>} />
        <Route path="/users" element={<Guard permission="USER_VIEW"><UsersPage /></Guard>} />
        <Route path="/announcements" element={<Guard permission="ANNOUNCEMENT_VIEW"><AnnouncementsPage /></Guard>} />
        <Route path="/banners" element={<Guard permission="BANNER_VIEW"><BannersPage /></Guard>} />
        <Route path="/platform-fee" element={<Guard permission="PLATFORM_FEE_VIEW"><PlatformFeePage /></Guard>} />
        <Route path="/jobs" element={<Guard permission="JOB_VIEW"><JobsPage /></Guard>} />
        <Route path="/experiences" element={<Guard permission="EXPERIENCE_VIEW"><ExperiencesPage /></Guard>} />
        <Route path="/certifications" element={<Guard permission="CERTIFICATION_VIEW"><RecordsPage type="certifications" /></Guard>} />
        <Route path="/discovery" element={<Guard permission="DISCOVERY_VIEW"><DiscoveryManagementPage /></Guard>} />
        <Route path="/inquiries" element={<Guard permission="INQUIRY_VIEW"><RecordsPage type="inquiries" /></Guard>} />
        <Route path="/withdrawals" element={<Guard permission="WITHDRAWAL_VIEW"><RecordsPage type="withdrawals" /></Guard>} />
        <Route path="/feedback" element={<Guard permission="FEEDBACK_VIEW"><RecordsPage type="feedback" /></Guard>} />
        <Route path="/cooperations" element={<Guard permission="COOPERATION_VIEW"><RecordsPage type="cooperations" /></Guard>} />
        <Route
          path="/customer-service"
          element={
            <Guard permission="CUSTOMER_SERVICE_VIEW"><CustomerServicePage
              realtimeEvent={customerServiceEvent}
              onUnreadChange={setCustomerServiceUnread}
            /></Guard>
          }
        />
        <Route path="/audit" element={<Guard permission="AUDIT_LOG_VIEW"><AuditPage /></Guard>} />
        <Route path="/security-events" element={<Guard permission="SECURITY_EVENT_VIEW"><SecurityEventsPage /></Guard>} />
        <Route path="/app-test-account" element={<Guard permission="APP_TEST_ACCOUNT_VIEW"><AppTestAccountPage /></Guard>} />
        <Route path="/app-versions" element={<Guard permission="APP_VERSION_VIEW"><AppVersionPage /></Guard>} />
        <Route path="/admin-users" element={<Guard permission="ADMIN_USER_VIEW"><AdminUsersPage /></Guard>} />
        <Route path="/admin-roles" element={<Guard permission="ROLE_VIEW"><AdminRolesPage /></Guard>} />
        <Route path="/admin-permissions" element={<Guard permission="PERMISSION_VIEW"><AdminPermissionsPage /></Guard>} />
      </Route>
      <Route path="*" element={<Navigate to={authed ? '/dashboard' : '/login'} replace />} />
      </Routes>
      <GlobalLoading />
    </>
  );
}

function Guard({ permission, children }) {
  const { admin, can } = useAdminAccess();
  if (!admin) return null;
  if (can(permission)) return children;
  return <section className="table-card" style={{ minHeight: 220 }}><div className="empty">当前账号没有访问此页面的权限</div></section>;
}
