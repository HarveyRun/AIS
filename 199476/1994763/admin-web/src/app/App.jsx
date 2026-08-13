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
import GlobalLoading from '../components/feedback/GlobalLoading.jsx';
import useAdminRealtimeConnection from '../hooks/useAdminRealtimeConnection.js';
export default function App() {
  const [authed, setAuthed] = useState(() => Boolean(token.get()));
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

  useAdminRealtimeConnection(authed, handleRealtimeEvent);
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
              onLoggedOut={() => setAuthed(false)}
              customerServiceUnread={customerServiceUnread}
            />
          ) : (
            <Navigate to="/login" replace />
          )
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/users" element={<UsersPage />} />
        <Route path="/jobs" element={<JobsPage />} />
        <Route path="/experiences" element={<ExperiencesPage />} />
        <Route path="/certifications" element={<RecordsPage type="certifications" />} />
        <Route path="/discovery" element={<DiscoveryManagementPage />} />
        <Route path="/inquiries" element={<RecordsPage type="inquiries" />} />
        <Route path="/withdrawals" element={<RecordsPage type="withdrawals" />} />
        <Route path="/feedback" element={<RecordsPage type="feedback" />} />
        <Route path="/cooperations" element={<RecordsPage type="cooperations" />} />
        <Route
          path="/customer-service"
          element={
            <CustomerServicePage
              realtimeEvent={customerServiceEvent}
              onUnreadChange={setCustomerServiceUnread}
            />
          }
        />
        <Route path="/audit" element={<AuditPage />} />
      </Route>
      <Route path="*" element={<Navigate to={authed ? '/dashboard' : '/login'} replace />} />
      </Routes>
      <GlobalLoading />
    </>
  );
}
