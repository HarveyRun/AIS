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
import './SupportPages.css';

export default function FeedbackPage({ go, notify }) {
  const [type, setType] = useState('feedback');
  const [target, setTarget] = useState('');
  const [text, setText] = useState('');
  const submit = () => {
    notify(type === 'complaint' ? '投诉已提交' : '反馈已提交');
    setText('');
  };

  return (
    <Page title="投诉与反馈" back={() => go('profile', 'profile')}>
      <div className="feedback-tabs">
        <button
          type="button"
          className={type === 'feedback' ? 'active' : ''}
          onClick={() => setType('feedback')}
        >
          产品反馈
        </button>
        <button
          type="button"
          className={type === 'complaint' ? 'active' : ''}
          onClick={() => setType('complaint')}
        >
          投诉
        </button>
      </div>
      <section className="feedback-form">
        {type === 'complaint' && (
          <>
            <label>投诉对象</label>
            <select value={target} onChange={(event) => setTarget(event.target.value)}>
              <option value="">请选择群成员或事项</option>
              <option>旧房装修协作群 · 装修监理</option>
              <option>家庭网络改善 · 程序开发</option>
              <option>某条群聊消息</option>
            </select>
            <label>投诉类型</label>
            <select>
              <option>服务态度问题</option>
              <option>虚假能力信息</option>
              <option>违规收费</option>
              <option>骚扰或不当言论</option>
              <option>其它</option>
            </select>
          </>
        )}
        <label>{type === 'complaint' ? '详细说明' : '反馈内容'}</label>
        <textarea
          value={text}
          onChange={(event) => setText(event.target.value)}
          placeholder={
            type === 'complaint' ? '请说明发生的时间、经过和诉求' : '说说你希望光忆改进什么'
          }
        />
      </section>
      <button
        type="button"
        disabled={!text.trim() || (type === 'complaint' && !target)}
        className="sticky-primary"
        onClick={submit}
      >
        {type === 'complaint' ? '提交投诉' : '提交反馈'}
      </button>
    </Page>
  );
}
