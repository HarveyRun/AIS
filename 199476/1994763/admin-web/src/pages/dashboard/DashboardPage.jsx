import { useEffect, useState } from 'react';
import { adminApi } from '../../api/adminApi.js';
import {
  Users,
  ShieldCheck,
  MessagesSquare,
  WalletCards,
  MessageSquareWarning,
  Landmark,
  Footprints,
  PhoneCall,
  TriangleAlert,
} from 'lucide-react';
import '../shared/Page.css';
import { message } from '../../components/feedback/message.js';
const cards = [
  ['users', '有效用户', Users],
  ['answerers', '经历发布者', ShieldCheck],
  ['approvedExperiences', '已通过经历', Footprints],
  ['pendingExperiences', '待审经历', Footprints],
  ['activeInquiries', '进行中询问', MessagesSquare],
  ['pendingWithdrawals', '待处理提现', WalletCards],
  ['openFeedback', '待处理反馈', MessageSquareWarning],
];
export default function DashboardPage() {
  const [data, setData] = useState(null);
  useEffect(() => {
    adminApi
      .dashboard()
      .then(setData)
      .catch((e) => message.error(e.message));
  }, []);
  return (
    <>
      <div className="page-title">
        <div>
          <h1>数据概览</h1>
          <p>平台当前需要关注的业务数据</p>
        </div>
      </div>
      <section className="stat-grid">
        {cards.map(([key, label, Icon]) => (
          <article key={key}>
            <i>
              <Icon />
            </i>
            <div>
              <span>{label}</span>
              <strong>{data?.[key] ?? '—'}</strong>
            </div>
          </article>
        ))}
      </section>
      <section className="fund-overview">
        <h2>
          <Landmark />
          平台资金概况
        </h2>
        <div>
          <span>
            用户可用余额<strong>¥{data?.totalBalance ?? '0.00'}</strong>
          </span>
          <span>
            冻结金额<strong>¥{data?.totalFrozen ?? '0.00'}</strong>
          </span>
        </div>
      </section>
      <section className="fund-overview">
        <h2>
          <PhoneCall />
          运行与业务监控
        </h2>
        <div>
          <span>今日语音接通<strong>{data?.voiceCallsToday ?? '—'}</strong></span>
          <span>今日连接异常<strong>{data?.voiceConnectionFailuresToday ?? '—'}</strong></span>
          <span>呼叫中语音<strong>{data?.pendingAudioRequests ?? '—'}</strong></span>
          <span>24小时内到期询问<strong>{data?.inquiriesExpiringWithin24Hours ?? '—'}</strong></span>
        </div>
      </section>
      <section className="fund-overview">
        <h2>
          <TriangleAlert />
          待关注异常
        </h2>
        <div>
          <span>资料整理失败<strong>{data?.mediaProcessingFailures ?? '—'}</strong></span>
          <span>投诉超过24小时<strong>{data?.feedbackOver24Hours ?? '—'}</strong></span>
          <span>风险提现待审核<strong>{data?.riskyWithdrawals ?? '—'}</strong></span>
        </div>
      </section>
    </>
  );
}
