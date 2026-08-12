import { useEffect, useState, useSyncExternalStore } from 'react';
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
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    if (pendingCount === 0) {
      setVisible(false);
      return undefined;
    }

    const timer = window.setTimeout(() => setVisible(true), 100);
    return () => window.clearTimeout(timer);
  }, [pendingCount]);

  if (!visible || pendingCount === 0) return null;

  return (
    <div className="admin-global-loading" role="status" aria-live="polite" aria-label="正在加载">
      <div>
        <i />
        <span>加载中</span>
      </div>
    </div>
  );
}
