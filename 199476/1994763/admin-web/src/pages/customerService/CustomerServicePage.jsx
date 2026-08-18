import { useCallback, useEffect, useRef, useState } from 'react';
import { Plus, Search, Send, UserRound, X } from 'lucide-react';
import { adminApi } from '../../api/adminApi.js';
import { date } from '../users/UsersPage.jsx';
import './CustomerServicePage.css';
import { message } from '../../components/feedback/message.js';

export default function CustomerServicePage({ realtimeEvent, onUnreadChange }) {
  const [conversations, setConversations] = useState([]);
  const [selected, setSelected] = useState(null);
  const [messages, setMessages] = useState([]);
  const [text, setText] = useState('');
  const [sending, setSending] = useState(false);
  const [userPickerOpen, setUserPickerOpen] = useState(false);
  const [userKeyword, setUserKeyword] = useState('');
  const [userResults, setUserResults] = useState([]);
  const messagesEndRef = useRef(null);
  const handledRealtimeMessageRef = useRef(realtimeEvent?.message?.id || null);

  const loadConversations = useCallback(
    (silent = false) =>
      adminApi
        .customerServiceConversations({ silent })
        .then(setConversations)
        .catch((error) => {
          if (!silent) message.error(error.message);
        }),
    [],
  );

  useEffect(() => {
    loadConversations();
  }, [loadConversations]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ block: 'end' });
  }, [messages]);

  useEffect(() => {
    onUnreadChange(conversations.reduce((total, item) => total + Number(item.unread || 0), 0));
  }, [conversations, onUnreadChange]);

  useEffect(() => {
    if (!realtimeEvent?.message) return;
    if (handledRealtimeMessageRef.current === realtimeEvent.message.id) return;
    handledRealtimeMessageRef.current = realtimeEvent.message.id;
    const payload = realtimeEvent;
    const incoming = payload.message;
    const isOpen = selected?.userId === payload.userId;

    if (isOpen) {
      setMessages((current) =>
        current.some((item) => item.id === incoming.id) ? current : [...current, incoming],
      );
      adminApi.readCustomerServiceMessages(payload.userId).catch(() => {});
    }

    setConversations((current) => {
      const previous = current.find((item) => item.userId === payload.userId);
      const updated = {
        ...previous,
        userId: payload.userId,
        uid: payload.uid,
        nickname: payload.nickname,
        avatarUrl: payload.avatarUrl,
        lastMessage: incoming.content,
        lastMessageAt: incoming.createdAt,
        unread: isOpen ? 0 : Number(previous?.unread || 0) + 1,
      };
      return [updated, ...current.filter((item) => item.userId !== payload.userId)];
    });
  }, [realtimeEvent, selected?.userId]);

  const open = async (conversation) => {
    try {
      setSelected(conversation);
      setMessages(await adminApi.customerServiceMessages(conversation.userId));
      setConversations((current) => {
        const existing = current.find((item) => item.userId === conversation.userId);
        const updated = { ...conversation, ...existing, unread: 0 };
        return [updated, ...current.filter((item) => item.userId !== conversation.userId)];
      });
    } catch (e) {
      message.error(e.message);
    }
  };

  const searchUsers = async (keyword = userKeyword) => {
    try {
      const query = new URLSearchParams({
        keyword: keyword.trim(),
        status: '',
        page: 0,
        size: 20,
      });
      const result = await adminApi.users(query.toString());
      setUserResults(result.items.filter((user) => user.accountStatus !== 'DELETED'));
    } catch (error) {
      message.error(error.message);
    }
  };

  const showUserPicker = () => {
    setUserKeyword('');
    setUserPickerOpen(true);
    searchUsers('');
  };

  const selectUser = async (user) => {
    setUserPickerOpen(false);
    await open({
      userId: user.id,
      uid: user.uid,
      nickname: user.nickname,
      avatarUrl: user.avatarUrl,
      lastMessage: '',
      lastMessageAt: null,
      unread: 0,
    });
  };

  const reply = async () => {
    if (!text.trim()) return;
    try {
      setSending(true);
      const saved = await adminApi.replyCustomerService(selected.userId, text.trim());
      setText('');
      setMessages((current) => [...current, saved]);
      setConversations((current) => {
        const existing = current.find((item) => item.userId === selected.userId) || selected;
        const updated = {
          ...existing,
          lastMessage: saved.content,
          lastMessageAt: saved.createdAt,
          unread: 0,
        };
        return [updated, ...current.filter((item) => item.userId !== selected.userId)];
      });
      message.success('回复已发送');
    } catch (e) {
      message.error(e.message);
    } finally {
      setSending(false);
    }
  };

  return (
    <>
      <div className="page-title">
        <div>
          <h1>在线客服</h1>
          <p>接收用户消息，也可以主动联系用户</p>
        </div>
        <button className="service-new-conversation" type="button" onClick={showUserPicker}>
          <Plus />
          选择用户
        </button>
      </div>
      <section className="service-workbench">
        <div className="service-conversation-list">
          <header className="service-conversation-list-header">
            <b>最近联系</b>
            <span>{conversations.length} 人</span>
          </header>
          {conversations.map((item) => (
            <button
              className={selected?.userId === item.userId ? 'active' : ''}
              key={item.userId}
              onClick={() => open(item)}
            >
              <i className="service-person-avatar">
                {item.avatarUrl ? <img src={item.avatarUrl} alt="" /> : <UserRound />}
              </i>
              <div className="service-person-content">
                <div className="service-person-heading">
                  <b>{item.nickname || `UID ${item.uid}`}</b>
                  <small>{date(item.lastMessageAt)}</small>
                </div>
                <p>{item.lastMessage || '暂无消息'}</p>
              </div>
              {Number(item.unread) > 0 && <em>{item.unread}</em>}
            </button>
          ))}
          {!conversations.length && <div className="service-empty">暂无客服消息</div>}
        </div>
        <div className="service-chat-panel">
          <header>
            {selected ? (
              <>
                <b>{selected.nickname || `UID ${selected.uid}`}</b>
                <span>UID {selected.uid}</span>
              </>
            ) : (
              <b>请选择一位用户</b>
            )}
          </header>
          <div className="service-messages">
            {!selected ? (
              ''
            ) : (
              <>
                {messages.map((message) => (
                  <article
                    className={message.senderType === 'SERVICE' ? 'service' : 'user'}
                    key={message.id}
                  >
                    {message.messageType === 'IMAGE' ? (
                      <a
                        className="service-image"
                        href={message.attachmentUrl}
                        target="_blank"
                        rel="noreferrer"
                      >
                        <img src={message.attachmentUrl} alt={message.attachmentName || '客服图片'} />
                      </a>
                    ) : (
                      <p>{message.content}</p>
                    )}
                    <small>{date(message.createdAt)}</small>
                  </article>
                ))}
              </>
            )}
            <div ref={messagesEndRef} />
          </div>
          <footer>
            <textarea
              disabled={!selected}
              value={text}
              onChange={(event) => setText(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter' && !event.shiftKey) {
                  event.preventDefault();
                  reply();
                }
              }}
              placeholder={selected ? '输入消息，按 Enter 发送' : '请先选择一位用户'}
            />
            <button type="button" disabled={!selected || !text.trim() || sending} onClick={reply}>
              <Send />
              {sending ? '发送中' : '发送'}
            </button>
          </footer>
        </div>
      </section>

      {userPickerOpen && (
        <div className="service-user-picker-mask" role="presentation">
          <section className="service-user-picker" role="dialog" aria-modal="true">
            <header>
              <div>
                <h2>选择用户</h2>
                <p>可以按 UID、手机号或昵称搜索</p>
              </div>
              <button type="button" onClick={() => setUserPickerOpen(false)} aria-label="关闭">
                <X />
              </button>
            </header>
            <div className="service-user-search">
              <Search />
              <input
                autoFocus
                value={userKeyword}
                onChange={(event) => setUserKeyword(event.target.value)}
                onKeyDown={(event) => event.key === 'Enter' && searchUsers()}
                placeholder="输入 UID、手机号或昵称"
              />
              <button type="button" onClick={() => searchUsers()}>
                搜索
              </button>
            </div>
            <div className="service-user-results">
              {userResults.map((user) => (
                <button type="button" key={user.id} onClick={() => selectUser(user)}>
                  <span>
                    <b>{user.nickname || `UID ${user.uid}`}</b>
                    <small>
                      UID {user.uid} · {user.phone}
                    </small>
                  </span>
                  <em>选择</em>
                </button>
              ))}
              {!userResults.length && <div className="service-empty">没有找到用户</div>}
            </div>
          </section>
        </div>
      )}
    </>
  );
}
