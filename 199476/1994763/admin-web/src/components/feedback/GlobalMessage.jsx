import { useEffect, useRef, useState } from 'react';
import { CheckCircle2, CircleAlert, Info, TriangleAlert, X } from 'lucide-react';
import { EVENT_NAME } from './message.js';
import './GlobalMessage.css';

const icons = { success: CheckCircle2, warning: TriangleAlert, error: CircleAlert, default: Info };

export default function GlobalMessage() {
  const [items, setItems] = useState([]);
  const sequence = useRef(0);
  const timers = useRef(new Map());
  const remove = (id) => {
    window.clearTimeout(timers.current.get(id));
    timers.current.delete(id);
    setItems((current) => current.filter((item) => item.id !== id));
  };
  useEffect(() => {
    const receive = (event) => {
      const id = ++sequence.current;
      const item = { id, type: event.detail?.type || 'default', content: event.detail?.content || '' };
      setItems((current) => [...current.slice(-3), item]);
      timers.current.set(id, window.setTimeout(() => remove(id), event.detail?.duration || 3000));
    };
    window.addEventListener(EVENT_NAME, receive);
    return () => {
      window.removeEventListener(EVENT_NAME, receive);
      timers.current.forEach((timer) => window.clearTimeout(timer));
      timers.current.clear();
    };
  }, []);
  return (
    <div className="global-message-host" aria-live="polite">
      {items.map((item) => {
        const Icon = icons[item.type] || Info;
        return <div className={`global-message ${item.type}`} role="status" key={item.id}><Icon /><span>{item.content}</span><button type="button" aria-label="关闭提示" onClick={() => remove(item.id)}><X /></button></div>;
      })}
    </div>
  );
}
