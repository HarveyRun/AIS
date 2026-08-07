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

export default function WalletPage({
  go,
  notify,
  balance,
  setBalance,
  ledger: records,
  setLedger: setRecords,
  withdrawals,
  setWithdrawals,
}) {
  const [mode, setMode] = useState('ledger');
  const [amount, setAmount] = useState('');
  const submitMoney = () => {
    const value = Number(amount);
    if (!Number.isFinite(value) || value <= 0) return;
    if (mode === 'withdraw' && value > balance) {
      notify('余额不足');
      return;
    }
    if (mode === 'recharge') {
      setBalance(balance + value);
      setRecords((current) => [['收入', '余额充值', `+¥${value.toFixed(2)}`, '刚刚'], ...current]);
      notify('充值成功');
    } else {
      setBalance(balance - value);
      setWithdrawals((current) => [[`¥${value.toFixed(2)}`, '处理中', '刚刚'], ...current]);
      notify('提现申请已经提交');
    }
    setAmount('');
    setMode(mode === 'recharge' ? 'ledger' : 'history');
  };
  return (
    <Page title="账户余额" back={() => go('profile', 'profile')}>
      <section className="balance-card">
        <span>可用余额</span>
        <strong>¥{balance.toFixed(2)}</strong>
        <p>累计已提现 ¥3,200 · 剩余免费提现额度 ¥6,800</p>
      </section>
      <div className="wallet-tabs four">
        <button className={mode === 'ledger' ? 'active' : ''} onClick={() => setMode('ledger')}>
          收支明细
        </button>
        <button className={mode === 'history' ? 'active' : ''} onClick={() => setMode('history')}>
          提现记录
        </button>
        <button className={mode === 'recharge' ? 'active' : ''} onClick={() => setMode('recharge')}>
          充值
        </button>
        <button className={mode === 'withdraw' ? 'active' : ''} onClick={() => setMode('withdraw')}>
          提现
        </button>
      </div>
      {mode === 'ledger' && (
        <section className="ledger-list">
          {records.map((r, i) => (
            <article key={i}>
              <i className={r[0] === '收入' ? 'income' : 'expense'}>
                {r[0] === '收入' ? '收' : '支'}
              </i>
              <div>
                <b>{r[1]}</b>
                <small>{r[3]}</small>
              </div>
              <strong className={r[0] === '收入' ? 'income' : ''}>{r[2]}</strong>
            </article>
          ))}
        </section>
      )}
      {mode === 'history' && (
        <section className="withdraw-list">
          {withdrawals.map((r, i) => (
            <article key={i}>
              <div>
                <b>提现至招商银行（2816）</b>
                <small>{r[2]}</small>
              </div>
              <strong>{r[0]}</strong>
              <span className={r[1] === '处理中' ? 'pending' : ''}>{r[1]}</span>
            </article>
          ))}
        </section>
      )}
      {['recharge', 'withdraw'].includes(mode) && (
        <>
          <section className="cash-form">
            <label>{mode === 'recharge' ? '充值金额' : '提现金额'}</label>
            <div className="cash-input">
              <b>¥</b>
              <input
                inputMode="decimal"
                value={amount}
                onChange={(event) => {
                  const next = event.target.value;
                  if (/^\d*(\.\d{0,2})?$/.test(next)) setAmount(next);
                }}
                placeholder="0.00"
              />
            </div>
            {mode === 'recharge' ? (
              <p>充值到账后，平台内所有消费将直接从余额扣除。</p>
            ) : (
              <>
                <div className="bank-row">
                  <Landmark />
                  <span>
                    <b>到账银行卡</b>
                    <small>招商银行（尾号 2816）</small>
                  </span>
                  <ChevronRight />
                </div>
                <p>累计提现10,000元以内佣金0%；超过额度后，每笔收取20%。</p>
              </>
            )}
          </section>
          <button disabled={!Number(amount)} className="sticky-primary" onClick={submitMoney}>
            {mode === 'recharge' ? '确认充值' : '确认提现'}
          </button>
        </>
      )}
    </Page>
  );
}
