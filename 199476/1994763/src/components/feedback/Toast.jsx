import './Toast.css';

export default function Toast({ toast }) {
  if (!toast) return null;

  return (
    <div
      key={toast.id}
      className={`toast ${toast.type} ${toast.leaving ? 'leaving' : ''}`}
      role="status"
      aria-live="polite"
    >
      <span>{toast.content}</span>
    </div>
  );
}
