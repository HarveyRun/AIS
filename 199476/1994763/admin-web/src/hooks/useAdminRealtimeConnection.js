import { useEffect, useRef } from 'react';
import { adminApi } from '../api/adminApi.js';

function websocketUrl(ticket) {
  const socketUrl = new URL('/api/realtime/ws', window.location.origin);
  socketUrl.protocol = socketUrl.protocol === 'https:' ? 'wss:' : 'ws:';
  socketUrl.searchParams.set('ticket', ticket);
  return socketUrl.toString();
}

export default function useAdminRealtimeConnection(active, onEvent) {
  const eventHandlerRef = useRef(onEvent);

  useEffect(() => {
    eventHandlerRef.current = onEvent;
  }, [onEvent]);

  useEffect(() => {
    if (!active) return undefined;

    let disposed = false;
    let socket = null;
    let reconnectTimer = null;
    let retryCount = 0;

    const connect = async () => {
      try {
        const { ticket } = await adminApi.realtimeTicket();
        if (disposed) return;

        socket = new WebSocket(websocketUrl(ticket));
        socket.onopen = () => {
          retryCount = 0;
        };
        socket.onmessage = (message) => {
          try {
            eventHandlerRef.current?.(JSON.parse(message.data));
          } catch {
            // 忽略无法识别的数据，等待下一条有效消息。
          }
        };
        socket.onclose = () => {
          if (disposed) return;
          retryCount += 1;
          const delay = Math.min(15000, 1000 * (2 ** Math.min(retryCount - 1, 4)));
          reconnectTimer = window.setTimeout(connect, delay);
        };
      } catch {
        if (disposed) return;
        retryCount += 1;
        const delay = Math.min(15000, 1000 * (2 ** Math.min(retryCount - 1, 4)));
        reconnectTimer = window.setTimeout(connect, delay);
      }
    };

    connect();
    return () => {
      disposed = true;
      window.clearTimeout(reconnectTimer);
      socket?.close();
    };
  }, [active]);
}
