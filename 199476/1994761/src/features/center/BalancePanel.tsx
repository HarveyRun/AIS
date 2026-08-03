import { useState } from "react";
import { appService } from "../../application/appService";
import { useAppStore } from "../../application/store";
import { formatDate, formatMoney } from "../../shared/lib/format";
import { errorMessage, useAsyncAction } from "../../shared/lib/useAsyncAction";
import { EmptyState } from "../../shared/ui/EmptyState";
import { useToast } from "../../shared/ui/Toast";

export function BalancePanel() {
  const { user, sessionEmail } = useAppStore();
  const notify = useToast();
  const [preset, setPreset] = useState(50);
  const [custom, setCustom] = useState("");
  const [pending, setPending] = useState<number | null>(null);
  const { isPending: isRecharging, run: runRecharge } = useAsyncAction();
  if (!user) return null;
  const selected = custom === "" ? preset : Number(custom);
  return (
    <section className="panel active">
      <p className="panel-lead">充值后，余额可用于想法费用和套餐开通。</p>
      <div className="balance-hero">
        <small>可用余额</small>
        <strong>{formatMoney(user.balance)}</strong>
        <p>余额可用于支付想法费用和开通服务套餐。</p>
      </div>
      <div className="recharge-card">
        <h2>支付宝充值</h2>
        <p>选择或输入充值金额，再完成支付宝支付。</p>
        <div className="amounts">
          {[50, 100, 300, 500].map((amount) => (
            <button
              className={`amount ${!custom && preset === amount ? "active" : ""}`}
              type="button"
              key={amount}
              onClick={() => {
                setPreset(amount);
                setCustom("");
                setPending(null);
              }}
            >
              ¥{amount}
            </button>
          ))}
        </div>
        <div className="custom-amount">
          <input
            type="number"
            min="1"
            max="99999"
            step="0.01"
            placeholder="其他金额"
            value={custom}
            onChange={(event) => {
              setCustom(event.target.value);
              setPending(null);
            }}
          />
          <button
            className="button primary"
            type="button"
            onClick={() => {
              if (!Number.isFinite(selected) || selected < 1 || selected > 99999) {
                notify("充值金额应在 1—99999 元之间。");
                return;
              }
              setPending(Number(selected.toFixed(2)));
            }}
          >
            支付宝充值
          </button>
        </div>
        {pending !== null && (
          <div className="alipay">
            <div className="alipay-head">
              <div className="alipay-mark">
                <i>支</i>
                <span>支付宝</span>
              </div>
              <strong>{formatMoney(pending)}</strong>
            </div>
            <p>请在支付宝完成支付后返回本页。</p>
            <button
              className="button"
              type="button"
              disabled={isRecharging}
              onClick={async () => {
                try {
                  await runRecharge(() =>
                    appService.recharge(sessionEmail, pending),
                  );
                  setPending(null);
                  notify("充值完成，余额已更新。");
                } catch (error) {
                  notify(errorMessage(error, "充值失败，请稍后重试。"));
                }
              }}
            >
              {isRecharging ? "确认中…" : "我已完成支付"}
            </button>
          </div>
        )}
      </div>
      <div className="section-head">
        <h2>余额记录</h2>
      </div>
      <div className="settings-card">
        {user.transactions.length ? (
          user.transactions.map((transaction) => (
            <div className="transaction" key={transaction.id}>
              <div>
                <b>{transaction.title}</b>
                <span className="jltime">{formatDate(transaction.createdAt)}</span>
              </div>
              <strong className={transaction.type === "credit" ? "credit" : ""}>
                {transaction.type === "credit" ? "+" : "-"}
                {formatMoney(transaction.amount)}
              </strong>
            </div>
          ))
        ) : (
          <EmptyState title="暂无余额记录" />
        )}
      </div>
    </section>
  );
}
