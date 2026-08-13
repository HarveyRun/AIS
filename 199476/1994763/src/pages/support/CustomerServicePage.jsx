import { useEffect, useRef, useState } from 'react';
import { LoaderCircle, Send } from 'lucide-react';
import Page from '../../components/layout/Page.jsx';
import './CustomerServicePage.css';
import { api } from '../../api/http.js';

function customerServiceMessage(item) {
  return {
    id: item.id,
    role: item.senderType === 'USER' ? 'user' : 'service',
    content: item.content,
    time: new Date(item.createdAt).toLocaleString(),
  };
}

export default function CustomerServicePage({
  go,
  notify,
  realtimeMessage,
  onRead = () => {},
}) {
  const [text, setText] = useState('');
  const [messages, setMessages] = useState([]);
  const [sending, setSending] = useState(false);
  const messagesEndRef = useRef(null);

  useEffect(() => {
    api.customerServiceMessages()
      .then((items) => {
        setMessages(items.map(customerServiceMessage));
        onRead();
      })
      .catch((error) => notify(error.message, 'error'));
  }, [notify, onRead]);

  useEffect(() => {
    if (!realtimeMessage?.id) return;
    const incoming = customerServiceMessage(realtimeMessage);
    setMessages((current) => (
      current.some((item) => item.id === incoming.id)
        ? current
        : [...current, incoming]
    ));
    onRead();
  }, [onRead, realtimeMessage]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ block: 'end' });
  }, [messages]);

  const send = async () => {
    const content = text.trim();
    if (!content || sending) return;
    try {
      setSending(true);
      setText('');
      const saved = await api.sendCustomerServiceMessage(content);
      setMessages((current) => [...current, customerServiceMessage(saved)]);
    } catch (error) {
      setText((current) => current || content);
      notify(error.message, 'error');
    } finally {
      setSending(false);
    }
  };

  return (
    <Page title="在线客服" back={() => go('profile')} className="customer-service-page">
      <section className="service-chat-list">
        {messages.map((message) => (
          <article className={message.role} key={message.id}>
            <div>{message.content}</div>
            <small>{message.time}</small>
          </article>
        ))}
        <div ref={messagesEndRef} />
      </section>
      <footer className="service-composer">
        <input
          value={text}
          onChange={(event) => setText(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter') send();
          }}
          placeholder="输入你想问的问题"
        />
        <button
          className={sending ? 'sending' : ''}
          type="button"
          disabled={!text.trim() || sending}
          onClick={send}
          aria-label={sending ? '正在发送' : '发送'}
        >
          {sending ? <LoaderCircle /> : <Send />}
        </button>
      </footer>
    </Page>
  );
}
