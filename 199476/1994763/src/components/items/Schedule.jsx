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

export default function Schedule({ days, setDays, sat, setSat, sun, setSun }) {
  const all = ['周一', '周二', '周三', '周四', '周五'];
  const toggle = (d) => setDays(days.includes(d) ? days.filter((x) => x !== d) : [...days, d]);
  return (
    <section className="apply-section">
      <label>
        大家方便沟通的时间 <small>日期可以多选，时间固定</small>
      </label>
      <div className="schedule-card">
        <div>
          <b>周一至周五</b>
          <span>19:00—23:00</span>
        </div>
        <div className="day-row">
          {all.map((d) => (
            <button className={days.includes(d) ? 'active' : ''} onClick={() => toggle(d)} key={d}>
              {d.slice(1)}
            </button>
          ))}
        </div>
      </div>
      <button className={`weekend ${sat ? 'active' : ''}`} onClick={() => setSat(!sat)}>
        <span>
          <b>周六</b>
          <small>10:00—23:00</small>
        </span>
        <i>{sat && <Check />}</i>
      </button>
      <button className={`weekend ${sun ? 'active' : ''}`} onClick={() => setSun(!sun)}>
        <span>
          <b>周日</b>
          <small>10:00—23:00</small>
        </span>
        <i>{sun && <Check />}</i>
      </button>
    </section>
  );
}
