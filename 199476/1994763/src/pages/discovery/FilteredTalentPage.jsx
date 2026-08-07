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
import { people } from '../../data/mockData.js';
import TalentCard from '../../components/talent/TalentCard.jsx';
import './DiscoveryPages.css';

export default function FilteredTalentPage({ go, problem, setTalent }) {
  return (
    <Page title="可以帮上忙的人" back={() => go('knowledge')}>
      <section className="result-title">
        <span>你想解决</span>
        <h2>{problem}</h2>
        <p>这些人的工作经历可能帮得上忙</p>
      </section>
      <section className="talent-section results">
        <div className="talent-list">
          {people.slice(0, 3).map((p) => (
            <TalentCard
              key={p.uid}
              p={p}
              onClick={() => {
                setTalent(p);
                go('talent');
              }}
            />
          ))}
        </div>
      </section>
    </Page>
  );
}
