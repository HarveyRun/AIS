import { useCallback, useEffect, useRef, useState } from 'react';

export default function useToast(duration = 1800) {
  const [toast, setToast] = useState('');
  const timerRef = useRef(null);

  const notify = useCallback((content) => {
    const normalizedContent = String(content || '').trim();
    if (!normalizedContent) return;

    window.clearTimeout(timerRef.current);
    setToast(normalizedContent);
    timerRef.current = window.setTimeout(() => setToast(''), duration);
  }, [duration]);

  useEffect(() => () => window.clearTimeout(timerRef.current), []);

  return { toast, notify };
}
