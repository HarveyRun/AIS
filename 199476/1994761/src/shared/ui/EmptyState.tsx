import type { ReactNode } from "react";

interface EmptyStateProps {
  title: string;
  description?: string;
  action?: ReactNode;
}

export function EmptyState({ title, description, action }: EmptyStateProps) {
  return (
    <div className="empty">
      <b>{title}</b>
      {description && <span>{description}</span>}
      {action && <div>{action}</div>}
    </div>
  );
}
