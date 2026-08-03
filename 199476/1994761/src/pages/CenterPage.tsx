import { Link } from "@tanstack/react-router";
import { useEffect, useMemo, useState } from "react";
import { appService } from "../application/appService";
import { type CenterTabPath, useAppNavigate } from "../application/navigation";
import { useAppStore } from "../application/store";
import { AdminPanel } from "../features/center/AdminPanel";
import { BalancePanel } from "../features/center/BalancePanel";
import { IdeasPanel } from "../features/center/IdeasPanel";
import { NotificationsPanel } from "../features/center/NotificationsPanel";
import { PackagesPanel } from "../features/center/PackagesPanel";
import { SettingsPanel } from "../features/center/SettingsPanel";
import { TeamPanel } from "../features/center/TeamPanel";
import { GlobalFeedback } from "../features/feedback/GlobalFeedback";
import { formatMoney } from "../shared/lib/format";
import { errorMessage, useAsyncAction } from "../shared/lib/useAsyncAction";
import { Icon, type IconName } from "../shared/ui/Icon";
import { useToast } from "../shared/ui/Toast";
import "../styles/pages/center.css";

type CenterTab = CenterTabPath;
const titles: Record<CenterTab, string> = {
  ideas: "我的想法",
  balance: "账户充值",
  packages: "购买套餐",
  notifications: "消息通知",
  team: "加入我们",
  settings: "账号设置",
  admin: "超级管理员",
};
const navigation: [CenterTab, IconName, string][] = [
  ["ideas", "lightbulb", "我的想法"],
  ["packages", "package", "购买套餐"],
  ["balance", "wallet", "账户充值"],
  ["notifications", "bell", "消息通知"],
  ["team", "user-plus", "加入我们"],
  ["settings", "settings", "账号设置"],
];

function CooperationDialog({
  open,
  onClose,
  onRecharge,
}: {
  open: boolean;
  onClose: () => void;
  onRecharge: () => void;
}) {
  const { data, user, sessionEmail } = useAppStore();
  const notify = useToast();
  const { isPending, run } = useAsyncAction();
  if (!open || !user) return null;
  const deposit = data.cooperationDeposits.find(
    (item) =>
      item.userEmail === sessionEmail &&
      !["已退回", "已取消"].includes(item.status),
  );
  const amount = 2_000;
  return (
    <div
      className="cooperation-modal open"
      onMouseDown={(event) => {
        if (event.currentTarget === event.target) onClose();
      }}
    >
      <div className="cooperation-dialog">
        <h2>商务合作</h2>
        <p>适用于中大型项目、深度定制和长期合作。</p>
        {deposit && (
          <div className="cooperation-email">timeline.1994.1976@gmail.com</div>
        )}
        {!deposit && (
          <div className="cooperation-email">******@gmail.com</div>
        )}
        <ul className="cooperation-notes">
          <li>请先整理项目目标、预算范围和期望时间。</li>
          <li>押金用于确认真实合作意向，减少无效沟通。</li>
          <li>沟通结束后，无论是否合作，押金都会全额退回。</li>
        </ul>
        <div className="deposit-box">
          <strong>{formatMoney(amount)} 合作押金</strong>
          <p>
            {deposit
              ? `当前状态：${deposit.status} · 联系邮箱已开放`
              : `当前余额：${formatMoney(user.balance)}`}
          </p>
        </div>
        <div className="cooperation-actions">
          <button className="button ghost" type="button" onClick={onClose}>
            关闭
          </button>
          {deposit ? (
            <a
              className="button primary"
              href="mailto:timeline.1994.1976@gmail.com"
            >
              发送合作邮件
            </a>
          ) : user.balance >= amount ? (
            <button
              className="button primary"
              type="button"
              disabled={isPending}
              onClick={async () => {
                try {
                  const result = await run(() =>
                    appService.createDeposit(sessionEmail, amount),
                  );
                  if (result.ok) notify("合作押金已支付，联系邮箱已开放。");
                  else notify(result.error || "支付失败。");
                } catch (error) {
                  notify(errorMessage(error, "支付失败，请稍后重试。"));
                }
              }}
            >
              {isPending ? "支付中…" : `余额支付 ${formatMoney(amount)}`}
            </button>
          ) : (
            <button
              className="button primary"
              type="button"
              onClick={onRecharge}
            >
              余额不足，去充值
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

export function CenterPage({ requestedTab }: { requestedTab: CenterTabPath }) {
  const { user, sessionEmail, isAdmin, data } = useAppStore();
  const appNavigate = useAppNavigate();
  const [cooperationOpen, setCooperationOpen] = useState(false);
  const allowed = useMemo<CenterTab[]>(
    () => [
      "ideas",
      "balance",
      "packages",
      "notifications",
      "team",
      "settings",
      ...(isAdmin ? ["admin" as const] : []),
    ],
    [isAdmin],
  );
  const tab: CenterTab = allowed.includes(requestedTab)
    ? requestedTab
    : "ideas";
  const unread = data.notifications.filter(
    (item) => item.userEmail === sessionEmail && !item.read,
  ).length;
  useEffect(() => {
    document.title = "点成｜助力小想法建站";
    if (!user) return;
    void appService.ensureDerivedNotifications(sessionEmail);
    // 派生通知只需在进入账号时生成一次。
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessionEmail, Boolean(user)]);
  useEffect(() => {
    if (!allowed.includes(requestedTab))
      void appNavigate("/center/ideas", { replace: true });
  }, [allowed, appNavigate, requestedTab]);
  useEffect(() => {
    if (!sessionEmail) {
      void appNavigate("/login", {
        replace: true,
        redirect: `/center/${requestedTab}`,
      });
    }
  }, [appNavigate, requestedTab, sessionEmail]);
  if (!user) return null;
  const navigateTab = (next: string) => {
    const nextTab = next as CenterTab;
    if (!allowed.includes(nextTab)) return;
    void appNavigate(`/center/${nextTab}`);
  };
  const panel =
    tab === "ideas" ? (
      <IdeasPanel onNavigate={navigateTab} />
    ) : tab === "balance" ? (
      <BalancePanel />
    ) : tab === "packages" ? (
      <PackagesPanel onNavigate={navigateTab} />
    ) : tab === "notifications" ? (
      <NotificationsPanel onNavigate={navigateTab} />
    ) : tab === "team" ? (
      <TeamPanel />
    ) : tab === "settings" ? (
      <SettingsPanel />
    ) : (
      <AdminPanel />
    );
  const logout = async () => {
    await appService.logout();
    await appNavigate("/login", { replace: true, redirect: "/center/ideas" });
  };
  return (
    <div className="center-page">
      <div className="app">
        <aside className="side">
          <div className="side-brand-row">
            <Link className="brand" to="/" hash="top">
              <span className="logo">点</span>点成
            </Link>
            <button
              className="business-link"
              type="button"
              onClick={() => setCooperationOpen(true)}
            >
              商务合作
            </button>
          </div>
          <div className="side-profile">
            <b>{user.name || user.email.split("@")[0]}</b>
            <small>{user.email}</small>
            {isAdmin && <span className="admin-chip">超级管理员</span>}
            <div className="side-invite">
              <span>我的邀请码</span>
              <strong>{user.inviteCode}</strong>
            </div>
            <div className="side-balance">
              <span>账户余额</span>
              <strong>{formatMoney(user.balance)}</strong>
            </div>
          </div>
          <nav className="tabs" aria-label="个人中心导航">
            {navigation.map(([key, icon, label]) => (
              <button
                className={`tab ${tab === key ? "active" : ""} ${key === "notifications" ? "notification-tab" : ""}`}
                type="button"
                key={key}
                onClick={() => navigateTab(key)}
              >
                <span className="tab-icon"><Icon name={icon} /></span>
                {label}
                {key === "notifications" && (
                  <span
                    className={`notification-badge ${unread ? "show" : ""}`}
                  >
                    {unread > 99 ? "99+" : unread}
                  </span>
                )}
              </button>
            ))}
            {isAdmin && (
              <button
                className={`tab admin-tab ${tab === "admin" ? "active" : ""}`}
                type="button"
                onClick={() => navigateTab("admin")}
              >
                <span className="tab-icon"><Icon name="shield" /></span>超级管理员
              </button>
            )}
          </nav>
          <div className="side-bottom">
            <Link className="home-link-right" to="/" hash="top">
              <span className="text-button primary"><Icon name="arrow-left" /> 返回首页</span>
            </Link>
            <div className="local">点成服务中心</div>
          </div>
        </aside>
        <main className="main">
          <header className="topbar">
            <h1>{titles[tab]}</h1>
            <div className="top-actions">
              <Link
                className="button ghost"
                style={{ fontSize: 16 }}
                to="/ideas"
              >
                <Icon name="trophy" />
              </Link>
              <button className="button" type="button" onClick={logout}>
                退出账户
              </button>
            </div>
          </header>
          <div className="content">{panel}</div>
        </main>
      </div>
      <CooperationDialog
        open={cooperationOpen}
        onClose={() => setCooperationOpen(false)}
        onRecharge={() => {
          setCooperationOpen(false);
          navigateTab("balance");
        }}
      />
      <GlobalFeedback />
    </div>
  );
}
