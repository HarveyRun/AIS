import { AlertTriangle, X } from 'lucide-react';
import './ConfirmDialog.css';

export default function ConfirmDialog({ open, title, message, confirmText = '确认', danger = false, busy = false, onConfirm, onCancel }) {
  if (!open) return null;
  return (
    <div className="confirm-dialog-layer" role="presentation">
      <button className="confirm-dialog-mask" type="button" aria-label="取消" onClick={busy ? undefined : onCancel} />
      <section className="confirm-dialog" role="alertdialog" aria-modal="true" aria-labelledby="confirm-dialog-title" aria-describedby="confirm-dialog-message">
        <header>
          <i><AlertTriangle /></i>
          <button type="button" aria-label="关闭" disabled={busy} onClick={onCancel}><X /></button>
        </header>
        <h2 id="confirm-dialog-title">{title}</h2>
        <p id="confirm-dialog-message">{message}</p>
        <footer>
          <button className="plain" type="button" disabled={busy} onClick={onCancel}>取消</button>
          <button className={danger ? 'danger-confirm' : 'primary'} type="button" disabled={busy} onClick={onConfirm}>{busy ? '处理中…' : confirmText}</button>
        </footer>
      </section>
    </div>
  );
}
