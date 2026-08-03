import { useMemo, useState } from "react";
import { appService } from "../../application/appService";
import { useAppStore } from "../../application/store";
import type { Feedback, Notification } from "../../domain/types";
import { formatDate } from "../../shared/lib/format";
import { EmptyState } from "../../shared/ui/EmptyState";
import { useToast } from "../../shared/ui/Toast";

type Filter = "all" | "unread" | "feedback" | "system";
type Entry =
  | { kind: "notice"; time: string; value: Notification }
  | { kind: "thread"; time: string; value: Feedback; unread: boolean };

const typeLabels: Record<string, string> = {
  idea: "想法进度",
  payment: "付款提醒",
  package: "套餐提醒",
  feedback: "反馈回复",
  cooperation: "商务合作",
};

function Thread({ feedback, unread }: { feedback: Feedback; unread: boolean }) {
  const { sessionEmail } = useAppStore();
  const notify = useToast();
  const [reply, setReply] = useState("");
  const last = feedback.messages.at(-1);
  const canReply = feedback.status !== "已结束" && last?.role === "admin";
  return (
    <article
      className={`thread-card ${unread ? "unread" : ""}`}
      onClick={async (event) => {
        if (
          !(event.target as HTMLElement).closest(
            "button,textarea,select,input,a",
          )
        )
          await appService.markBusinessNotifications(sessionEmail, feedback.id);
      }}
    >
      <div className="thread-top">
        <div>
          <span className="thread-category">{feedback.category}</span>
          <h3>{feedback.messages[0]?.content || "反馈"}</h3>
        </div>
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
            <b>{message.role === "admin" ? "管理员" : "我"}</b>
            <p>{message.content}</p>
            <small>{formatDate(message.createdAt)}</small>
          </div>
        ))}
      </div>
      {canReply ? (
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
                "user",
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
            placeholder="回复管理员"
            value={reply}
            onChange={(event) => setReply(event.target.value)}
          />
          <div className="thread-reply-actions">
            <button className="button primary small" type="submit">
              发送回复
            </button>
          </div>
        </form>
      ) : (
        <div className="thread-waiting">
          {feedback.status === "已结束"
            ? "本次反馈已处理完成。"
            : "等待管理员回复。"}
        </div>
      )}
    </article>
  );
}

export function NotificationsPanel({
  onNavigate,
}: {
  onNavigate: (tab: string) => void;
}) {
  const { data, sessionEmail, isAdmin } = useAppStore();
  const [filter, setFilter] = useState<Filter>("all");
  const [visible, setVisible] = useState(12);
  const notices = useMemo(
    () =>
      data.notifications
        .filter((item) => item.userEmail === sessionEmail)
        .sort(
          (a, b) =>
            new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
        ),
    [data.notifications, sessionEmail],
  );
  const feedbacks = useMemo(
    () =>
      data.feedbacks
        .filter((item) => item.userEmail === sessionEmail)
        .sort(
          (a, b) =>
            new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime(),
        ),
    [data.feedbacks, sessionEmail],
  );
  const unreadFeedbackIds = useMemo(
    () =>
      new Set(
        notices
          .filter(
            (item) => !item.read && item.type === "feedback" && item.businessId,
          )
          .map((item) => item.businessId!),
      ),
    [notices],
  );
  const entries = useMemo<Entry[]>(() => {
    const system = notices
      .filter(
        (item) =>
          (isAdmin || item.type !== "feedback") &&
          (filter === "all" ||
            (filter === "unread" && !item.read) ||
            (filter === "feedback" && item.type === "feedback") ||
            (filter === "system" && item.type !== "feedback")),
      )
      .map((value) => ({
        kind: "notice" as const,
        time: value.createdAt,
        value,
      }));
    const threads = feedbacks
      .filter(
        (item) =>
          filter === "all" ||
          filter === "feedback" ||
          (filter === "unread" && unreadFeedbackIds.has(item.id)),
      )
      .map((value) => ({
        kind: "thread" as const,
        time: value.updatedAt,
        value,
        unread: unreadFeedbackIds.has(value.id),
      }));
    return [...system, ...threads].sort(
      (a, b) => new Date(b.time).getTime() - new Date(a.time).getTime(),
    );
  }, [feedbacks, filter, isAdmin, notices, unreadFeedbackIds]);
  const unreadCount = notices.filter((item) => !item.read).length;
  const feedbackCount =
    feedbacks.length +
    (isAdmin ? notices.filter((item) => item.type === "feedback").length : 0);
  const systemCount = notices.filter((item) => item.type !== "feedback").length;
  const counts: Record<Filter, number> = {
    all: feedbackCount + systemCount,
    unread: unreadCount,
    feedback: feedbackCount,
    system: systemCount,
  };
  const labels: Record<Filter, string> = {
    all: "全部",
    unread: "未读",
    feedback: "反馈",
    system: "业务消息",
  };
  return (
    <section className="panel active">
      <p className="panel-lead">消息中心</p>
      <div className="section-head">
        <h2>消息通知</h2>
        <span className="notification-summary">{unreadCount} 条未读</span>
      </div>
      <div className="notification-toolbar">
        {(["all", "unread", "feedback", "system"] as Filter[]).map((value) => (
          <button
            className={filter === value ? "active" : ""}
            type="button"
            key={value}
            onClick={() => {
              setFilter(value);
              setVisible(12);
            }}
          >
            {labels[value]} <b>{counts[value]}</b>
          </button>
        ))}
        <button
          className="mark-all"
          type="button"
          disabled={!unreadCount}
          onClick={() => void appService.markAllNotifications(sessionEmail)}
        >
          全部标为已读
        </button>
      </div>
      <div className="notification-list">
        {entries.length ? (
          entries.slice(0, visible).map((entry) =>
            entry.kind === "notice" ? (
              <article
                className={`notification-card ${entry.value.read ? "" : "unread"}`}
                role="button"
                tabIndex={0}
                key={entry.value.id}
                onKeyDown={(event) => {
                  if (event.key === "Enter" || event.key === " ") {
                    event.preventDefault();
                    event.currentTarget.click();
                  }
                }}
                onClick={async () => {
                  await appService.markNotification(
                    sessionEmail,
                    entry.value.id,
                  );
                  const link = entry.value.link;
                  if (link.includes("#packages") || link.includes("/packages"))
                    onNavigate("packages");
                  else if (link.includes("#ideas") || link.includes("/ideas"))
                    onNavigate("ideas");
                  else if (
                    (link.includes("#admin") || link.includes("/admin")) &&
                    isAdmin
                  )
                    onNavigate("admin");
                }}
              >
                <div className="notification-top">
                  <div>
                    <span className="notification-type">
                      {typeLabels[entry.value.type] || "系统消息"}
                    </span>
                    <h3>{entry.value.title}</h3>
                  </div>
                  <span className="notification-time">
                    {formatDate(entry.value.createdAt)}
                  </span>
                </div>
                <p>{entry.value.content}</p>
                <small className="notification-state">
                  {entry.value.read ? "已读" : "未读"}
                </small>
              </article>
            ) : (
              <Thread
                feedback={entry.value}
                unread={entry.unread}
                key={entry.value.id}
              />
            ),
          )
        ) : (
          <EmptyState
            title="暂时没有消息"
            description="新的进度和回复会显示在这里。"
          />
        )}
      </div>
      {entries.length > visible && (
        <button
          className="notification-more"
          type="button"
          onClick={() => setVisible((value) => value + 12)}
        >
          再显示 {Math.min(12, entries.length - visible)} 条
        </button>
      )}
    </section>
  );
}
