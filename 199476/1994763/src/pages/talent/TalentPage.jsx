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
import { Career } from '../../components/talent/TalentCard.jsx';
import './TalentPage.css';

export default function TalentPage({ go, talent: p, matter, helpers, setHelpers }) {
  const [picker, setPicker] = useState(false);
  const [target, setTarget] = useState('');
  const [success, setSuccess] = useState(false);
  const joined = helpers.some((x) => x.uid === p.uid);
  const submit = () => {
    if (!target) return;
    if (target === matter.title && !joined) setHelpers([...helpers, p]);
    setPicker(false);
    setSuccess(true);
    setTimeout(() => setSuccess(false), 3500);
  };
  return (
    <Page title="个人档案" back={() => window.history.back()}>
      <section className="profile-hero">
        <div className="avatar large" style={{ background: p.color }}>
          {p.name.slice(-1)}
          <i>
            <BadgeCheck size={17} />
          </i>
        </div>
        <h1>{p.name}</h1>
        <span>UID {p.uid} · 信息已经核实</span>
      </section>
      <section className="fact-box">
        <h2>
          <ShieldCheck size={19} /> 已经核实的经历
        </h2>
        <Career label="主职" name={p.main} years={p.mainYears} />
        <Career label="副职" name={p.side} years={p.sideYears} />
        <Career label="个人事业" name={p.venture} years={p.ventureYears} />
      </section>
      <section className="story-box">
        <h2>人生主要经历</h2>
        <div className="story-tags big">
          {p.tags.map((x) => (
            <span key={x}>{x}</span>
          ))}
        </div>
        <p>这些经历都有证明材料，可以帮你更好地了解这个人。</p>
      </section>
      <section className="talent-schedule">
        <Clock3 />
        <div>
          <span>服务时间</span>
          <b>{p.serviceTime}</b>
        </div>
      </section>
      {picker && (
        <>
          <button className="sheet-mask" onClick={() => setPicker(false)} />
          <div className="matter-picker">
            <h2>选择一件事项</h2>
            {[matter.title].map((x) => (
              <button className={target === x ? 'active' : ''} onClick={() => setTarget(x)} key={x}>
                <i>{target === x && <Check />}</i>
                <span>
                  <b>{x}</b>
                  <small>{x === matter.title ? matter.desc : '改善路由器位置与家庭网络覆盖'}</small>
                </span>
              </button>
            ))}
            <button disabled={!target} className="picker-submit" onClick={submit}>
              确认添加
            </button>
          </div>
        </>
      )}
      {success && (
        <div className="floating-success">
          <Check />
          <span>已添加到“{target}”</span>
          <button onClick={() => go('matter', 'requests')}>
            去查看 <ArrowRight />
          </button>
        </div>
      )}
      <button className="sticky-primary" onClick={() => setPicker(true)}>
        添加到我的事项 <ArrowRight size={18} />
      </button>
    </Page>
  );
}
