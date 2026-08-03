import { useEffect, useState } from "react";
import { FeedbackDialog } from "./FeedbackDialog";
import { useToast } from "../../shared/ui/Toast";
import { Icon } from "../../shared/ui/Icon";

export function GlobalFeedback() {
  const [open, setOpen] = useState(false);
  const [showBackTop, setShowBackTop] = useState(false);
  const notify = useToast();
  useEffect(() => {
    const update = () => setShowBackTop(window.scrollY > 520);
    update();
    window.addEventListener("scroll", update, { passive: true });
    return () => window.removeEventListener("scroll", update);
  }, []);
  return (
    <>
      <div
        className={`floating-tools ${showBackTop ? "has-back-top" : ""}`}
        aria-label="页面快捷操作"
      >
        <button
          className="feedback-launcher"
          type="button"
          aria-label="留言与反馈"
          onClick={() => setOpen(true)}
        >
          留言
        </button>
        {showBackTop && (
          <button
            className="floating-back-top"
            type="button"
            aria-label="返回顶部"
            title="返回顶部"
            onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })}
          >
            <Icon name="arrow-up" />
          </button>
        )}
      </div>
      <FeedbackDialog
        open={open}
        onClose={() => setOpen(false)}
        onSubmitted={notify}
      />
    </>
  );
}
