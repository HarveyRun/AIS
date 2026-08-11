import { useEffect, useState } from 'react';
import { Send } from 'lucide-react';
import { adminApi } from '../../api/adminApi.js';
import { date } from '../users/UsersPage.jsx';
import './CustomerServicePage.css';

export default function CustomerServicePage() {
  const [conversations, setConversations] = useState([]);
  const [selected, setSelected] = useState(null);
  const [messages, setMessages] = useState([]);
  const [text, setText] = useState('');
  const [error, setError] = useState('');
  const [sending, setSending] = useState(false);

  const loadConversations = () =>
    adminApi
      .customerServiceConversations()
      .then(setConversations)
      .catch((e) => setError(e.message));
  useEffect(() => {
    loadConversations();
  }, []);

  const open = async (conversation) => {
    try {
      setError('');
      setSelected(conversation);
      setMessages(await adminApi.customerServiceMessages(conversation.userId));
      loadConversations();
    } catch (e) {
      setError(e.message);
    }
  };

  const reply = async () => {
    if (!text.trim()) return;
    try {
      setSending(true);
      setError('');
      await adminApi.replyCustomerService(selected.userId, text.trim());
      setText('');
      setMessages(await adminApi.customerServiceMessages(selected.userId));
      loadConversations();
    } catch (e) {
      setError(e.message);
    } finally {
      setSending(false);
    }
  };

  return (
    <>
      <div className="page-title">
        <div>
          <h1>在线客服</h1>
          <p>查看用户留言并直接回复</p>
        </div>
      </div>
      {error && <div className="error-box">{error}</div>}
      <section className="service-workbench">
        <aside>
          {conversations.map((item) => (
            <button
              className={selected?.userId === item.userId ? 'active' : ''}
              key={item.userId}
              onClick={() => open(item)}
            >
              <div>
                <b>{item.nickname || `UID ${item.uid}`}</b>
                {Number(item.unread) > 0 && <em>{item.unread}</em>}
              </div>
              <p>{item.lastMessage}</p>
              <small>{date(item.lastMessageAt)}</small>
            </button>
          ))}
          {!conversations.length && <div className="service-empty">暂无客服消息</div>}
        </aside>
        <main>
          {!selected ? (
            <div className="service-empty">请选择一位用户</div>
          ) : (
            <>
              <header>
                <b>{selected.nickname || `UID ${selected.uid}`}</b>
                <span>UID {selected.uid}</span>
              </header>
              <div className="service-messages">
                {messages.map((message) => (
                  <article
                    className={message.senderType === 'SERVICE' ? 'service' : 'user'}
                    key={message.id}
                  >
                    <p>{message.content}</p>
                    <small>{date(message.createdAt)}</small>
                  </article>
                ))}
              </div>
              <footer>
                <textarea
                  value={text}
                  onChange={(event) => setText(event.target.value)}
                  placeholder="输入回复内容"
                />
                <button disabled={!text.trim() || sending} onClick={reply}>
                  <Send />
                  {sending ? '发送中' : '发送'}
                </button>
              </footer>
            </>
          )}
        </main>
      </section>
    </>
  );
}
