import {
  ChevronRight,
  CircleHelp,
  Handshake,
  Headphones,
  LogOut,
  MessageSquareWarning,
  Settings,
  UserRoundPlus,
  WalletCards,
} from 'lucide-react';
import UserAvatar from '../../components/profile/UserAvatar.jsx';
import './ProfilePages.css';

export default function MyProfilePage({
  go,
  certifications,
  logout,
  userProfile,
  customerServiceUnreadCount = 0,
}) {
  const hasJoined = certifications
    .filter((item) => item.required)
    .every((item) => item.status === '已认证');
  const identityVerified = certifications.some(
    (item) => item.type === '实名认证' && item.status === '已认证',
  );
  return (
    <div className="profile-page">
      <section className="profile-hero">
        <header>
          <h1>我的</h1>
        </header>

        <div className="profile-overview">
          <div className="my-card">
            <div className="profile-user-row">
              <UserAvatar
                src={userProfile.avatar}
                uid={userProfile.uid}
                name={userProfile.name}
                verified={identityVerified}
              />
              <div className="profile-user-text">
                <h2>{userProfile.name?.trim() || `UID ${userProfile.uid}`}</h2>
                {userProfile.name?.trim() && <p>UID {userProfile.uid}</p>}
              </div>
            </div>
            <button
              className="profile-settings-button"
              type="button"
              onClick={() => go('accountSettings')}
              aria-label="账号设置"
            >
              <Settings />
            </button>
          </div>

          <section className="profile-primary-section">
            <div className="profile-dashboard">
              <button type="button" onClick={() => go('certs')}>
                <i className="profile-icon">
                  <UserRoundPlus />
                </i>
                <b>{hasJoined ? '答主信息' : '成为答主'}</b>
                <ChevronRight className="profile-shortcut-chevron" />
              </button>

              <button type="button" onClick={() => go('wallet')}>
                <i className="wallet-icon">
                  <WalletCards />
                </i>
                <b>账户余额</b>
                <ChevronRight className="profile-shortcut-chevron" />
              </button>
            </div>
          </section>
        </div>
      </section>

      <section className="profile-platform-section">
        <h2>更多</h2>
        <div className="menu">
          <button type="button" onClick={() => go('faq')}>
            <span>
              <CircleHelp />
              常见问题
            </span>
            <ChevronRight />
          </button>
          <button type="button" onClick={() => go('customerService')}>
            <span>
              <Headphones />
              在线客服
            </span>
            {customerServiceUnreadCount > 0 && (
              <em className="profile-menu-count">
                {customerServiceUnreadCount > 99 ? '99+' : customerServiceUnreadCount}
              </em>
            )}
            <ChevronRight />
          </button>
          <button type="button" onClick={() => go('feedback')}>
            <span>
              <MessageSquareWarning />
              投诉与反馈
            </span>
            <ChevronRight />
          </button>
          <button type="button" onClick={() => go('business')}>
            <span>
              <Handshake />
              商务合作
            </span>
            <ChevronRight />
          </button>
          <button type="button" onClick={logout}>
            <span>
              <LogOut />
              退出登录
            </span>
            <ChevronRight />
          </button>
        </div>
      </section>

      <p className="footer-copy">每个人，都有能帮上别人的地方。</p>
    </div>
  );
}
