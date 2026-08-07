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
import './RulesPage.css';

export default function RulesPage({ go }) {
  return (
    <Page title="平台规则" back={() => go('profile', 'profile')}>
      <section className="rules">
        <Rule icon={<Star />} title="劳务费分配">
          <p>用户支付的服务费，在参与服务的人员之间均分。</p>
          <div className="score-list">
            <span>
              5分 <b>获得100%</b>
            </span>
            <span>
              4分 <b>获得100%</b>
            </span>
            <span>
              3分 <b>获得80%</b>
            </span>
            <span>
              2分 <b>获得60%</b>
            </span>
            <span>
              1分 <b>获得40%</b>
            </span>
          </div>
          <small>低于4分时，每少1分扣减20%劳务费。</small>
        </Rule>
        <Rule icon={<WalletCards />} title="平台佣金">
          <p>累计提现不超过10,000元，每笔佣金0%；超过免费提现额度后，每笔佣金20%。</p>
        </Rule>
        <Rule icon={<ShieldCheck />} title="投诉与违规">
          <div className="violation-grid">
            {[
              [1, 3],
              [2, 5],
              [3, 10],
              [4, 50],
              [5, 100],
            ].map(([l, n]) => (
              <div key={l}>
                <b>{l}级</b>
                <span>{n}次</span>
              </div>
            ))}
          </div>
          <small>对应等级的有效违规次数扣完后，账号永久封禁。</small>
        </Rule>
      </section>
    </Page>
  );
}

export function Rule({ icon, title, children }) {
  return (
    <article className="rule-card">
      <header>
        {icon}
        <h2>{title}</h2>
      </header>
      {children}
    </article>
  );
}
