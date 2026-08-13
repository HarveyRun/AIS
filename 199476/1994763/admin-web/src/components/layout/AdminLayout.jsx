import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
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
  BriefcaseBusiness,
  Footprints,
  ChevronDown,
  Settings2,
  Ellipsis,
  ShieldCog,
} from 'lucide-react';
import { adminApi, token } from '../../api/adminApi.js';
import './AdminLayout.css';
const primaryItems = [
  ['/dashboard', '概览', LayoutDashboard],
  ['/users', '用户管理', Users],
  ['/certifications', '认证审核', ShieldCheck],
  ['/inquiries', '询问管理', MessagesSquare],
  ['/withdrawals', '提现处理', WalletCards],
  ['/customer-service', '在线客服', Headset],
];

const secondaryGroups = [
  {
    id: 'business-settings',
    label: '业务配置',
    icon: Settings2,
    items: [
      ['/jobs', '岗位管理', BriefcaseBusiness],
      ['/experiences', '经历管理', Footprints],
      ['/discovery', '分类管理', Tags],
    ],
  },
  {
    id: 'other-business',
    label: '其他业务',
    icon: Ellipsis,
    items: [
      ['/feedback', '投诉反馈', MessageSquareWarning],
      ['/cooperations', '商务合作', Handshake],
    ],
  },
  {
    id: 'system-management',
    label: '系统管理',
    icon: ShieldCog,
    items: [['/audit', '操作记录', ScrollText]],
  },
];

export default function AdminLayout({ onLoggedOut, customerServiceUnread = 0 }) {
  const navigate = useNavigate();
  const location = useLocation();
  const [adminName, setAdminName] = useState('管理员');
  const [expandedGroups, setExpandedGroups] = useState(() => {
    const activeGroup = secondaryGroups.find((group) =>
      group.items.some(([to]) => location.pathname.startsWith(to)),
    );
    return activeGroup ? [activeGroup.id] : [];
  });
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

  const toggleGroup = (groupId) => {
    setExpandedGroups((current) =>
      current.includes(groupId) ? current.filter((id) => id !== groupId) : [...current, groupId],
    );
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
          {primaryItems.map(([to, label, Icon]) => (
            <NavLink key={to} to={to}>
              <Icon />
              <span>{label}</span>
              {to === '/customer-service' && customerServiceUnread > 0 && (
                <em className="admin-nav-count">
                  {customerServiceUnread > 99 ? '99+' : customerServiceUnread}
                </em>
              )}
            </NavLink>
          ))}
          <div className="nav-divider">
            <span>更多管理</span>
          </div>
          {secondaryGroups.map((group) => {
            const GroupIcon = group.icon;
            const expanded = expandedGroups.includes(group.id);
            const active = group.items.some(([to]) => location.pathname.startsWith(to));
            return (
              <div className={`nav-group ${active ? 'active' : ''}`} key={group.id}>
                <button
                  type="button"
                  className="nav-group-trigger"
                  aria-expanded={expanded}
                  onClick={() => toggleGroup(group.id)}
                >
                  <GroupIcon />
                  <span>{group.label}</span>
                  <ChevronDown className={expanded ? 'expanded' : ''} />
                </button>
                {expanded && (
                  <div className="nav-group-items">
                    {group.items.map(([to, label, Icon]) => (
                      <NavLink key={to} to={to}>
                        <Icon />
                        <span>{label}</span>
                      </NavLink>
                    ))}
                  </div>
                )}
              </div>
            );
          })}
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
