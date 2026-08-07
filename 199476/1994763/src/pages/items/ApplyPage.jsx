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

export default function ApplyPage({ go, selected, setSelected }) {
  const [weeks, setWeeks] = useState(4);
  const [days, setDays] = useState(['周三']);
  const [sat, setSat] = useState(true);
  const [sun, setSun] = useState(false);
  const changeCount = (job, d) =>
    setSelected(
      selected.map((x) =>
        x.job === job ? { ...x, count: Math.max(1, Math.min(3, x.count + d)) } : x,
      ),
    );
  const total = selected.reduce((n, x) => n + x.count, 0);
  return (
    <Page title="找人一起聊聊" back={() => go('knowledge')}>
      <section className="apply-section">
        <div className="section-head">
          <div>
            <span>可能帮得上忙的岗位</span>
            <h2>谁一起进入协作群？</h2>
          </div>
          <small>共{total}人</small>
        </div>
        {selected.map((x) => (
          <div className="job-row" key={x.job}>
            <div>
              <b>{x.job}</b>
              <span>同一个岗位最多选3人</span>
            </div>
            <div className="stepper">
              <button onClick={() => changeCount(x.job, -1)}>−</button>
              <strong>{x.count}</strong>
              <button onClick={() => changeCount(x.job, 1)}>＋</button>
            </div>
          </div>
        ))}
      </section>
      <section className="apply-section">
        <label>简单说明你的事情</label>
        <textarea placeholder="例如：准备装修一套旧房，希望几类岗位一起帮我梳理注意事项……" />
      </section>
      <Schedule days={days} setDays={setDays} sat={sat} setSat={setSat} sun={sun} setSun={setSun} />
      <section className="apply-section">
        <label>服务周期</label>
        <div className="weeks">
          <button onClick={() => setWeeks(Math.max(1, weeks - 1))}>−</button>
          <strong>
            {weeks}
            <small>周</small>
          </strong>
          <button onClick={() => setWeeks(weeks + 1)}>＋</button>
        </div>
      </section>
      <div className="group-rule">
        <UsersRound size={20} />
        <p>
          <b>选好以后，大家会进入同一个群</b>
          <br />
          请选择大家都有空的时间，这样才能在群里一起聊。
        </p>
      </div>
      <button
        disabled={!days.length && !sat && !sun}
        className="sticky-primary"
        onClick={() => go('messages', 'messages')}
      >
        邀请他们并创建群聊
      </button>
    </Page>
  );
}
