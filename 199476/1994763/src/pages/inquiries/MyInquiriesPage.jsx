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
};

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
          我发起的
        </button>
        <button className={type === 'incoming' ? 'active' : ''} onClick={() => setType('incoming')}>
          我收到的
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
                <time>{conversation.time || ''}</time>
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
