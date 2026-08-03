import { useEffect, useRef, useState, type ReactNode } from "react";
import { errorMessage } from "../lib/useAsyncAction";

interface ConfirmModalProps {
  open: boolean;
  title: string;
  children: ReactNode;
  confirmLabel: string;
  onCancel: () => void;
  onConfirm: () => void | Promise<void>;
}

export function ConfirmModal({
  open,
  title,
  children,
  confirmLabel,
  onCancel,
  onConfirm,
}: ConfirmModalProps) {
  const confirmRef = useRef<HTMLButtonElement>(null);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const cancel = () => {
    setError("");
    onCancel();
  };
  useEffect(() => {
    if (!open) return;
    confirmRef.current?.focus();
    const handleKey = (event: KeyboardEvent) => {
      if (event.key === "Escape" && !pending) {
        setError("");
        onCancel();
      }
    };
    document.addEventListener("keydown", handleKey);
    return () => document.removeEventListener("keydown", handleKey);
  }, [onCancel, open, pending]);
  if (!open) return null;
  return (
    <div
      className="purchase-modal"
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-title"
      onMouseDown={(event) => {
        if (event.currentTarget === event.target) cancel();
      }}
    >
      <div className="purchase-dialog" aria-busy={pending}>
        <h2 id="confirm-title">{title}</h2>
        {children}
        <div className="purchase-actions">
          <button
            className="button ghost"
            type="button"
            onClick={cancel}
            disabled={pending}
          >
            取消
          </button>
          <button
            ref={confirmRef}
            className="button primary"
            type="button"
            disabled={pending}
            onClick={async () => {
              setPending(true);
              setError("");
              try {
                await onConfirm();
              } catch (reason) {
                setError(errorMessage(reason));
              } finally {
                setPending(false);
              }
            }}
          >
            {pending ? "处理中…" : confirmLabel}
          </button>
        </div>
        {error && (
          <div className="form-error" role="alert">
            {error}
          </div>
        )}
      </div>
    </div>
  );
}
