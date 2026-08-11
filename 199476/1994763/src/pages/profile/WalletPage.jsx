import { useState } from 'react';
import { ChevronRight, Landmark, X } from 'lucide-react';
import Page from '../../components/layout/Page.jsx';
import './ProfilePages.css';
import { api } from '../../api/http.js';

export default function WalletPage({
  go,
  notify,
  balance,
  setBalance,
  ledger: records,
  setLedger: setRecords,
  withdrawals,
  setWithdrawals,
  accountStats,
  setAccountStats,
  frozenAmount = 0,
}) {
  const [mode, setMode] = useState('ledger');
  const [amount, setAmount] = useState('');
  const [editingBank, setEditingBank] = useState(false);
  const [bankDraft, setBankDraft] = useState({
    bankName: '',
    cardNumber: '',
    holderName: '',
  });
  const [bankError, setBankError] = useState('');
  const boundBank = accountStats.bankCard || null;
  const numericAmount = Number(amount || 0);
  const remainingFree = Math.max(0, 10000 - Number(accountStats.totalWithdrawn || 0));
  const commissionBase = Math.max(0, numericAmount - remainingFree);
  const commission = Number((commissionBase * 0.2).toFixed(2));
  const arrivalAmount = Number(Math.max(0, numericAmount - commission).toFixed(2));
  const bankLabel = boundBank ? `${boundBank.bankName}（${boundBank.cardNumber.slice(-4)}）` : '';

  const openBankEditor = () => {
    setBankDraft(
      boundBank || {
        bankName: '',
        cardNumber: '',
        holderName: '',
      },
    );
    setBankError('');
    setEditingBank(true);
  };

  const saveBank = async () => {
    const cardNumber = bankDraft.cardNumber.replace(/\s/g, '');
    if (!bankDraft.holderName.trim()) {
      setBankError('请填写持卡人姓名');
      return;
    }
    if (!bankDraft.bankName) {
      setBankError('请选择开户银行');
      return;
    }
    if (!/^\d{12,19}$/.test(cardNumber)) {
      setBankError('请输入正确的银行卡号');
      return;
    }

    try {
      const saved = await api.bindBankCard({
        ...bankDraft,
        cardNumber,
        holderName: bankDraft.holderName.trim(),
      });
      setAccountStats((current) => ({
        ...current,
        bankCard: {
          holderName: saved.holderName,
          bankName: saved.bankName,
          cardNumber: saved.lastFour,
        },
      }));
      setEditingBank(false);
      notify(boundBank ? '银行卡已修改' : '银行卡已绑定');
    } catch (requestError) {
      setBankError(requestError.message);
    }
  };

  const submitMoney = async () => {
    const value = Number(amount);
    if (!Number.isFinite(value) || value <= 0) return;
    if (mode === 'withdraw' && value > balance) {
      notify('余额不足');
      return;
    }
    if (mode === 'withdraw' && !boundBank) {
      openBankEditor();
      return;
    }
    if (mode === 'recharge') {
      try {
        const order = await api.createRecharge(value);
        notify(order.paymentUrl ? '正在打开支付宝' : '充值订单已创建，请完成支付宝支付');
        if (order.paymentUrl) window.location.href = order.paymentUrl;
      } catch (requestError) {
        notify(requestError.message);
        return;
      }
    } else {
      try {
        const withdrawal = await api.withdraw(value);
        setBalance((current) => Number((current - value).toFixed(2)));
        setAccountStats((current) => ({
          ...current,
          totalWithdrawn: Number((Number(current.totalWithdrawn || 0) + value).toFixed(2)),
        }));
        setWithdrawals((current) => [
          [
            `¥${arrivalAmount.toFixed(2)}`,
            '处理中',
            '刚刚',
            `手续费 ¥${Number(withdrawal.fee).toFixed(2)}`,
            bankLabel,
          ],
          ...current,
        ]);
        setRecords((current) => [
          ['支出', '余额提现', `-¥${value.toFixed(2)}`, '刚刚'],
          ...current,
        ]);
        notify(
          commission > 0 ? `提现已提交，手续费 ¥${commission.toFixed(2)}` : '提现申请已经提交',
        );
      } catch (requestError) {
        notify(requestError.message);
        return;
      }
    }
    setAmount('');
    setMode(mode === 'recharge' ? 'ledger' : 'history');
  };
  return (
    <Page title="账户余额" back={() => go('profile', 'profile')}>
      <section className="balance-card">
        <div className="balance-amounts">
          <div>
            <span>可用余额</span>
            <strong>¥{balance.toFixed(2)}</strong>
          </div>
          <div>
            <span>冻结中</span>
            <strong>¥{frozenAmount.toFixed(2)}</strong>
          </div>
        </div>
        <p>
          累计已提现 ¥{Number(accountStats.totalWithdrawn || 0).toFixed(2)} · 剩余免费提现额度 ¥
          {remainingFree.toFixed(2)}
        </p>
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
                <b>提现至{r[4] || '招商银行（2816）'}</b>
                <small>{r[2]}</small>
                {r[3] && <small>{r[3]}</small>}
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
              <>
                <div className="recharge-channel">
                  <i>支</i>
                  <span>
                    <b>支付宝</b>
                    <small>仅支持支付宝充值</small>
                  </span>
                </div>
                <p>充值到账后，平台内所有消费将直接从余额扣除。</p>
              </>
            ) : (
              <>
                <button className="bank-row" type="button" onClick={openBankEditor}>
                  <Landmark />
                  <span>
                    <b>到账银行卡</b>
                    <small>{boundBank ? bankLabel : '尚未绑定，点击添加'}</small>
                  </span>
                  <em>{boundBank ? '修改' : '添加'}</em>
                  <ChevronRight />
                </button>
                <p>
                  本次手续费 ¥{commission.toFixed(2)}，预计到账 ¥{arrivalAmount.toFixed(2)}。
                  累计提现10,000元以内免费，超出部分收取20%。
                </p>
              </>
            )}
          </section>
          <button disabled={!Number(amount)} className="sticky-primary" onClick={submitMoney}>
            {mode === 'recharge' ? '支付宝充值' : boundBank ? '确认提现' : '绑定银行卡并提现'}
          </button>
        </>
      )}

      {editingBank && (
        <>
          <button className="sheet-mask" type="button" onClick={() => setEditingBank(false)} />
          <section className="bank-editor-sheet">
            <header>
              <div>
                <h2>{boundBank ? '修改银行卡' : '添加银行卡'}</h2>
                <p>仅可绑定一张银行卡</p>
              </div>
              <button type="button" onClick={() => setEditingBank(false)} aria-label="关闭">
                <X />
              </button>
            </header>
            <label>
              <span>持卡人</span>
              <input
                value={bankDraft.holderName}
                onChange={(event) => {
                  setBankDraft((current) => ({
                    ...current,
                    holderName: event.target.value,
                  }));
                  setBankError('');
                }}
                placeholder="请输入持卡人姓名"
              />
            </label>
            <label>
              <span>开户银行</span>
              <select
                value={bankDraft.bankName}
                onChange={(event) => {
                  setBankDraft((current) => ({
                    ...current,
                    bankName: event.target.value,
                  }));
                  setBankError('');
                }}
              >
                <option value="">请选择开户银行</option>
                <option>中国工商银行</option>
                <option>中国农业银行</option>
                <option>中国银行</option>
                <option>中国建设银行</option>
                <option>交通银行</option>
                <option>招商银行</option>
                <option>中国邮政储蓄银行</option>
              </select>
            </label>
            <label>
              <span>银行卡号</span>
              <input
                inputMode="numeric"
                value={bankDraft.cardNumber}
                onChange={(event) => {
                  const cardNumber = event.target.value.replace(/\D/g, '').slice(0, 19);
                  setBankDraft((current) => ({ ...current, cardNumber }));
                  setBankError('');
                }}
                placeholder="请输入银行卡号"
              />
            </label>
            {bankError && <p className="bank-editor-error">{bankError}</p>}
            <button className="bank-editor-save" type="button" onClick={saveBank}>
              {boundBank ? '保存修改' : '确认绑定'}
            </button>
          </section>
        </>
      )}
    </Page>
  );
}
