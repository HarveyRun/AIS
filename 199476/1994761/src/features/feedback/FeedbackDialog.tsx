import { useEffect, useState } from "react";
import { appService } from "../../application/appService";
import { legacyPageName } from "../../application/navigation";
import { FEEDBACK_CATEGORIES } from "../../domain/constants";
import type { FeedbackCategory } from "../../domain/types";
import { useAppStore } from "../../application/store";
import { errorMessage, useAsyncAction } from "../../shared/lib/useAsyncAction";

interface FeedbackDialogProps {
  open: boolean;
  onClose: () => void;
  onSubmitted: (message: string) => void;
}

export function FeedbackDialog({
  open,
  onClose,
  onSubmitted,
}: FeedbackDialogProps) {
  const { user, sessionEmail } = useAppStore();
  const [category, setCategory] = useState<FeedbackCategory>("建议");
  const [content, setContent] = useState("");
  const [error, setError] = useState("");
  const { isPending, run } = useAsyncAction();

  useEffect(() => {
    if (!open) return;
    const handleKey = (event: KeyboardEvent) => {
      if (event.key === "Escape" && !isPending) onClose();
    };
    document.addEventListener("keydown", handleKey);
    return () => document.removeEventListener("keydown", handleKey);
  }, [isPending, onClose, open]);

  if (!open) return null;
  const close = () => {
    setContent("");
    setError("");
    onClose();
  };
  return (
    <div
      className="feedback-modal open"
      role="dialog"
      aria-modal="true"
      aria-labelledby="feedback-title"
      onMouseDown={(event) => {
        if (event.currentTarget === event.target && !isPending) close();
      }}
    >
      <form
        className="feedback-card"
        onSubmit={async (event) => {
          event.preventDefault();
          const value = content.trim();
          if (value.length < 5) {
            setError("请至少输入 5 个字。");
            return;
          }
          try {
            await run(() =>
              appService.createFeedback(
                user ? sessionEmail : null,
                value,
                legacyPageName(),
                category,
              ),
            );
            close();
            onSubmitted("反馈已提交，感谢你的建议。");
          } catch (reason) {
            setError(errorMessage(reason, "提交失败，请稍后重试。"));
          }
        }}
      >
        <h2 id="feedback-title">留言</h2>
        <p>告诉我们您遇到的问题，或留下改进建议。</p>
        <label className="feedback-label" htmlFor="feedback-category">
          反馈类型
        </label>
        <select
          id="feedback-category"
          className="feedback-category"
          value={category}
          onChange={(event) =>
            setCategory(event.target.value as FeedbackCategory)
          }
        >
          {FEEDBACK_CATEGORIES.map((item) => (
            <option key={item}>{item}</option>
          ))}
        </select>
        <label className="feedback-label" htmlFor="feedback-content">
          反馈内容
        </label>
        <textarea
          id="feedback-content"
          maxLength={500}
          required
          placeholder="请描述问题或建议（5—500 字）"
          value={content}
          onChange={(event) => setContent(event.target.value)}
          autoFocus
        />
        <div className="feedback-count">{content.length} / 500</div>
        <div className="feedback-error">{error}</div>
        <div className="feedback-actions">
          <button
            className="feedback-cancel"
            type="button"
            onClick={close}
            disabled={isPending}
          >
            取消
          </button>
          <button type="submit" disabled={isPending}>
            {isPending ? "提交中…" : "提交反馈"}
          </button>
        </div>
      </form>
    </div>
  );
}
