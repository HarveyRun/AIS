import { useState } from 'react';
import { Send } from 'lucide-react';
import Page from '../../components/layout/Page.jsx';
import './CustomerServicePage.css';

export default function CustomerServicePage({ go }) {
  const [text, setText] = useState('');
  const [messages, setMessages] = useState([
    {
      id: 'welcome',
      role: 'service',
      content: '你好，我是事先问客服。请告诉我你遇到了什么问题。',
      time: '现在',
    },
  ]);

  const send = () => {
    const content = text.trim();
    if (!content) return;

    setMessages((current) => [
      ...current,
      {
        id: `user-${Date.now()}`,
        role: 'user',
        content,
        time: '现在',
      },
      {
        id: `service-${Date.now()}`,
        role: 'service',
        content: '已经收到，我们会尽快查看并回复你。',
        time: '现在',
      },
    ]);
    setText('');
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
