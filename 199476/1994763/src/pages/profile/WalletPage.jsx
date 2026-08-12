import { useEffect, useState } from 'react';
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
  const [rechargeCapability, setRechargeCapability] = useState(null);
  const boundBank = accountStats.bankCard || null;
  const numericAmount = Number(amount || 0);
  const remainingFree = Math.max(0, 10000 - Number(accountStats.totalWithdrawn || 0));
  const commissionBase = Math.max(0, numericAmount - remainingFree);
  const commission = Number((commissionBase * 0.2).toFixed(2));
  const arrivalAmount = Number(Math.max(0, numericAmount - commission).toFixed(2));
  const bankLabel = boundBank ? `${boundBank.bankName}（${boundBank.cardNumber.slice(-4)}）` : '';

  useEffect(() => {
    const parameters = new URLSearchParams(window.location.search);
    const orderNo = parameters.get('orderNo');
    if (parameters.get('recharge') !== 'success' || !orderNo) return;

    Promise.all([
      api.rechargeOrder(orderNo),
      api.wallet(),
      api.walletTransactions(),
    ]).then(([order, wallet, transactionItems]) => {
      setBalance(Number(wallet.availableBalance));
      setAccountStats((current) => ({
        ...current,
        totalWithdrawn: Number(wallet.totalWithdrawn),
      }));
      setRecords(transactionItems.map((item) => [
        item.direction === 'IN' ? '收入' : '支出',
        item.description,
        `${item.direction === 'IN' ? '+' : '-'}¥${item.amount}`,
        new Date(item.createdAt).toLocaleString(),
      ]));
      notify(
        order.status === 'PAID' ? '充值成功' : '支付结果确认中',
        order.status === 'PAID' ? 'success' : 'default',
      );
      window.history.replaceState({}, '', window.location.pathname);
      setMode('ledger');
    }).catch((requestError) => notify(requestError.message, 'error'));
  }, [notify, setAccountStats, setBalance, setRecords]);

  const openRecharge = async () => {
    setMode('recharge');
    try {
      const capability = await api.rechargeCapability();
      setRechargeCapability(capability);
      if (!capability.available) notify(capability.message, 'warning');
    } catch (requestError) {
      notify(requestError.message, 'error');
    }
  };

  const openBankEditor = () => {
    setBankDraft(
      boundBank || {
        bankName: '',
        cardNumber: '',
        holderName: '',
      },
    );
    setEditingBank(true);
  };

  const saveBank = async () => {
    const cardNumber = bankDraft.cardNumber.replace(/\s/g, '');
    if (!bankDraft.holderName.trim()) {
      notify('请填写持卡人姓名', 'warning');
      return;
    }
    if (!bankDraft.bankName) {
      notify('请选择开户银行', 'warning');
      return;
    }
    if (!/^\d{12,19}$/.test(cardNumber)) {
      notify('请输入正确的银行卡号', 'warning');
      return;
    }

    try {
      await api.bindBankCard({
        ...bankDraft,
        cardNumber,
        holderName: bankDraft.holderName.trim(),
      });
      const saved = await api.bankCard();
      setAccountStats((current) => ({
        ...current,
        bankCard: {
          holderName: saved.holderName,
          bankName: saved.bankName,
          cardNumber: saved.lastFour,
        },
      }));
      setEditingBank(false);
      notify(boundBank ? '银行卡已修改' : '银行卡已绑定', 'success');
    } catch (requestError) {
      notify(requestError.message, 'error');
    }
  };

  const submitMoney = async () => {
    const value = Number(amount);
    if (!Number.isFinite(value) || value <= 0) return;
    if (mode === 'withdraw' && value > balance) {
      notify('余额不足', 'warning');
      return;
    }
    if (mode === 'withdraw' && !boundBank) {
      openBankEditor();
      return;
    }
    if (mode === 'recharge') {
      if (rechargeCapability?.available === false) {
        notify(rechargeCapability.message, 'warning');
        return;
      }
      try {
        const order = await api.createRecharge(value);
        notify(
          order.paymentUrl ? '正在打开支付宝' : '充值订单已创建，请完成支付宝支付',
          'default',
        );
        if (order.paymentUrl) window.location.href = order.paymentUrl;
      } catch (requestError) {
        notify(requestError.message, 'error');
        return;
      }
    } else {
      try {
        await api.withdraw(value);
        const [wallet, transactionItems, withdrawalItems] = await Promise.all([
          api.wallet(),
          api.walletTransactions(),
          api.withdrawals(),
        ]);
        setBalance(Number(wallet.availableBalance));
        setAccountStats((current) => ({
          ...current,
          totalWithdrawn: Number(wallet.totalWithdrawn),
        }));
        setRecords(transactionItems.map((item) => [
          item.direction === 'IN' ? '收入' : '支出',
          item.description,
          `${item.direction === 'IN' ? '+' : '-'}¥${item.amount}`,
          new Date(item.createdAt).toLocaleString(),
        ]));
        setWithdrawals(withdrawalItems.map((item) => [
          `¥${item.amount}`,
          item.status === 'COMPLETED' ? '已到账' : '处理中',
          new Date(item.createdAt).toLocaleString(),
        ]));
        notify(
          commission > 0 ? `提现已提交，手续费 ¥${commission.toFixed(2)}` : '提现申请已经提交',
          'success',
        );
      } catch (requestError) {
        notify(requestError.message, 'error');
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
        <button className={mode === 'recharge' ? 'active' : ''} onClick={openRecharge}>
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
          <button
            disabled={!Number(amount) || (mode === 'recharge' && rechargeCapability?.available === false)}
            className="sticky-primary"
            onClick={submitMoney}
          >
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
                }}
                placeholder="请输入银行卡号"
              />
            </label>
            <button className="bank-editor-save" type="button" onClick={saveBank}>
              {boundBank ? '保存修改' : '确认绑定'}
            </button>
          </section>
        </>
      )}
    </Page>
  );
}
