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

const RATING_HELPERS = [
  ['装修监理', '工程监理', '#698b9b'],
  ['室内设计', '室内设计师', '#c27b62'],
  ['水电维修', '物业电工', '#6f9584'],
];

export default function RatingPage({ go, notify }) {
  const [scores, setScores] = useState([0, 0, 0]);
  const rate = (index, value) =>
    setScores(scores.map((score, current) => (current === index ? value : score)));

  return (
    <Page title="服务评分" back={() => go('requests', 'requests')}>
      <section className="rating-head">
        <CheckCircle2 />
        <h1>事项已经结束</h1>
        <p>请分别评价每一位参与人员</p>
      </section>
      <section className="rating-list">
        {RATING_HELPERS.map((helper, index) => (
          <article key={helper[0]}>
            <div className="avatar" style={{ background: helper[2] }}>
              {helper[0][0]}
            </div>
            <div>
              <b>{helper[0]}</b>
              <small>本次服务 2小时</small>
            </div>
            <div className="stars">
              {[1, 2, 3, 4, 5].map((value) => (
                <button
                  type="button"
                  className={scores[index] >= value ? 'active' : ''}
                  onClick={() => rate(index, value)}
                  key={value}
                >
                  <Star />
                </button>
              ))}
            </div>
            <span>{scores[index] ? `${scores[index]}分` : '尚未评分'}</span>
          </article>
        ))}
      </section>
      <div className="rating-rule">
        <Info />
        <p>低于4分时，每少1分将按平台规则扣减对应人员20%的劳务费。</p>
      </div>
      <button
        type="button"
        disabled={scores.some((score) => !score)}
        className="sticky-primary"
        onClick={() => {
          notify('评分已提交');
          go('requests', 'requests');
        }}
      >
        提交评分
      </button>
    </Page>
  );
}
