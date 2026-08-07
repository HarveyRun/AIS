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
import Page from '../../components/layout/Page.jsx';
import './NoticesPage.css';

export default function NoticesPage({ go }) {
  const [read, setRead] = useState([]);
  const notices = [
    ['事情申请', '有人邀请你以“程序开发”身份加入事情', '刚刚'],
    ['时间提醒', '旧房装修协助中有1人的时间和其他人不同', '20分钟前'],
    ['认证消息', '你提交的人生经历材料正在核对中', '昨天'],
  ];
  return (
    <Page title="通知" back={() => go('home')}>
      <section className="notice-list">
        {notices.map((n, i) => (
          <button
            className={read.includes(i) ? 'read' : ''}
            key={n[0]}
            onClick={() => setRead([...read, i])}
          >
            <i>
              <Bell />
            </i>
            <div>
              <h3>{n[0]}</h3>
              <p>{n[1]}</p>
              <span>{n[2]}</span>
            </div>
            {!read.includes(i) && <b />}
          </button>
        ))}
      </section>
    </Page>
  );
}
