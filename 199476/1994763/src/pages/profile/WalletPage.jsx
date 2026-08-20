import { useEffect, useState } from 'react';
import { BadgeDollarSign, ChevronRight } from 'lucide-react';
import Page from '../../components/layout/Page.jsx';
import './ProfilePages.css';
import { api } from '../../api/http.js';
import { walletTransactionFromApi, withdrawalFromApi } from '../../utils/walletView.js';

function createRequestId() {
  return `withdraw_${crypto.randomUUID().replaceAll('-', '')}`;
}

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
  const [withdrawalCode, setWithdrawalCode] = useState('');
  const boundAlipay = accountStats.alipayAccount || null;

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
      setRecords(transactionItems.map(walletTransactionFromApi));
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
      if (!capability.available) notify(capability.message, 'warning');
    } catch (requestError) {
      notify(requestError.message, 'error');
    }
  };

  const openWithdrawalHistory = async () => {
    setMode('history');
    try {
      const withdrawalItems = await api.withdrawals();
      setWithdrawals(withdrawalItems.map(withdrawalFromApi));
    } catch (requestError) {
      notify(requestError.message, 'error');
    }
  };

  const openAlipayEditor = () => {
    notify('请在事先问 App 内完成支付宝授权', 'default');
  };

  const sendWalletCode = async (purpose) => {
    try {
      await api.sendWalletCode(purpose);
      notify('验证码已发送', 'success');
    } catch (requestError) {
      notify(requestError.message, 'error');
    }
  };

  const submitMoney = async () => {
    const value = Number(amount);
    if (!Number.isInteger(value) || value < 1 || value > 9999) {
      notify('金额必须是1至9999的整数', 'warning');
      return;
    }

    if (mode === 'recharge') {
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
      if (!boundAlipay) {
        openAlipayEditor();
        return;
      }
      if (!/^\d{4}$/.test(withdrawalCode)) {
        notify('请输入4位验证码', 'warning');
        return;
      }
      try {
        await api.withdraw({
          amount: value,
          requestId: createRequestId(),
          verificationCode: withdrawalCode,
        });
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
        setRecords(transactionItems.map(walletTransactionFromApi));
        setWithdrawals(withdrawalItems.map(withdrawalFromApi));
        setWithdrawalCode('');
        notify('提现申请已经提交', 'success');
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
        <p>累计已提现 ¥{Number(accountStats.totalWithdrawn || 0).toFixed(2)}</p>
      </section>

      <div className="wallet-tabs four">
        <button className={mode === 'ledger' ? 'active' : ''} onClick={() => setMode('ledger')}>
          收支明细
        </button>
        <button className={mode === 'history' ? 'active' : ''} onClick={openWithdrawalHistory}>
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
          {records.map((record, index) => (
            <article key={index}>
              <i className={record[0] === '收入' ? 'income' : record[0] === '冻结' || record[0] === '解冻' ? 'frozen' : 'expense'}>
                {record[0] === '收入' ? '收' : record[0] === '冻结' ? '冻' : record[0] === '解冻' ? '解' : '支'}
              </i>
              <div>
                <b>{record[1]}</b>
                <small>{record[3]}</small>
              </div>
              <strong className={record[0] === '收入' ? 'income' : record[0] === '冻结' || record[0] === '解冻' ? 'frozen' : ''}>
                {record[2]}
              </strong>
            </article>
          ))}
        </section>
      )}

      {mode === 'history' && (
        <section className="withdraw-list">
          {withdrawals.map((record, index) => (
            <article key={index}>
              <div>
                <b>提现至{record[4]}</b>
                <small>{record[2]}</small>
                {record[3] && <small>{record[3]}</small>}
              </div>
              <strong>{record[0]}</strong>
              <span className={['处理中', '支付处理中'].includes(record[1]) ? 'pending' : ''}>
                {record[1]}
              </span>
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
                inputMode="numeric"
                value={amount}
                onChange={(event) => setAmount(event.target.value.replace(/\D/g, '').slice(0, 4))}
                placeholder="0"
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
                <p>充值到账后，平台内消费将直接从余额扣除。</p>
              </>
            ) : (
              <>
                <button className="bank-row" type="button" onClick={openAlipayEditor}>
                  <BadgeDollarSign />
                  <span>
                    <b>支付宝收款账户</b>
                    <small>
                      {boundAlipay
                        ? `${boundAlipay.displayName} · ${boundAlipay.accountMasked}`
                        : '尚未授权，点击前往 App 授权'}
                    </small>
                  </span>
                  <em>{boundAlipay ? '重新授权' : '去授权'}</em>
                  <ChevronRight />
                </button>
                <label className="wallet-code-field">
                  <span>短信验证码</span>
                  <input
                    inputMode="numeric"
                    value={withdrawalCode}
                    onChange={(event) => setWithdrawalCode(event.target.value.replace(/\D/g, '').slice(0, 4))}
                    placeholder="请输入4位验证码"
                  />
                  <button type="button" onClick={() => sendWalletCode('WITHDRAWAL')}>
                    获取验证码
                  </button>
                </label>
                <p>只有回答收入可以提现，充值余额不可提现。</p>
              </>
            )}
          </section>

          <button disabled={!Number(amount)} className="sticky-primary" onClick={submitMoney}>
            {mode === 'recharge' ? '支付宝充值' : boundAlipay ? '确认提现' : '先授权支付宝'}
          </button>
        </>
      )}

    </Page>
  );
}
