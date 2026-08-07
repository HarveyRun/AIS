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

export default function RegisterPage({ go, onRegister }) {
  const [phone, setPhone] = useState('');
  const [code, setCode] = useState('');
  const [name, setName] = useState('');
  const [agreed, setAgreed] = useState(true);
  const [sent, setSent] = useState(false);

  return (
    <div className="auth-page register">
      <button type="button" className="auth-back" onClick={() => go('login')}>
        <ArrowLeft />
      </button>
      <div className="auth-brand compact">
        <div>
          <HeartHandshake />
        </div>
        <h1>创建光忆账号</h1>
        <p>从一个真实、可信的身份开始</p>
      </div>
      <section className="auth-card">
        <label>昵称</label>
        <div className="auth-input">
          <CircleUserRound />
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            maxLength="12"
            placeholder="请输入昵称"
          />
        </div>
        <label>手机号</label>
        <div className="auth-input">
          <Smartphone />
          <input
            value={phone}
            onChange={(event) => setPhone(digitsOnly(event.target.value))}
            maxLength="11"
            inputMode="numeric"
            placeholder="请输入手机号"
          />
        </div>
        <label>验证码</label>
        <div className="auth-input">
          <LockKeyhole />
          <input
            value={code}
            onChange={(event) => setCode(digitsOnly(event.target.value))}
            maxLength="6"
            inputMode="numeric"
            placeholder="请输入验证码"
          />
          <button type="button" disabled={phone.length !== 11} onClick={() => setSent(true)}>
            {sent ? '已发送' : '获取验证码'}
          </button>
        </div>
        <label className="agree">
          <input
            type="checkbox"
            checked={agreed}
            onChange={(event) => setAgreed(event.target.checked)}
          />
          我已阅读并同意用户协议与隐私政策
        </label>
        <button
          type="button"
          className="auth-submit"
          disabled={!name.trim() || phone.length !== 11 || code.length < 4 || !agreed}
          onClick={onRegister}
        >
          注册并进入
        </button>
      </section>
    </div>
  );
}
