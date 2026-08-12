import { useState } from 'react';
import { MessageCircleMore } from 'lucide-react';
import UserAvatar from '../../components/profile/UserAvatar.jsx';
import './MyInquiriesPage.css';

const statusLabels = {
  pending: {
    outgoing: '等待接受',
    incoming: '待你处理',
  },
  active: {
    outgoing: '交流中',
    incoming: '交流中',
  },
  awaiting_confirmation: {
    outgoing: '待你确认',
    incoming: '等待确认',
  },
  ended: {
    outgoing: '已结束',
    incoming: '已结束',
  },
  rejected: {
    outgoing: '未接受',
    incoming: '已拒绝',
  },
  cancelled: {
    outgoing: '已撤销',
    incoming: '已撤销',
  },
  expired: {
    outgoing: '已超时',
    incoming: '已超时',
  },
};

function formatLastMessageTime(value) {
  if (!value) return '尚未聊天';
  const date = new Date(value);
  const now = new Date();
  const time = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
  const isToday = date.getFullYear() === now.getFullYear()
    && date.getMonth() === now.getMonth()
    && date.getDate() === now.getDate();
  return isToday ? time : `${date.getMonth() + 1}/${date.getDate()} ${time}`;
}

export default function MyInquiriesPage({
  go,
  conversations,
  setConversations,
  setSelectedConversation,
}) {
  const [type, setType] = useState('outgoing');
  const visibleConversations = conversations.filter(
    (conversation) => conversation.direction === type,
  );
  const outgoingUnread = conversations.some(
    (conversation) => conversation.direction === 'outgoing' && conversation.unread > 0,
  );
  const incomingUnread = conversations.some(
    (conversation) => conversation.direction === 'incoming' && conversation.unread > 0,
  );

  const openConversation = (conversation) => {
    const openedConversation = {
      ...conversation,
      unread: 0,
      returnScreen: 'inquiries',
    };

    setConversations((current) =>
      current.map((item) => (item.id === conversation.id ? openedConversation : item)),
    );
    setSelectedConversation(openedConversation);
    go('directChat', 'inquiries');
  };

  return (
    <>
      <header className="topbar">
        <div>
          <h1>我的询问</h1>
        </div>
      </header>

      <div className="inquiry-tabs">
        <button className={type === 'outgoing' ? 'active' : ''} onClick={() => setType('outgoing')}>
          <span>我发起的</span>
          {outgoingUnread && <i aria-label="有未读消息" />}
        </button>
        <button className={type === 'incoming' ? 'active' : ''} onClick={() => setType('incoming')}>
          <span>我收到的</span>
          {incomingUnread && <i aria-label="有未读消息" />}
        </button>
      </div>

      <section className="inquiry-list">
        {visibleConversations.map((conversation) => {
          const label =
            statusLabels[conversation.inquiryStatus]?.[conversation.direction] || '查看交流';
          const partnerLabel =
            conversation.partner?.name?.trim() ||
            (conversation.partner?.uid ? `UID ${conversation.partner.uid}` : conversation.title);
          return (
            <button
              className={`inquiry-row ${conversation.inquiryStatus}`}
              type="button"
              key={conversation.id}
              onClick={() => openConversation(conversation)}
            >
              <UserAvatar
                src={conversation.partner?.avatar}
                uid={conversation.partner?.uid}
                name={partnerLabel}
                className="inquiry-avatar"
              />
              <div className="inquiry-row-content">
                <b>{partnerLabel}</b>
                <p>{conversation.question}</p>
                <small>
                  <strong>¥{Number(conversation.amount || 0).toFixed(2)}</strong>
                  <span>{label}</span>
                </small>
              </div>
              <div className="inquiry-row-meta">
                <time>{formatLastMessageTime(conversation.lastMessageAt)}</time>
                {conversation.unread > 0 && (
                  <em>{conversation.unread > 99 ? '99+' : conversation.unread}</em>
                )}
              </div>
            </button>
          );
        })}

        {visibleConversations.length === 0 && (
          <div className="inquiry-empty">
            <MessageCircleMore />
            <p>{type === 'outgoing' ? '还没有发起询问' : '还没有收到询问'}</p>
          </div>
        )}
      </section>
    </>
  );
}
