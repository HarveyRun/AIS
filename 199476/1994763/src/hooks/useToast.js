import { useCallback, useEffect, useRef, useState } from 'react';

export default function useToast(duration = 1800) {
  const [toast, setToast] = useState('');
  const timerRef = useRef(null);

  const notify = useCallback(
    (message) => {
      window.clearTimeout(timerRef.current);
      setToast(message);
      timerRef.current = window.setTimeout(() => setToast(''), duration);
    },
    [duration],
  );

  useEffect(() => () => window.clearTimeout(timerRef.current), []);

  return { toast, notify };
}
