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
import { knowledge } from '../../data/mockData.js';
import './DiscoveryPages.css';

export default function KnowledgePage({ go, category, setCategory, problem, setProblem }) {
  return (
    <Page title="按问题筛选" back={() => go('home')}>
      <section className="knowledge-intro">
        <LightIcon />
        <div>
          <h2>这件事，可能需要谁？</h2>
          <p>先看看通常需要哪些岗位，再由你自己选人。</p>
        </div>
      </section>
      <div className="category-tabs">
        {Object.keys(knowledge).map((x) => (
          <button className={category === x ? 'active' : ''} onClick={() => setCategory(x)} key={x}>
            {x}
          </button>
        ))}
      </div>
      <section className="tree-list">
        {Object.entries(knowledge[category]).map(([group, jobs]) => (
          <div className="tree-group" key={group}>
            <h3>
              {group}
              <span>{jobs.length}类问题</span>
            </h3>
            <div>
              {jobs.map((job) => (
                <button
                  className={problem === job ? 'selected' : ''}
                  key={job}
                  onClick={() => setProblem(job)}
                >
                  {job}
                  {problem === job ? <Check size={15} /> : <span>○</span>}
                </button>
              ))}
            </div>
          </div>
        ))}
      </section>
      <button disabled={!problem} className="sticky-primary" onClick={() => go('filtered')}>
        查看能帮助“{problem || '该问题'}”的人 <ArrowRight size={18} />
      </button>
    </Page>
  );
}

export function LightIcon() {
  return (
    <div className="light-icon">
      <Zap size={23} />
    </div>
  );
}
