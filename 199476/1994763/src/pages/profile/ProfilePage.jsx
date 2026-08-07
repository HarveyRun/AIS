import {
  BadgeCheck,
  Bell,
  BriefcaseBusiness,
  ChevronRight,
  LogOut,
  MessageSquareWarning,
  ShieldCheck,
  SlidersHorizontal,
  WalletCards,
} from 'lucide-react';
import './ProfilePages.css';

export default function MyProfilePage({ go, hourlyFee, logout }) {
  return (
    <div className="profile-page">
      <section className="profile-hero">
        <header>
          <h1>我的</h1>
          <button className="round" type="button" aria-label="通知消息">
            <Bell size={20} />
          </button>
        </header>

        <div className="profile-overview">
          <div className="my-card">
            <div className="profile-user-row">
              <div className="avatar" style={{ background: '#d28b64' }}>
                安
                <i>
                  <BadgeCheck size={15} />
                </i>
              </div>
              <div className="profile-user-text">
                <h2>安然</h2>
                <p>UID 1000286</p>
              </div>
            </div>
            <span className="verified-label">
              <BadgeCheck />
              已实名认证
            </span>
          </div>

          <section className="profile-primary-section">
            <div className="profile-dashboard">
              <button type="button" onClick={() => go('settings')}>
                <i className="settings-icon">
                  <SlidersHorizontal />
                </i>
                <b>统一设置</b>
                <span>¥{hourlyFee}/小时</span>
              </button>

              <button type="button" onClick={() => go('wallet')}>
                <i className="wallet-icon">
                  <WalletCards />
                </i>
                <b>账户余额</b>
                <span>¥2,680.00</span>
              </button>

              <button type="button" onClick={() => go('certs')}>
                <i className="profile-icon">
                  <BriefcaseBusiness />
                </i>
                <b>我的档案</b>
                <span>4项已认证</span>
              </button>
            </div>
          </section>
        </div>
      </section>

      <section className="profile-platform-section">
        <h2>更多</h2>
        <div className="menu">
          <button type="button" onClick={() => go('rules')}>
            <span>
              <ShieldCheck />
              平台规则
            </span>
            <ChevronRight />
          </button>
          <button type="button" onClick={() => go('feedback')}>
            <span>
              <MessageSquareWarning />
              投诉与反馈
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
