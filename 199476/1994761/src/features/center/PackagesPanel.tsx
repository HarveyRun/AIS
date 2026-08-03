import { useState } from "react";
import { Link } from "@tanstack/react-router";
import { appService } from "../../application/appService";
import { useAppStore } from "../../application/store";
import { PACKAGES, type PackageId } from "../../domain/constants";
import { formatDate, formatMoney } from "../../shared/lib/format";
import { ConfirmModal } from "../../shared/ui/ConfirmModal";
import { EmptyState } from "../../shared/ui/EmptyState";
import { useToast } from "../../shared/ui/Toast";
import { Icon } from "../../shared/ui/Icon";

export function PackagesPanel({
  onNavigate,
}: {
  onNavigate: (tab: string) => void;
}) {
  const { user, sessionEmail } = useAppStore();
  const notify = useToast();
  const [pending, setPending] = useState<PackageId | null>(null);
  const [currentTime] = useState(() => Date.now());
  if (!user) return null;
  const active = user.activePackage;
  const definition = pending
    ? PACKAGES.find((item) => item.id === pending)
    : null;
  const days = active
    ? Math.ceil(
        (new Date(active.expiresAt).getTime() - currentTime) / 86_400_000,
      )
    : 0;
  return (
    <section className="panel active">
      <p className="panel-lead">
        选择适合你的套餐{" "}
        <Link className="rules-link" to="/rules" hash="packages">
          查看套餐权益与规则
        </Link>
      </p>
      {active && (
        <div className="current-package">
          <div>
            <small>当前套餐</small>
            <h2>{active.packageName}</h2>
            <br/>
            <div className="quota">
              <span className="jltimeTf">
                剩余项目 <b>{active.projectQuota}</b>
              </span>
              <br/>
              <span className="jltimeTf">
                剩余迭代 <b>{active.iterationQuota}</b>
              </span>
              <br/>
              <br/>
              <p>
                有效期至{" "}
                {formatDate(active.expiresAt, false)}
              </p>
            </div>
          </div>
          <div>
            <div className="quota-alert">
              <button
                className="button primary small"
                type="button"
                onClick={() => setPending(active.packageId)}
              >
                续费当前套餐
              </button>
            </div>
          </div>
        </div>
      )}
      <div className="package-grid">
        {PACKAGES.map((item) => {
          const featured = "featured" in item && item.featured;
          return (
            <article
              className={`package-card ${featured ? "featured" : ""}`}
              key={item.id}
            >
              <span className="package-level">{item.levelRange}</span>
              <h2>{item.name}</h2>
              <strong className="package-price">
                {formatMoney(item.price)}
                <small>/ 30 天</small>
              </strong>
              <ul>
                {item.benefits.map((benefit) => (
                  <li key={benefit}><Icon name="check" />{benefit}</li>
                ))}
              </ul>
              <div className="package-pay-note">余额支付 · 有效期 30 天</div>
              <button
                className={`button ${featured ? "primary" : ""}`}
                type="button"
                onClick={() => setPending(item.id)}
              >
                {active?.packageId === item.id ? "续费套餐" : "开通套餐"}
              </button>
            </article>
          );
        })}
      </div>
      <div className="section-head">
        <h2>套餐订单</h2>
      </div>
      <div className="settings-card">
        {user.packageOrders.length ? (
          user.packageOrders.map((order) => (
            <div className="transaction" key={order.id}>
              <div>
                <b>{order.packageName}</b>
                <span className="jltime">
                  {formatDate(order.createdAt)} · 有效期至{" "}
                  {formatDate(order.expiresAt, false)}
                </span>
              </div>
              <strong>-{formatMoney(order.amount)}</strong>
            </div>
          ))
        ) : (
          <EmptyState title="暂无套餐订单" />
        )}
      </div>
      <ConfirmModal
        open={Boolean(definition)}
        title="确认开通套餐"
        confirmLabel="确认余额支付"
        onCancel={() => setPending(null)}
        onConfirm={async () => {
          if (!pending) return;
          const result = await appService.purchasePackage(
            sessionEmail,
            pending,
          );
          setPending(null);
          if (!result.ok && result.error?.startsWith("余额不足"))
            onNavigate("balance");
          notify(
            result.ok
              ? `开通成功，“${result.order?.packageName}”已生效。`
              : result.error || "开通失败。",
          );
        }}
      >
        <p>
          {definition?.name} · {definition?.levelRange}
        </p>
        <div className="purchase-summary">
          <span>支付方式：站内余额</span>
          <strong>{formatMoney(definition?.price || 0)}</strong>
        </div>
      </ConfirmModal>
    </section>
  );
}
