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

export default function MatterPage({
  go,
  matter,
  helpers,
  setHelpers,
  notify,
  hourlyFee,
  balance,
  setBalance,
  setSelectedGroup,
  setGroups,
  setLedger,
}) {
  const [pay, setPay] = useState(false);
  const remove = (id) => setHelpers(helpers.filter((x) => x.uid !== id));
  const serviceTime = (person) => person.serviceTime;
  const total = helpers.length * hourlyFee;
  const establish = () => {
    const times = [...new Set(helpers.map(serviceTime))];
    if (times.length > 1) notify('服务时间不一致，暂时不能开始群聊');
    else setPay(true);
  };
  return (
    <Page title="事项详情" back={() => go('requests', 'requests')}>
      <section className="helper-list single">
        <div className="section-head simple">
          <h2>选择参与人员</h2>
          <strong className="estimate">预计支付 ¥{total}/小时</strong>
        </div>
        {helpers.map((p) => (
          <div className="helper-time-row" key={p.uid}>
            <div className="avatar" style={{ background: p.color }}>
              {p.name.slice(-1)}
            </div>
            <span>
              <b>
                {p.name} · {p.main}
              </b>
              <strong>
                <Clock3 /> {serviceTime(p)}
              </strong>
            </span>
            <button onClick={() => remove(p.uid)}>
              <X />
            </button>
          </div>
        ))}
        <button className="add-helper" onClick={() => go('filter')}>
          ＋ 继续添加人员
        </button>
      </section>
      {pay && (
        <>
          <button className="sheet-mask" onClick={() => setPay(false)} />
          <div className="balance-pay">
            <h2>余额支付</h2>
            <div>
              <span>本次预付</span>
              <b>¥{total}.00</b>
            </div>
            <p>账户余额：¥{balance.toFixed(2)}</p>
            <button
              onClick={() => {
                if (balance < total) {
                  notify('余额不足，请先充值');
                  return;
                }
                setPay(false);
                setBalance(balance - total);
                const newGroup = {
                  id: `matter-${Date.now()}`,
                  title: `${matter.title}群`,
                  desc: helpers.map((person) => person.main).join('、'),
                  members: helpers.length + 1,
                  unread: 0,
                  status: 'active',
                  statusText: '服务中',
                };
                setSelectedGroup(newGroup);
                setGroups((current) => [newGroup, ...current]);
                setLedger((current) => [
                  ['支出', `${matter.title}预付`, `-¥${total.toFixed(2)}`, '刚刚'],
                  ...current,
                ]);
                notify('余额支付成功');
                go('chat', 'messages');
              }}
            >
              确认支付并进入群聊
            </button>
            <small>请使用账户余额支付</small>
          </div>
        </>
      )}
      <button disabled={!helpers.length} className="sticky-primary" onClick={establish}>
        开始群聊
      </button>
    </Page>
  );
}
