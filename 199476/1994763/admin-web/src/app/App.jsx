import { useEffect, useState } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { token } from '../api/adminApi.js';
import AdminLayout from '../components/layout/AdminLayout.jsx';
import LoginPage from '../pages/auth/LoginPage.jsx';
import DashboardPage from '../pages/dashboard/DashboardPage.jsx';
import UsersPage from '../pages/users/UsersPage.jsx';
import RecordsPage from '../pages/records/RecordsPage.jsx';
import AuditPage from '../pages/audit/AuditPage.jsx';
import CustomerServicePage from '../pages/customerService/CustomerServicePage.jsx';
import DiscoveryManagementPage from '../pages/discovery/DiscoveryManagementPage.jsx';
export default function App() {
  const [authed, setAuthed] = useState(() => Boolean(token.get()));
  useEffect(() => {
    const unauthorized = () => setAuthed(false);
    window.addEventListener('shixianwen-admin-unauthorized', unauthorized);
    return () => window.removeEventListener('shixianwen-admin-unauthorized', unauthorized);
  }, []);
  return (
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
            <AdminLayout onLoggedOut={() => setAuthed(false)} />
          ) : (
            <Navigate to="/login" replace />
          )
        }
      >
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/users" element={<UsersPage />} />
        <Route path="/certifications" element={<RecordsPage type="certifications" />} />
        <Route path="/discovery" element={<DiscoveryManagementPage />} />
        <Route path="/inquiries" element={<RecordsPage type="inquiries" />} />
        <Route path="/withdrawals" element={<RecordsPage type="withdrawals" />} />
        <Route path="/feedback" element={<RecordsPage type="feedback" />} />
        <Route path="/cooperations" element={<RecordsPage type="cooperations" />} />
        <Route path="/customer-service" element={<CustomerServicePage />} />
        <Route path="/audit" element={<AuditPage />} />
      </Route>
      <Route path="*" element={<Navigate to={authed ? '/dashboard' : '/login'} replace />} />
    </Routes>
  );
}
