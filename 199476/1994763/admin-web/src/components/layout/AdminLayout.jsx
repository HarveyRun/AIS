import { useEffect, useMemo, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  BellRing,
  ChartNoAxesCombined,
  ChevronDown,
  Ellipsis,
  Handshake,
  Headset,
  Images,
  ListTree,
  KeyRound,
  LayoutDashboard,
  LogOut,
  MessageSquareWarning,
  MessagesSquare,
  RefreshCw,
  ScrollText,
  Settings2,
  ShieldCheck,
  ShieldAlert,
  ShieldCog,
  Siren,
  Smartphone,
  UserRoundCog,
  Users,
  WalletCards,
  Scale,
  BadgeCheck,
  BadgeDollarSign,
  MessageSquarePlus,
} from 'lucide-react';
import { AdminAccessProvider } from '../../app/AdminAccessContext.jsx';
import { adminApi, token } from '../../api/adminApi.js';
import './AdminLayout.css';

const primaryItems = [
  ['/dashboard', '概览', LayoutDashboard, 'DASHBOARD_VIEW'],
  ['/analytics', '运营分析', ChartNoAxesCombined, 'ANALYTICS_VIEW'],
  ['/users', '用户管理', Users, 'USER_VIEW'],
  ['/curated-chat', '严选直聊', MessageSquarePlus, 'CURATED_CHAT_VIEW'],
  ['/announcements', '通知管理', BellRing, 'ANNOUNCEMENT_VIEW'],
  ['/certifications', '认证审核', ShieldCheck, 'CERTIFICATION_VIEW'],
  ['/inquiries', '询问管理', MessagesSquare, 'INQUIRY_VIEW'],
  ['/user-risk', '用户风险', ShieldAlert, 'RISK_WATCH_VIEW'],
  ['/communication-evaluations', '交流评价', BadgeCheck, 'ANSWER_QUALITY_VIEW'],
  ['/withdrawals', '普通提现', WalletCards, 'WITHDRAWAL_VIEW'],
  ['/permanent-ban-payouts', '封禁余额处理', BadgeDollarSign, 'PERMANENT_BAN_PAYOUT_VIEW'],
  ['/finance-reconciliation', '资金对账', Scale, 'FINANCE_RECONCILIATION_VIEW'],
  ['/customer-service', '在线客服', Headset, 'CUSTOMER_SERVICE_VIEW'],
];

const secondaryGroups = [
  {
    id: 'business-settings',
    label: '业务配置',
    icon: Settings2,
    items: [
      [
        '/app-settings',
        'App全局设置',
        Settings2,
        ['PLATFORM_FEE_VIEW', 'INQUIRY_CAPACITY_VIEW', 'APP_GLOBAL_SETTING_VIEW'],
      ],
      ['/banners', '首页轮播', Images, 'BANNER_VIEW'],
      ['/experience-categories', '经历分类', ListTree, 'EXPERIENCE_CATEGORY_VIEW'],
    ],
  },
  {
    id: 'other-business',
    label: '其他业务',
    icon: Ellipsis,
    items: [
      ['/feedback', '投诉反馈', MessageSquareWarning, 'FEEDBACK_VIEW'],
      ['/cooperations', '商务合作', Handshake, 'COOPERATION_VIEW'],
    ],
  },
  {
    id: 'system-management',
    label: '系统管理',
    icon: ShieldCog,
    items: [
      ['/app-versions', 'App版本管理', RefreshCw, 'APP_VERSION_VIEW'],
      ['/app-test-account', '测试账号', Smartphone, 'APP_TEST_ACCOUNT_VIEW'],
      ['/admin-users', '后台账号', UserRoundCog, 'ADMIN_USER_VIEW'],
      ['/admin-roles', '角色管理', ShieldCog, 'ROLE_VIEW'],
      ['/admin-permissions', '权限管理', KeyRound, 'PERMISSION_VIEW'],
      ['/audit', '操作记录', ScrollText, 'AUDIT_LOG_VIEW'],
      ['/security-events', '安全事件', Siren, 'SECURITY_EVENT_VIEW'],
    ],
  },
];

export default function AdminLayout({ adminData, onLoggedOut, customerServiceUnread = 0 }) {
  const navigate = useNavigate();
  const location = useLocation();
  const [admin, setAdmin] = useState(adminData || null);
  const [expandedGroups, setExpandedGroups] = useState(() => {
    const activeGroup = secondaryGroups.find((group) =>
      group.items.some(([to]) => location.pathname.startsWith(to)),
    );
    return activeGroup ? [activeGroup.id] : [];
  });

  useEffect(() => {
    if (adminData) setAdmin(adminData);
  }, [adminData]);

  const permissionSet = useMemo(() => new Set(admin?.permissions || []), [admin]);
  const can = (code) => {
    if (permissionSet.has('*')) return true;
    if (Array.isArray(code)) return code.some((item) => permissionSet.has(item));
    return permissionSet.has(code);
  };
  const visiblePrimary = primaryItems.filter((item) => can(item[3]));
  const visibleGroups = secondaryGroups
    .map((group) => ({ ...group, items: group.items.filter((item) => can(item[3])) }))
    .filter((group) => group.items.length > 0);

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
    <AdminAccessProvider admin={admin}>
      <div className="admin-shell">
        <aside>
          <div className="admin-brand">
            <img src="/brand/app-icon.png" alt="事先问" />
            <div>
              <b>事先问</b>
              <span>管理后台</span>
            </div>
          </div>
          <nav>
            {visiblePrimary.map(([to, label, Icon]) => (
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
            {visibleGroups.length > 0 && (
              <div className="nav-divider">
                <span>更多管理</span>
              </div>
            )}
            {visibleGroups.map((group) => {
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
            <em>{admin?.displayName || '管理员'}</em>
          </header>
          <div className="admin-content">
            <Outlet />
          </div>
        </main>
      </div>
    </AdminAccessProvider>
  );
}
