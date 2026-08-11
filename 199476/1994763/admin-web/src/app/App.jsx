import { Navigate, Route, Routes } from 'react-router-dom';
import { token } from '../api/adminApi.js';
import AdminLayout from '../components/layout/AdminLayout.jsx';
import LoginPage from '../pages/auth/LoginPage.jsx';
import DashboardPage from '../pages/dashboard/DashboardPage.jsx';
import UsersPage from '../pages/users/UsersPage.jsx';
import RecordsPage from '../pages/records/RecordsPage.jsx';
import AuditPage from '../pages/audit/AuditPage.jsx';
import CustomerServicePage from '../pages/customerService/CustomerServicePage.jsx';
export default function App() {
  const authed = Boolean(token.get());
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={authed ? <AdminLayout /> : <Navigate to="/login" replace />}>
        <Route index element={<Navigate to="/dashboard" replace />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/users" element={<UsersPage />} />
        <Route path="/certifications" element={<RecordsPage type="certifications" />} />
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
