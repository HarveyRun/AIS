import { Check } from 'lucide-react';
import './Toast.css';

export default function Toast({ content }) {
  if (!content) return null;

  return (
    <div className="toast" role="status" aria-live="polite">
      <Check size={16} aria-hidden="true" />
      <span>{content}</span>
    </div>
  );
}
