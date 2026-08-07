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

export default function ParticipantList({ go, onOpenChat }) {
  const [items, setItems] = useState([
    {
      id: 1,
      title: '电脑运行缓慢排查',
      desc: '电脑启动慢、软件频繁卡顿，希望梳理原因。',
      role: '程序开发',
      status: '待接受',
      rate: 60,
      applicant: '林',
    },
    {
      id: 2,
      title: '个人网站制作建议',
      desc: '准备制作个人作品网站，需要产品结构建议。',
      role: '产品设计',
      status: '服务中',
      rate: 80,
      applicant: '顾',
    },
  ]);
  const [detail, setDetail] = useState(null);
  const update = (id, status) => setItems(items.map((x) => (x.id === id ? { ...x, status } : x)));
  return (
    <section className="participant-list">
      {items.map((x) => (
        <article key={x.id}>
          <header>
            <button className="applicant-avatar" onClick={() => setDetail(x)}>
              {x.applicant}
            </button>
            <div>
              <h3>{x.title}</h3>
              <p>以“{x.role}”身份参与</p>
            </div>
            <span className={`p-status s${x.status}`}>{x.status}</span>
          </header>
          <div className="earning">
            <Coins /> 每小时收益 <b>¥{x.rate}</b>
          </div>
          <footer>
            {x.status === '待接受' && (
              <>
                <button onClick={() => update(x.id, '已拒绝')}>拒绝</button>
                <button className="main" onClick={() => update(x.id, '已接受')}>
                  接受申请
                </button>
              </>
            )}
            {x.status === '已接受' && (
              <button className="main" onClick={() => update(x.id, '服务中')}>
                确认开始服务
              </button>
            )}
            {x.status === '服务中' && (
              <button className="main" onClick={onOpenChat || (() => go('messages', 'messages'))}>
                进入群聊
              </button>
            )}
            {x.status === '已拒绝' && <span>你已拒绝本次申请</span>}
          </footer>
        </article>
      ))}
      {detail && (
        <>
          <button className="sheet-mask" onClick={() => setDetail(null)} />
          <div className="applicant-detail">
            <button onClick={() => setDetail(null)}>
              <X />
            </button>
            <div className="applicant-avatar large">{detail.applicant}</div>
            <span>申请人创建的协作事项</span>
            <h2>{detail.title}</h2>
            <p>{detail.desc}</p>
          </div>
        </>
      )}
    </section>
  );
}
