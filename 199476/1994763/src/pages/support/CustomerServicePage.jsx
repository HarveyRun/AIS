import { useEffect, useState } from 'react';
import { Send } from 'lucide-react';
import Page from '../../components/layout/Page.jsx';
import './CustomerServicePage.css';
import { api } from '../../api/http.js';

export default function CustomerServicePage({ go, notify }) {
  const [text, setText] = useState('');
  const [messages, setMessages] = useState([]);

  useEffect(() => {
    api.customerServiceMessages()
      .then((items) => {
        setMessages(items.map((item) => ({
          id: item.id,
          role: item.senderType === 'USER' ? 'user' : 'service',
          content: item.content,
          time: new Date(item.createdAt).toLocaleString(),
        })));
      })
      .catch((error) => notify(error.message, 'error'));
  }, [notify]);

  const send = async () => {
    const content = text.trim();
    if (!content) return;
    try {
      await api.sendCustomerServiceMessage(content);
      const latest = await api.customerServiceMessages();
      setMessages(latest.map((item) => ({
        id: item.id,
        role: item.senderType === 'USER' ? 'user' : 'service',
        content: item.content,
        time: new Date(item.createdAt).toLocaleString(),
      })));
      setText('');
    } catch (error) {
      notify(error.message, 'error');
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
        <button type="button" disabled={!text.trim()} onClick={send} aria-label="发送">
          <Send />
        </button>
      </footer>
    </Page>
  );
}
