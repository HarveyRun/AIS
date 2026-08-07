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
import './ProfilePages.css';

export default function SettingsPage({
  go,
  notify,
  hourlyFee,
  setHourlyFee,
  userSchedule,
  setUserSchedule,
}) {
  const [fee, setFee] = useState(String(hourlyFee));
  const [days, setDays] = useState(userSchedule.days);
  const [sat, setSat] = useState(userSchedule.sat);
  const [sun, setSun] = useState(userSchedule.sun);
  return (
    <Page title="个人设置" back={() => go('profile', 'profile')}>
      <div className="settings-lead">
        <h1>统一设置</h1>
        <p>这里的费用和时间，会用在你之后的所有事项中</p>
      </div>
      <section className="setting-panel">
        <div className="setting-title">
          <i>
            <Coins />
          </i>
          <div>
            <h2>我付的钱</h2>
            <p>你发起事项时，支付给每位参与人员</p>
          </div>
          <span>3个月可改一次</span>
        </div>
        <div className="money-input large">
          <span>¥</span>
          <input
            inputMode="decimal"
            value={fee}
            onChange={(event) => {
              const next = event.target.value;
              if (/^\d*(\.\d{0,2})?$/.test(next)) setFee(next);
            }}
          />
          <small>/人/小时</small>
        </div>
      </section>
      <section className="setting-panel schedule-panel">
        <div className="setting-title">
          <i>
            <Clock3 />
          </i>
          <div>
            <h2>我的服务时间</h2>
            <p>别人邀请你帮忙时，会看到这些时间</p>
          </div>
          <span>3个月可改一次</span>
        </div>
        <Schedule
          days={days}
          setDays={setDays}
          sat={sat}
          setSat={setSat}
          sun={sun}
          setSun={setSun}
        />
      </section>
      <button
        disabled={!Number(fee) || (!days.length && !sat && !sun)}
        className="sticky-primary"
        onClick={() => {
          setHourlyFee(Number(fee));
          setUserSchedule({ days, sat, sun });
          notify('设置已经保存');
        }}
      >
        保存设置
      </button>
    </Page>
  );
}
