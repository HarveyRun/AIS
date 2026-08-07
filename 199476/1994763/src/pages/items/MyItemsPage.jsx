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
import Schedule from '../../components/items/Schedule.jsx';
import RequestCard from '../../components/items/RequestCard.jsx';
import ParticipantList from '../../components/items/ParticipantList.jsx';
import './ItemPages.css';

export default function MyItemsPage({ go, matter, groups, setSelectedGroup }) {
  const [type, setType] = useState('mine');
  const openGroup = (title) => {
    const group = groups.find((item) => item.title.includes(title)) || groups[0];
    setSelectedGroup(group);
    go('chat', 'messages');
  };
  return (
    <>
      <header className="topbar">
        <div>
          <h1>我的事项</h1>
        </div>
        <button className="round create" onClick={() => go('createMatter')}>
          <PlusCircle size={21} />
        </button>
      </header>
      <div className="request-tabs">
        <button className={type === 'mine' ? 'active' : ''} onClick={() => setType('mine')}>
          我创建的
        </button>
        <button className={type === 'joined' ? 'active' : ''} onClick={() => setType('joined')}>
          我参与的
        </button>
      </div>
      {type === 'mine' ? (
        <section className="request-cards">
          <RequestCard
            title={matter.title}
            people="已加入2位 · 继续添加"
            status="选择人员"
            color="#e98b5d"
            onClick={() => go('matter', 'requests')}
          />
          <RequestCard
            title="家庭网络改善"
            people="2类岗位 · 2人"
            status="群聊中"
            color="#7188a8"
            onClick={() => openGroup('家庭网络改善')}
          />
          <RequestCard
            title="劳动争议材料梳理"
            people="已结束 · 3人参与"
            status="待评分"
            color="#8c7aa0"
            onClick={() => go('rating', 'requests')}
          />
        </section>
      ) : (
        <ParticipantList go={go} onOpenChat={() => openGroup('家庭网络改善')} />
      )}
    </>
  );
}
