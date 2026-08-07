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
import { groupMessages } from '../../data/mockData.js';
import './MessagePages.css';

export default function MessagesPage({ go, groups, setGroups, setSelectedGroup }) {
  const openGroup = (group) => {
    const opened = { ...group, unread: 0 };
    setGroups(groups.map((item) => (item.id === group.id ? opened : item)));
    setSelectedGroup(opened);
    go('chat', 'messages');
  };

  return (
    <>
      <header className="topbar">
        <div>
          <h1>消息</h1>
        </div>
        <button className="round">
          <Bell size={20} />
        </button>
      </header>
      <section className="message-list">
        {groups.map((g) => (
          <button key={g.id} className={`group-row ${g.status}`} onClick={() => openGroup(g)}>
            <div className="group-avatar" style={{ background: g.color }}>
              <UsersRound />
              {g.members}
            </div>
            <div>
              <div className="group-title-row">
                <h3>{g.title}</h3>
                <em>{g.statusText}</em>
              </div>
              <p>{g.desc}</p>
            </div>
            <span className="msg-meta">
              <small>{g.time}</small>
              {g.unread > 0 && <i>{g.unread}</i>}
            </span>
          </button>
        ))}
      </section>
    </>
  );
}
