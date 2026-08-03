import { useMemo, useState } from "react";
import { appService } from "../../application/appService";
import { useAppStore } from "../../application/store";
import {
  FEEDBACK_CATEGORIES,
  IDEA_STATUS_TRANSITIONS,
  TEAM_STATUSES,
} from "../../domain/constants";
import type {
  Feedback,
  Idea,
  TeamStatus,
  UserAccount,
} from "../../domain/types";
import { fileApi } from "../../api/client";
import { formatDate, formatMoney, statusClass } from "../../shared/lib/format";
import { EmptyState } from "../../shared/ui/EmptyState";
import { useToast } from "../../shared/ui/Toast";

type View = "ideas" | "team" | "feedback" | "cooperation" | "audit";
type IdeaRow = { email: string; user: UserAccount; idea: Idea };

function AdminIdeaCard({ row }: { row: IdeaRow }) {
  const notify = useToast();
  const [level, setLevel] = useState(1);
  const [decision, setDecision] = useState<"制作" | "不制作">("制作");
  const [fee, setFee] = useState("");
  const { email, user, idea } = row;
  const transitions = IDEA_STATUS_TRANSITIONS[idea.status];
  const nextStatus = transitions[0] || idea.status;
  return (
    <article className="admin-card">
      <div className="owner">
        {user.name || email.split("@")[0]} · {email} ·{" "}
        {idea.type === "iteration" ? "后续迭代" : "新想法"}
      </div>
      <div className="admin-top">
        <div>
          <h3>{idea.text}</h3>
          <div className="meta">
            <span>{formatDate(idea.createdAt)}</span>
            <span>{idea.level ? `评级 ${idea.level}` : "尚未评级"}</span>
            {idea.fee > 0 && <span>{formatMoney(idea.fee)}</span>}
          </div>
        </div>
        <span className={`status ${statusClass(idea.status)}`}>
          {idea.status || "待评估"}
        </span>
      </div>
      {idea.status === "待评估" ? (
        <form
          className="admin-form"
          onSubmit={async (event) => {
            event.preventDefault();
            const result = await appService.evaluateIdea(
              email,
              idea.id,
              level,
              decision,
              Number(fee) || 0,
            );
            notify(
              result.ok ? "评估结果已保存。" : result.error || "保存失败。",
            );
          }}
        >
          <select
            aria-label="评级"
            value={level}
            onChange={(event) => setLevel(Number(event.target.value))}
          >
            {[1, 2, 3, 4, 5, 6].map((value) => (
              <option value={value} key={value}>
                {value} 级
              </option>
            ))}
          </select>
          <select
            aria-label="是否制作"
            value={decision}
            onChange={(event) =>
              setDecision(event.target.value as "制作" | "不制作")
            }
          >
            <option value="制作">制作</option>
            <option value="不制作">不制作</option>
          </select>
          <input
            type="number"
            min="0"
            step="0.01"
            placeholder="4—6级费用"
            value={fee}
            onChange={(event) => setFee(event.target.value)}
          />
          <button className="button primary small" type="submit">
            保存评估
          </button>
        </form>
      ) : transitions.length ? (
        <div className="workflow">
          <span className="notice">下一状态：{nextStatus}</span>
          <button
            className="button ghost small"
            type="button"
            onClick={async () => {
              await appService.updateIdeaStatus(email, idea.id, nextStatus);
              notify("制作进度已更新。");
            }}
          >
            更新进度
          </button>
        </div>
      ) : (
        <div className="notice">当前流程已结束，无需继续处理。</div>
      )}
    </article>
  );
}

function AdminFeedbackCard({ feedback }: { feedback: Feedback }) {
  const { sessionEmail } = useAppStore();
  const notify = useToast();
  const [reply, setReply] = useState("");
  return (
    <article className="admin-card">
      <div className="owner">
        {feedback.userEmail || "游客"} · {feedback.category} ·{" "}
        {formatDate(feedback.updatedAt)}
      </div>
      <div className="admin-top">
        <h3>{feedback.messages[0]?.content || "反馈"}</h3>
        <span
          className={`status ${feedback.status === "已结束" ? "green" : ""}`}
        >
          {feedback.status}
        </span>
      </div>
      <div className="thread-messages">
        {feedback.messages.map((message) => (
          <div
            className={`thread-message ${message.role === "admin" ? "admin" : ""}`}
            key={message.id}
          >
            <b>{message.role === "admin" ? "我" : "用户"}</b>
            <p>{message.content}</p>
            <small>{formatDate(message.createdAt)}</small>
          </div>
        ))}
      </div>
      {feedback.status !== "已结束" && (
        <form
          className="thread-reply"
          onSubmit={async (event) => {
            event.preventDefault();
            if (reply.trim().length < 2) {
              notify("请填写完整的回复内容。");
              return;
            }
            try {
              await appService.appendFeedbackMessage(
                feedback.id,
                "admin",
                sessionEmail,
                reply,
              );
              setReply("");
              notify("回复已发送。");
            } catch {
              notify("这条反馈暂时无法回复。");
            }
          }}
        >
          <textarea
            maxLength={500}
            required
            placeholder="发送一条新回复"
            value={reply}
            onChange={(event) => setReply(event.target.value)}
          />
          <div className="thread-reply-actions">
            <button
              className="button ghost small"
              type="button"
              onClick={async () => {
                await appService.closeFeedback(feedback.id, sessionEmail);
                notify("反馈处理已结束。");
              }}
            >
              结束处理
            </button>
            <button className="button primary small" type="submit">
              发送回复
            </button>
          </div>
        </form>
      )}
    </article>
  );
}

function AdminTeamCard({ email, user }: { email: string; user: UserAccount }) {
  const notify = useToast();
  const application = user.teamApplication!;
  const [status, setStatus] = useState<TeamStatus>(application.status);
  return (
    <article className="admin-card">
      <div className="owner">
        {user.name || email.split("@")[0]} · {email}
      </div>
      <div className="admin-top">
        <div>
          <h3>{application.skill}</h3>
          <div className="meta">
            <span>{application.time}</span>
            <span>{application.resumeName}</span>
            <span>{formatDate(application.createdAt)}</span>
          </div>
        </div>
        <span className={`status ${statusClass(application.status)}`}>
          {application.status}
        </span>
      </div>
      <div className="workflow">
        {application.resumeId && (
          <button
            className="button ghost small"
            type="button"
            onClick={async () => {
              const found = await fileApi.download(
                application.resumeId!,
                application.resumeName || "简历.pdf",
              );
              if (!found) notify("没有找到已上传的简历文件。");
            }}
          >
            下载简历
          </button>
        )}
        <select
          value={status}
          onChange={(event) => setStatus(event.target.value as TeamStatus)}
        >
          {TEAM_STATUSES.map((value) => (
            <option key={value}>{value}</option>
          ))}
        </select>
        <button
          className="button small"
          type="button"
          onClick={async () => {
            await appService.updateTeamStatus(email, status);
            notify("团队申请状态已保存。");
          }}
        >
          保存处理结果
        </button>
      </div>
    </article>
  );
}

export function AdminPanel() {
  const { data, isAdmin } = useAppStore();
  const notify = useToast();
  const [view, setView] = useState<View>("ideas");
  const [ideaFilter, setIdeaFilter] = useState("全部");
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState("全部");
  const [feedbackStatus, setFeedbackStatus] = useState("全部");
  const allIdeas = useMemo(() => {
    const rows: IdeaRow[] = [];
    Object.entries(data.users).forEach(([email, user]) =>
      user.ideas.forEach((idea) => rows.push({ email, user, idea })),
    );
    return rows.sort(
      (a, b) =>
        new Date(b.idea.createdAt).getTime() -
        new Date(a.idea.createdAt).getTime(),
    );
  }, [data.users]);
  if (!isAdmin) return null;
  const ideaRows =
    ideaFilter === "全部"
      ? allIdeas
      : allIdeas.filter((row) => row.idea.status === ideaFilter);
  const applications = Object.entries(data.users).filter(
    ([, user]) => user.teamApplication,
  );
  const feedbacks = data.feedbacks.filter(
    (feedback) =>
      (category === "全部" || feedback.category === category) &&
      (feedbackStatus === "全部" || feedback.status === feedbackStatus) &&
      (!query ||
        feedback.userEmail?.toLowerCase().includes(query.toLowerCase()) ||
        feedback.messages.some((message) =>
          message.content.toLowerCase().includes(query.toLowerCase()),
        )),
  );
  const views: [View, string][] = [
    ["ideas", "想法评估"],
    ["team", "团队申请"],
    ["feedback", "用户反馈"],
    ["cooperation", "商务合作"],
    ["audit", "操作记录"],
  ];
  return (
    <section className="panel active">
      <div className="admin-banner">
        <div>
          <h2>超级管理员</h2>
          <p>评估用户想法、处理费用与制作流程，并审核团队申请和用户反馈。</p>
        </div>
        <span className="status">仅管理员可见</span>
      </div>
      <div className="summary">
        <div className="stat">
          <small>待评估</small>
          <b>
            {String(
              allIdeas.filter((row) => row.idea.status === "待评估").length,
            ).padStart(2, "0")}
          </b>
        </div>
        <div className="stat">
          <small>待付款</small>
          <b>
            {String(
              allIdeas.filter((row) => row.idea.status === "待付款").length,
            ).padStart(2, "0")}
          </b>
        </div>
        <div className="stat">
          <small>制作中</small>
          <b>
            {String(
              allIdeas.filter((row) => row.idea.status === "制作中").length,
            ).padStart(2, "0")}
          </b>
        </div>
      </div>
      <div className="admin-subnav">
        {views.map(([key, label]) => (
          <button
            className={view === key ? "active" : ""}
            type="button"
            key={key}
            onClick={() => setView(key)}
          >
            {label}
          </button>
        ))}
      </div>
      {view === "ideas" && (
        <div className="admin-view active">
          <div className="admin-filters">
            {["全部", "待评估", "待付款", "制作中"].map((value) => (
              <button
                className={`filter ${ideaFilter === value ? "active" : ""}`}
                type="button"
                key={value}
                onClick={() => setIdeaFilter(value)}
              >
                {value}
              </button>
            ))}
          </div>
          <div className="admin-list">
            {ideaRows.length ? (
              ideaRows.map((row) => (
                <AdminIdeaCard row={row} key={`${row.email}-${row.idea.id}`} />
              ))
            ) : (
              <EmptyState
                title="没有符合条件的想法"
                description="更换筛选条件后查看。"
              />
            )}
          </div>
        </div>
      )}
      {view === "team" && (
        <div className="admin-view active">
          <div className="admin-list">
            {applications.length ? (
              applications.map(([email, user]) => (
                <AdminTeamCard email={email} user={user} key={email} />
              ))
            ) : (
              <EmptyState
                title="暂无团队申请"
                description="用户提交简历后会显示在这里。"
              />
            )}
          </div>
        </div>
      )}
      {view === "feedback" && (
        <div className="admin-view active">
          <div className="ops-summary">
            <div className="ops-stat">
              <small>待管理员回复</small>
              <b>
                {
                  data.feedbacks.filter(
                    (item) => item.status === "待管理员回复",
                  ).length
                }
              </b>
            </div>
            <div className="ops-stat">
              <small>待用户回复</small>
              <b>
                {
                  data.feedbacks.filter((item) => item.status === "待用户回复")
                    .length
                }
              </b>
            </div>
            <div className="ops-stat">
              <small>已结束</small>
              <b>
                {
                  data.feedbacks.filter((item) => item.status === "已结束")
                    .length
                }
              </b>
            </div>
          </div>
          <div className="admin-ops-toolbar">
            <input
              className="ops-search"
              value={query}
              placeholder="搜索用户或反馈内容"
              onChange={(event) => setQuery(event.target.value)}
            />
            <select
              className="ops-select"
              value={category}
              onChange={(event) => setCategory(event.target.value)}
            >
              <option>全部</option>
              {FEEDBACK_CATEGORIES.map((value) => (
                <option key={value}>{value}</option>
              ))}
            </select>
            <select
              className="ops-select"
              value={feedbackStatus}
              onChange={(event) => setFeedbackStatus(event.target.value)}
            >
              <option>全部</option>
              {["待管理员回复", "待用户回复", "已结束"].map((value) => (
                <option key={value}>{value}</option>
              ))}
            </select>
          </div>
          <div className="admin-list">
            {feedbacks.length ? (
              feedbacks.map((feedback) => (
                <AdminFeedbackCard feedback={feedback} key={feedback.id} />
              ))
            ) : (
              <EmptyState
                title="没有符合条件的反馈"
                description="调整筛选条件后查看。"
              />
            )}
          </div>
        </div>
      )}
      {view === "cooperation" && (
        <div className="admin-view active">
          <div className="admin-list">
            {data.cooperationDeposits.length ? (
              data.cooperationDeposits.map((deposit) => (
                <article className="deposit-card" key={deposit.id}>
                  <div className="admin-top">
                    <div>
                      <h3>{deposit.userEmail}</h3>
                      <div className="meta">
                        <span>{formatMoney(deposit.amount)}</span>
                        <span>{formatDate(deposit.createdAt)}</span>
                      </div>
                    </div>
                    <span
                      className={`status ${deposit.status === "已退回" ? "green" : ""}`}
                    >
                      {deposit.status}
                    </span>
                  </div>
                  {deposit.status !== "已退回" && (
                    <div className="deposit-actions">
                      <button
                        className="button ghost small"
                        type="button"
                        onClick={async () => {
                          await appService.updateDeposit(deposit.id, "已联系");
                          notify("合作状态已更新。");
                        }}
                      >
                        标记已联系
                      </button>
                      <button
                        className="button primary small"
                        type="button"
                        onClick={async () => {
                          await appService.updateDeposit(deposit.id, "已退回");
                          notify("押金已退回用户余额。");
                        }}
                      >
                        退回押金
                      </button>
                    </div>
                  )}
                </article>
              ))
            ) : (
              <EmptyState title="暂无商务合作记录" />
            )}
          </div>
        </div>
      )}
      {view === "audit" && (
        <div className="admin-view active">
          <div className="settings-card">
            {data.auditLogs.length ? (
              data.auditLogs.map((log) => (
                <div className="audit-row" key={log.id}>
                  <b>{log.action}</b>
                  <p>
                    {log.detail}
                    <br />
                    {log.actor} · {formatDate(log.createdAt)}
                  </p>
                </div>
              ))
            ) : (
              <EmptyState title="暂无操作记录" />
            )}
          </div>
        </div>
      )}
    </section>
  );
}
