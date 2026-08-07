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
import CertItem from '../../components/certification/CertItem.jsx';
import './CertificationPages.css';

export default function CertificationApplyPage({ go, type, setType }) {
  const [name, setName] = useState(type === '实名认证' ? '本人身份认证' : '');
  const [desc, setDesc] = useState('');
  return (
    <Page title="说说你的经历" back={() => go('certs')}>
      <section className="cert-form">
        <span>你要证明</span>
        <h1>{type || '其它经历认证'}</h1>
        <label>这段经历叫什么</label>
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="例如：软件工程师工作经历"
        />
        <label>简单说说</label>
        <textarea
          value={desc}
          onChange={(e) => setDesc(e.target.value)}
          placeholder="说说你做过什么、做了多久"
        />
      </section>
      <button disabled={!name.trim()} className="sticky-primary" onClick={() => go('certUpload')}>
        下一步
      </button>
    </Page>
  );
}
