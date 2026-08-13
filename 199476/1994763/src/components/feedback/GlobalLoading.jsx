import { useSyncExternalStore } from 'react';
import {
  getPendingRequestCount,
  subscribeRequestActivity,
} from '../../api/requestActivity.js';
import './GlobalLoading.css';

export default function GlobalLoading() {
  const pendingCount = useSyncExternalStore(
    subscribeRequestActivity,
    getPendingRequestCount,
    getPendingRequestCount,
  );
  if (pendingCount === 0) return null;

  return (
    <div
      className="global-loading"
      role="status"
      aria-live="polite"
      aria-label="正在加载"
    >
      <div>
        <i />
        <b />
      </div>
    </div>
  );
}
