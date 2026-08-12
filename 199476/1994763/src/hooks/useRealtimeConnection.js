import { useEffect, useRef } from 'react';
import { api } from '../api/http.js';

function websocketUrl(ticket) {
  const configuredBase = import.meta.env.VITE_API_BASE_URL || '/api';
  const apiUrl = new URL(configuredBase, window.location.origin);
  const socketUrl = new URL(`${apiUrl.pathname.replace(/\/$/, '')}/realtime/ws`, apiUrl.origin);
  socketUrl.protocol = socketUrl.protocol === 'https:' ? 'wss:' : 'ws:';
  socketUrl.searchParams.set('ticket', ticket);
  return socketUrl.toString();
}

export default function useRealtimeConnection(active, onEvent) {
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
        const { ticket } = await api.realtimeTicket();
        if (disposed) return;

        socket = new WebSocket(websocketUrl(ticket));
        socket.onopen = () => {
          retryCount = 0;
        };
        socket.onmessage = (message) => {
          try {
            eventHandlerRef.current?.(JSON.parse(message.data));
          } catch {
            // 忽略无法识别的长连接数据，等待下一条有效事件。
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
