import { useState } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  BadgeCheck,
  Banknote,
  Bell,
  BriefcaseBusiness,
  Building2,
  CalendarDays,
  Camera,
  Check,
  CheckCircle2,
  ChevronRight,
  CircleUserRound,
  Clock3,
  Coins,
  FileCheck2,
  HeartHandshake,
  Info,
  Landmark,
  LockKeyhole,
  MessageCircleMore,
  MessageSquareWarning,
  MoreHorizontal,
  PlusCircle,
  Search,
  Send,
  ShieldCheck,
  SlidersHorizontal,
  Smartphone,
  Sparkles,
  Star,
  UsersRound,
  WalletCards,
  X,
  Zap,
} from 'lucide-react';
import './AuthPages.css';

const digitsOnly = (value) => value.replace(/\D/g, '');

export default function LoginPage({ go, onLogin }) {
  const [phone, setPhone] = useState('');
  const [code, setCode] = useState('');
  const [sent, setSent] = useState(false);

  return (
    <div className="auth-page">
      <div className="auth-brand">
        <div>
          <HeartHandshake />
        </div>
        <h1>光忆</h1>
        <p>让普通人的经验，继续帮助普通人</p>
      </div>
      <section className="auth-card">
        <h2>欢迎回来</h2>
        <p>使用手机号登录</p>
        <label>手机号</label>
        <div className="auth-input">
          <Smartphone />
          <input
            maxLength="11"
            inputMode="numeric"
            value={phone}
            onChange={(event) => setPhone(digitsOnly(event.target.value))}
            placeholder="请输入手机号"
          />
        </div>
        <label>验证码</label>
        <div className="auth-input">
          <LockKeyhole />
          <input
            maxLength="6"
            inputMode="numeric"
            value={code}
            onChange={(event) => setCode(digitsOnly(event.target.value))}
            placeholder="请输入验证码"
          />
          <button type="button" onClick={() => setSent(true)} disabled={phone.length !== 11}>
            {sent ? '已发送' : '获取验证码'}
          </button>
        </div>
        <button
          type="button"
          className="auth-submit"
          disabled={phone.length !== 11 || code.length < 4}
          onClick={onLogin}
        >
          登录
        </button>
        <div className="auth-switch">
          还没有账号？
          <button type="button" onClick={() => go('register')}>
            立即注册
          </button>
        </div>
        <small>登录即代表你同意《用户协议》和《隐私政策》</small>
      </section>
    </div>
  );
}
