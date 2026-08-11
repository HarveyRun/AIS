import { useEffect, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  Users,
  ShieldCheck,
  MessagesSquare,
  WalletCards,
  MessageSquareWarning,
  Handshake,
  ScrollText,
  Headset,
  LogOut,
  Tags,
} from 'lucide-react';
import { adminApi, token } from '../../api/adminApi.js';
import './AdminLayout.css';
const items = [
  ['/dashboard', '概览', LayoutDashboard],
  ['/users', '用户管理', Users],
  ['/certifications', '认证审核', ShieldCheck],
  ['/discovery', '内容分类', Tags],
  ['/inquiries', '询问管理', MessagesSquare],
  ['/withdrawals', '提现处理', WalletCards],
  ['/feedback', '投诉反馈', MessageSquareWarning],
  ['/cooperations', '商务合作', Handshake],
  ['/customer-service', '在线客服', Headset],
  ['/audit', '操作记录', ScrollText],
];
export default function AdminLayout({ onLoggedOut }) {
  const navigate = useNavigate();
  const [adminName, setAdminName] = useState('管理员');
  useEffect(() => {
    adminApi
      .me()
      .then((admin) => setAdminName(admin.displayName || '管理员'))
      .catch(() => {});
  }, []);
  const logout = async () => {
    try {
      await adminApi.logout();
    } catch {}
    token.set('');
    onLoggedOut();
    navigate('/login', { replace: true });
  };
  return (
    <div className="admin-shell">
      <aside>
        <div className="admin-brand">
          <i>问</i>
          <div>
            <b>事先问</b>
            <span>管理后台</span>
          </div>
        </div>
        <nav>
          {items.map(([to, label, Icon]) => (
            <NavLink key={to} to={to}>
              <Icon />
              {label}
            </NavLink>
          ))}
        </nav>
        <button className="logout" onClick={logout}>
          <LogOut />
          退出登录
        </button>
      </aside>
      <main>
        <header>
          <div>
            <b>事先问运营管理</b>
            <span>数据、审核与资金处理</span>
          </div>
          <em>{adminName}</em>
        </header>
        <div className="admin-content">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
