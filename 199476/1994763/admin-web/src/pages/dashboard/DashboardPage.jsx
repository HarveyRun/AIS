import { useEffect, useState } from 'react';
import { adminApi } from '../../api/adminApi.js';
import {
  Users,
  ShieldCheck,
  MessagesSquare,
  WalletCards,
  MessageSquareWarning,
  Landmark,
} from 'lucide-react';
import '../shared/Page.css';
const cards = [
  ['users', '有效用户', Users],
  ['answerers', '答主', ShieldCheck],
  ['pendingCertifications', '待审认证', ShieldCheck],
  ['activeInquiries', '进行中询问', MessagesSquare],
  ['pendingWithdrawals', '待处理提现', WalletCards],
  ['openFeedback', '待处理反馈', MessageSquareWarning],
];
export default function DashboardPage() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => {
    adminApi
      .dashboard()
      .then(setData)
      .catch((e) => setError(e.message));
  }, []);
  return (
    <>
      <div className="page-title">
        <div>
          <h1>数据概览</h1>
          <p>平台当前需要关注的业务数据</p>
        </div>
      </div>
      {error && <div className="error-box">{error}</div>}
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
    </>
  );
}
