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

export default function CreateMatterPage({ go, setMatter, setHelpers }) {
  const [title, setTitle] = useState('');
  const [desc, setDesc] = useState('');
  return (
    <Page title="创建我的事项" back={() => go('requests', 'requests')}>
      <section className="create-matter">
        <label>
          事项标题 <b>必填</b>
        </label>
        <input
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          maxLength="20"
          placeholder="例如：旧房装修协助"
        />
        <small>{title.length}/20</small>
        <label>
          简单描述 <b>必填</b>
        </label>
        <textarea
          value={desc}
          onChange={(e) => setDesc(e.target.value)}
          maxLength="120"
          placeholder="说清楚你想解决什么，以及目前的基本情况"
        />
        <small>{desc.length}/120</small>
      </section>
      <button
        disabled={!title.trim() || !desc.trim()}
        className="sticky-primary"
        onClick={() => {
          setMatter({ title, desc });
          setHelpers([]);
          go('matter', 'requests');
        }}
      >
        立即创建
      </button>
    </Page>
  );
}
