import { useMemo, useState } from "react";
import { Link } from "@tanstack/react-router";
import { appService } from "../../application/appService";
import { useAppStore } from "../../application/store";
import type { Idea } from "../../domain/types";
import {
  formatDate,
  formatMoney,
  isSingleIdea,
  statusClass,
} from "../../shared/lib/format";
import { EmptyState } from "../../shared/ui/EmptyState";
import { errorMessage, useAsyncAction } from "../../shared/lib/useAsyncAction";
import { useToast } from "../../shared/ui/Toast";
import { Icon } from "../../shared/ui/Icon";

interface IdeasPanelProps {
  onNavigate: (tab: string) => void;
}

function PayBox({
  idea,
  onNavigate,
}: {
  idea: Idea;
  onNavigate: (tab: string) => void;
}) {
  const { user, sessionEmail } = useAppStore();
  const notify = useToast();
  const { isPending, run } = useAsyncAction();
  if (!user || idea.status !== "待付款") return null;
  return (
    <div className="pay-box">
      <p>
        <b>{formatMoney(idea.fee)}</b>只能使用账户余额付款
      </p>
      {user.balance >= idea.fee ? (
        <button
          className="button primary small"
          type="button"
          disabled={isPending}
          onClick={async () => {
            try {
              const result = await run(() =>
                appService.payIdea(sessionEmail, idea.id),
              );
              notify(
                result.ok
                  ? "付款完成，想法已进入队列。"
                  : result.error || "付款失败。",
              );
            } catch (error) {
              notify(errorMessage(error, "付款失败，请稍后重试。"));
            }
          }}
        >
          {isPending ? "支付中…" : "余额支付"}
        </button>
      ) : (
        <button
          className="button small"
          type="button"
          onClick={() => onNavigate("balance")}
        >
          余额不足，去充值
        </button>
      )}
    </div>
  );
}

export function IdeasPanel({ onNavigate }: IdeasPanelProps) {
  const { user, sessionEmail } = useAppStore();
  const notify = useToast();
  const [activeParent, setActiveParent] = useState<string | null>(null);
  const [iteration, setIteration] = useState("");
  const [error, setError] = useState("");
  const ideas = useMemo(() => user?.ideas || [], [user?.ideas]);
  const roots = useMemo(
    () => ideas.filter((idea) => idea.type === "new"),
    [ideas],
  );
  const iterations = useMemo(
    () => ideas.filter((idea) => idea.type === "iteration"),
    [ideas],
  );
  if (!user) return null;
  return (
    <section className="panel active">
      <p className="panel-lead">
        管理中心
      </p>
      <div className="summary">
        <div className="stat">
          <small>新想法</small>
          <b>{String(roots.length).padStart(2, "0")}</b>
        </div>
        <div className="stat">
          <small>后续迭代</small>
          <b>{String(iterations.length).padStart(2, "0")}</b>
        </div>
        <div className="stat">
          <small>处理中</small>
          <b>
            {String(
              ideas.filter(
                (idea) => !["已完成", "不制作"].includes(idea.status),
              ).length,
            ).padStart(2, "0")}
          </b>
        </div>
      </div>
      <div className="section-head">
        <h2>全部记录</h2>
      </div>
      <div className="idea-list">
        {roots.length ? (
          roots.map((root) => {
            const children = iterations.filter(
              (idea) => idea.parentId === root.id,
            );
            return (
              <article className="idea-card" key={root.id}>
                <div className="idea-main">
                  <div>
                    <h3>{root.text}</h3>
                    <div className="meta">
                      <span>新想法</span>
                      <span>{formatDate(root.createdAt)}</span>
                      <span>
                        {root.level ? `评级 ${root.level}` : "尚未评级"}
                      </span>
                      <span><Icon name="heart" /> {root.likedBy.length}</span>
                    </div>
                  </div>
                  <span className={`status ${statusClass(root.status)}`}>
                    {root.status || "待评估"}
                  </span>
                </div>
                <PayBox idea={root} onNavigate={onNavigate} />
                <div className="idea-actions">
                  <span className="meta">
                    {children.length} 次迭代 ·{" "}
                    {root.isPublic ? "公开" : "仅自己可见"}
                  </span>
                  <span>
                    <button
                      className="text-button"
                      type="button"
                      onClick={async () => {
                        await appService.toggleIdeaVisibility(
                          sessionEmail,
                          root.id,
                        );
                        notify(root.isPublic ? "已设为私密。" : "已公开。");
                      }}
                    >
                      {root.isPublic ? "设为私密" : "公开"}
                    </button>{" "}
                    <button
                      className="text-button"
                      type="button"
                      onClick={() => {
                        setActiveParent(root.id);
                        setIteration("");
                        setError("");
                      }}
                    >
                      提出迭代 <Icon name="arrow-up-right" />
                    </button>
                  </span>
                </div>
                {children.length > 0 && (
                  <div className="iteration-list">
                    {children.map((child) => (
                      <div className="iteration" key={child.id}>
                        <div className="idea-main">
                          <div>
                            <h4>{child.text}</h4>
                            <div className="meta">
                              <span>{formatDate(child.createdAt)}</span>
                              <span>
                                {child.level
                                  ? `评级 ${child.level}`
                                  : "尚未评级"}
                              </span>
                            </div>
                          </div>
                          <span
                            className={`status ${statusClass(child.status)}`}
                          >
                            {child.status || "待评估"}
                          </span>
                        </div>
                        <PayBox idea={child} onNavigate={onNavigate} />
                      </div>
                    ))}
                  </div>
                )}
                {activeParent === root.id && (
                  <div className="iteration-slot">
                    <form
                      className="iteration-form"
                      onSubmit={async (event) => {
                        event.preventDefault();
                        if (!isSingleIdea(iteration)) {
                          setError(
                            "请用一句完整的话，只说明这次要更新的一件事。",
                          );
                          return;
                        }
                        await appService.addIdea(
                          sessionEmail,
                          "iteration",
                          iteration,
                          root.id,
                          false,
                        );
                        setActiveParent(null);
                        notify("迭代已提交，等待评估。");
                      }}
                    >
                      <label htmlFor={`iteration-${root.id}`}>
                        这次要更新什么？
                      </label>
                      <textarea
                        id={`iteration-${root.id}`}
                        maxLength={80}
                        placeholder="一句话说明这次更新，并保持原来的方向"
                        value={iteration}
                        onChange={(event) => setIteration(event.target.value)}
                        autoFocus
                      />
                      <div className="form-error">{error}</div>
                      <div className="iteration-actions">
                        <button
                          className="button ghost small"
                          type="button"
                          onClick={() => setActiveParent(null)}
                        >
                          取消
                        </button>
                        <button className="button primary small" type="submit">
                          提交迭代
                        </button>
                      </div>
                    </form>
                  </div>
                )}
              </article>
            );
          })
        ) : (
          <EmptyState
            title="还没有提交过想法"
            description="赶快提交一个想法吧~"
            action={
              <Link className="button primary" to="/" hash="top">
                提出想法
              </Link>
            }
          />
        )}
      </div>
    </section>
  );
}
