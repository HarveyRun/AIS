import { useCallback, useEffect, useRef, useState } from 'react';

export default function useToast(duration = 2200) {
  const [toast, setToast] = useState(null);
  const hideTimerRef = useRef(null);
  const removeTimerRef = useRef(null);

  const notify = useCallback((content, type = 'default') => {
    const normalizedContent = String(content || '').trim();
    if (!normalizedContent) return;

    window.clearTimeout(hideTimerRef.current);
    window.clearTimeout(removeTimerRef.current);

    const nextToast = {
      id: `${Date.now()}-${Math.random()}`,
      content: normalizedContent,
      type: ['success', 'warning', 'error'].includes(type) ? type : 'default',
      leaving: false,
    };
    setToast(nextToast);

    hideTimerRef.current = window.setTimeout(() => {
      setToast((current) => (
        current?.id === nextToast.id ? { ...current, leaving: true } : current
      ));
    }, Math.max(0, duration - 240));

    removeTimerRef.current = window.setTimeout(() => {
      setToast((current) => (current?.id === nextToast.id ? null : current));
    }, duration);
  }, [duration]);

  useEffect(() => () => {
    window.clearTimeout(hideTimerRef.current);
    window.clearTimeout(removeTimerRef.current);
  }, []);

  return { toast, notify };
}
