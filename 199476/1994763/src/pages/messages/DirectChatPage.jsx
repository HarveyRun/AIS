import { Fragment, useEffect, useRef, useState } from 'react';
import {
  ArrowLeft,
  CheckCircle2,
  Clock3,
  MessageCircleMore,
  ImagePlus,
  Keyboard,
  Plus,
  Smile,
  WalletCards,
  XCircle,
} from 'lucide-react';
import UserAvatar from '../../components/profile/UserAvatar.jsx';
import './MessagePages.css';
import { api } from '../../api/http.js';

const EMOJIS = ['😀', '😄', '😂', '😊', '🥰', '😎', '🤔', '👍', '👏', '🙏', '💪', '🎉', '❤️', '👌', '🌹', '🤝'];

function formatDeadline(value) {
  if (!value) return '';
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

function messageFromApi(message, currentUserId) {
  return {
    id: message.id,
    name: message.senderName,
    text: message.content,
    avatar: message.senderAvatar,
    me: message.senderId === currentUserId,
    status: 'sent',
    createdAt: message.createdAt,
    time: new Date(message.createdAt).toLocaleTimeString([], {
      hour: '2-digit',
      minute: '2-digit',
    }),
    attachment: message.attachmentUrl ? {
      url: message.attachmentUrl,
      name: message.attachmentName || '照片',
      type: message.type === 'IMAGE' ? 'image/*' : '',
    } : null,
  };
}

function formatMessageTime(value) {
  if (!value) return '';
  const date = new Date(value);
  const now = new Date();
  const time = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
  const isToday = date.getFullYear() === now.getFullYear()
    && date.getMonth() === now.getMonth()
    && date.getDate() === now.getDate();
  return isToday ? time : `${date.getMonth() + 1}月${date.getDate()}日 ${time}`;
}

function shouldShowMessageTime(messages, index) {
  const currentTime = messages[index]?.createdAt;
  if (!currentTime) return false;
  if (index === 0) return true;
  const previousTime = messages[index - 1]?.createdAt;
  if (!previousTime) return true;
  return new Date(currentTime).getTime() - new Date(previousTime).getTime() >= 10 * 60 * 1000;
}

export default function DirectChatPage({
  go,
  conversation,
  setConversations,
  setSelectedConversation,
  currentUser = { name: '', uid: '', avatar: '' },
  refreshWallet,
  notify,
}) {
  const partner = conversation?.partner || {
    name: conversation?.title || '',
    uid: conversation?.uid || '',
    role: '过来人',
    color: conversation?.color || '#c27b62',
  };
  const partnerLabel = partner.name?.trim() || (partner.uid ? `UID ${partner.uid}` : '对方');
  const inquiryStatus = conversation?.inquiryStatus || 'active';
  const isIncoming = conversation?.direction === 'incoming';
  const canChat = inquiryStatus === 'active';
  const [text, setText] = useState('');
  const [messages, setMessages] = useState(conversation?.messages || []);
  const [endConfirmOpen, setEndConfirmOpen] = useState(false);
  const [cancelConfirmOpen, setCancelConfirmOpen] = useState(false);
  const [emojiOpen, setEmojiOpen] = useState(false);
  const [moreOpen, setMoreOpen] = useState(false);
  const imageInputRef = useRef(null);
  const textInputRef = useRef(null);
  const messagesEndRef = useRef(null);

  const applyInquiryDetail = (detail) => {
    const item = detail.inquiry;
    const loadedMessages = detail.messages.map((message) => messageFromApi(message, currentUser.id));
    const normalizedStatus = item.status === 'COMPLETED' ? 'ended' : item.status.toLowerCase();
    const lastMessageTime = item.lastMessageAt
      ? new Date(item.lastMessageAt).toLocaleString('zh-CN', {
          month: 'numeric',
          day: 'numeric',
          hour: '2-digit',
          minute: '2-digit',
        })
      : '尚未聊天';
    const updatedConversation = {
      ...conversation,
      amount: Number(item.amount),
      inquiryStatus: normalizedStatus,
      settlementStatus: item.fundsStatus === 'SETTLED' ? 'settled' : item.fundsStatus.toLowerCase(),
      responseDeadline: item.responseDeadline,
      confirmationDeadline: item.confirmationDeadline,
      lastMessageAt: item.lastMessageAt,
      time: lastMessageTime,
      messages: loadedMessages,
    };
    setMessages(loadedMessages);
    setConversations((current) =>
      current.map((entry) => (entry.id === updatedConversation.id ? updatedConversation : entry)),
    );
    setSelectedConversation(updatedConversation);
    return updatedConversation;
  };

  const refreshInquiry = async () => applyInquiryDetail(await api.inquiry(conversation.id));

  const appendPendingMessage = (message) => {
    setMessages((current) => [...current, message]);
    setConversations((current) => current.map((item) => (
      item.id === conversation.id
        ? { ...item, messages: [...(item.messages || []), message] }
        : item
    )));
    setSelectedConversation((current) => (
      current?.id === conversation.id
        ? { ...current, messages: [...(current.messages || []), message] }
        : current
    ));
  };

  const replacePendingMessage = (temporaryId, message) => {
    const replace = (items = []) => items.map((item) => (
      item.id === temporaryId ? message : item
    ));
    setMessages((current) => replace(current));
    setConversations((current) => current.map((item) => (
      item.id === conversation.id
        ? {
            ...item,
            messages: replace(item.messages),
            lastMessageAt: message.createdAt || item.lastMessageAt,
          }
        : item
    )));
    setSelectedConversation((current) => (
      current?.id === conversation.id
        ? { ...current, messages: replace(current.messages) }
        : current
    ));
  };

  useEffect(() => {
    setMessages(conversation?.messages || []);
  }, [conversation?.messages]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ block: 'end' });
  }, [messages]);

  useEffect(() => {
    if (!conversation?.id) return;
    api
      .inquiry(conversation.id)
      .then((detail) => {
        applyInquiryDetail(detail);
        setConversations((current) => current.map((item) => (
          item.id === conversation.id ? { ...item, unread: 0 } : item
        )));
        api.markInquiryRead(conversation.id).catch(() => {});
      })
      .catch((error) => notify(error.message, 'error'));
  }, [conversation?.id, currentUser.id, notify]);

  const respondToInquiry = async (accepted) => {
    try {
      if (!accepted) {
        await api.rejectInquiry(conversation.id);
        await refreshInquiry();
        await refreshWallet();
        notify('已拒绝询问，冻结金额将退回', 'success');
        return;
      }
      await api.acceptInquiry(conversation.id);
      await refreshInquiry();
      await refreshWallet();
      notify('已接受询问', 'success');
    } catch (error) {
      notify(error.message, 'error');
    }
  };

  const requestEnd = async () => {
    try {
      await api.requestInquiryEnd(conversation.id);
      await refreshInquiry();
      await refreshWallet();
      notify('结束申请已发送', 'success');
    } catch (error) {
      notify(error.message, 'error');
    }
  };

  const continueConversation = async () => {
    try {
      await api.continueInquiry(conversation.id);
      await refreshInquiry();
      notify('已继续交流', 'success');
    } catch (error) {
      notify(error.message, 'error');
    }
  };

  const confirmEnd = async () => {
    try {
      await api.confirmInquiryEnd(conversation.id);
      await refreshInquiry();
      await refreshWallet();
      notify('本次交流已结束', 'success');
    } catch (error) {
      notify(error.message, 'error');
    }
  };

  const cancelInquiry = async () => {
    try {
      await api.cancelInquiry(conversation.id);
      await refreshInquiry();
      await refreshWallet();
      notify('询问已撤销，冻结金额已退回', 'success');
    } catch (error) {
      notify(error.message, 'error');
    }
  };

  const send = async () => {
    const content = text.trim();
    if (!canChat || !content) return;

    const temporaryId = `sending-${Date.now()}`;
    const pendingMessage = {
      id: temporaryId,
      name: currentUser.name,
      text: content,
      avatar: currentUser.avatar,
      me: true,
      status: 'sending',
      time: '',
    };
    setText('');
    appendPendingMessage(pendingMessage);
    try {
      const saved = await api.sendInquiryMessage(conversation.id, content);
      const deliveredMessage = messageFromApi(saved, currentUser.id);
      deliveredMessage.createdAt = saved.createdAt;
      replacePendingMessage(temporaryId, deliveredMessage);
    } catch (error) {
      replacePendingMessage(temporaryId, { ...pendingMessage, status: 'failed' });
      notify(error.message, 'error');
    }
  };

  const sendImage = async (event) => {
    const image = event.target.files?.[0];
    event.target.value = '';
    if (!image || !canChat) return;
    if (!image.type.startsWith('image/')) {
      notify('只能发送照片', 'warning');
      return;
    }
    if (image.size > 10 * 1024 * 1024) {
      notify('照片不能超过10MB', 'warning');
      return;
    }
    const previewUrl = URL.createObjectURL(image);
    const temporaryId = `sending-image-${Date.now()}`;
    const pendingMessage = {
      id: temporaryId,
      name: currentUser.name,
      avatar: currentUser.avatar,
      me: true,
      status: 'sending',
      time: '',
      attachment: {
        url: previewUrl,
        name: image.name || '照片',
        type: image.type,
      },
    };
    appendPendingMessage(pendingMessage);
    try {
      const saved = await api.sendInquiryImage(conversation.id, image);
      const deliveredMessage = messageFromApi(saved, currentUser.id);
      deliveredMessage.createdAt = saved.createdAt;
      replacePendingMessage(temporaryId, deliveredMessage);
      URL.revokeObjectURL(previewUrl);
    } catch (error) {
      replacePendingMessage(temporaryId, { ...pendingMessage, status: 'failed' });
      notify(error.message, 'error');
    }
  };

  const statePresentation = {
    pending: {
      className: 'pending',
      Icon: Clock3,
      text: isIncoming ? '等待你的决定' : `等待${partnerLabel}接受`,
    },
    active: {
      className: 'active',
      Icon: MessageCircleMore,
      text: '交流进行中',
    },
    awaiting_confirmation: {
      className: 'pending',
      Icon: Clock3,
      text: isIncoming ? '等待提问者确认结束' : '对方申请结束',
    },
    ended: {
      className: 'ended',
      Icon: CheckCircle2,
      text: '本次交流已经结束',
    },
    rejected: {
      className: 'ended',
      Icon: XCircle,
      text: '本次询问未接受',
    },
    cancelled: {
      className: 'ended',
      Icon: XCircle,
      text: '本次询问已撤销',
    },
    expired: {
      className: 'ended',
      Icon: Clock3,
      text: '本次询问已超时',
    },
    disputed: {
      className: 'pending',
      Icon: Clock3,
      text: '本次询问正在处理中',
    },
  }[inquiryStatus];
  const StateIcon = statePresentation.Icon;

  return (
    <div className="chat-screen direct-chat-screen">
      <header className="chat-head">
        <button type="button" onClick={() => go(conversation?.returnScreen || 'inquiries')}>
          <ArrowLeft />
        </button>
        <div>
          <h3>{partnerLabel}</h3>
        </div>
        <i className="chat-head-placeholder" />
      </header>

      <div className={`chat-topic ${statePresentation.className}`}>
        <StateIcon />
        <span>{statePresentation.text}</span>
      </div>

      <div className="chat-body">

      <section className={`direct-inquiry-card ${inquiryStatus}`}>
        <span>本次询问</span>
        <p>{conversation?.question || '询问亲身经历中的实际情况'}</p>
        <div>
          <b>¥{Number(conversation?.amount || 0).toFixed(2)}</b>
        </div>
        {!['ended', 'rejected', 'cancelled'].includes(inquiryStatus) && !isIncoming && (
          <small>该金额正在冻结中</small>
        )}

        {inquiryStatus === 'pending' && isIncoming && (
          <footer>
            <button type="button" onClick={() => respondToInquiry(false)}>
              暂不接受
            </button>
            <button
              type="button"
              onClick={() => respondToInquiry(true)}
            >
              接受询问
            </button>
          </footer>
        )}

        {inquiryStatus === 'pending' && !isIncoming && (
          <>
            <small>{formatDeadline(conversation?.responseDeadline)}前未接受，金额自动退回</small>
            <footer className="single-action">
              <button type="button" onClick={() => setCancelConfirmOpen(true)}>
                撤销询问
              </button>
            </footer>
          </>
        )}

        {inquiryStatus === 'awaiting_confirmation' && !isIncoming && (
          <footer>
            <button type="button" onClick={continueConversation}>
              继续交流
            </button>
            <button type="button" onClick={confirmEnd}>
              确认结束
            </button>
          </footer>
        )}

        {inquiryStatus === 'awaiting_confirmation' && (
          <small>{formatDeadline(conversation?.confirmationDeadline)}后自动结束并结算</small>
        )}

        {inquiryStatus === 'rejected' && !isIncoming && <small>金额已退回余额</small>}
        {inquiryStatus === 'cancelled' && <small>冻结金额已退回余额</small>}
        {inquiryStatus === 'ended' && (
          <small>{isIncoming ? '金额已转入你的余额' : '金额已结算给回答者'}</small>
        )}
      </section>

      <div className="messages">
        {messages.map((message, index) => {
          const sender = message.me
            ? {
                ...currentUser,
                avatar: currentUser.avatar || message.avatar,
                name: currentUser.name || message.name,
              }
            : {
                name: message.name || partner.name,
                uid: message.uid || partner.uid || '',
                color: message.color || partner.color,
                avatar: message.avatar || partner.avatar,
              };
          const senderLabel = sender.name?.trim() || (sender.uid ? `UID ${sender.uid}` : '用户');

          return (
            <Fragment key={message.id}>
              {shouldShowMessageTime(messages, index) && (
                <time className="chat-time-divider">{formatMessageTime(message.createdAt)}</time>
              )}
            <div className={`message ${message.me ? 'me' : ''}`}>
              {!message.me && (
                <UserAvatar
                  src={sender.avatar}
                  uid={sender.uid}
                  name={sender.name}
                  className="mini-avatar"
                />
              )}
              {message.me && message.status === 'sending' && (
                <i className="message-send-state sending" aria-label="发送中" />
              )}
              {message.me && message.status === 'failed' && (
                <i className="message-send-state failed" aria-label="发送失败">!</i>
              )}
              <div className="message-content">
                <span className="message-sender">
                  <b>{senderLabel}</b>
                </span>
                {message.text && <p>{message.text}</p>}
                {message.attachment && (
                  <a
                    className="message-attachment"
                    href={message.attachment.url}
                    target="_blank"
                    rel="noreferrer"
                  >
                    {message.attachment.type?.startsWith('image/') && (
                      <img src={message.attachment.url} alt={message.attachment.name} />
                    )}
                  </a>
                )}
              </div>
              {message.me && (
                <UserAvatar
                  src={sender.avatar}
                  uid={sender.uid}
                  name={sender.name}
                  className="mini-avatar"
                />
              )}
            </div>
            </Fragment>
          );
        })}
        <div ref={messagesEndRef} />
      </div>

      </div>

      {endConfirmOpen && (
        <>
          <button className="sheet-mask" type="button" onClick={() => setEndConfirmOpen(false)} />
          <section className="direct-end-confirm">
            <h2>结束本次询问？</h2>
            <p>结束后，本次费用将结算给回答者，聊天不能继续。</p>
            <div>
              <button type="button" onClick={() => setEndConfirmOpen(false)}>
                继续交流
              </button>
              <button
                type="button"
                onClick={() => {
                  setEndConfirmOpen(false);
                  confirmEnd();
                }}
              >
                确认结束
              </button>
            </div>
          </section>
        </>
      )}

      {cancelConfirmOpen && (
        <>
          <button
            className="sheet-mask"
            type="button"
            onClick={() => setCancelConfirmOpen(false)}
          />
          <section className="direct-end-confirm">
            <h2>撤销本次询问？</h2>
            <p>撤销后对方无法再接受，冻结金额将立即退回余额。</p>
            <div>
              <button type="button" onClick={() => setCancelConfirmOpen(false)}>
                继续等待
              </button>
              <button
                type="button"
                onClick={() => {
                  setCancelConfirmOpen(false);
                  cancelInquiry();
                }}
              >
                确认撤销
              </button>
            </div>
          </section>
        </>
      )}

      <div className={`composer ${!canChat ? 'disabled' : ''}`}>
        <input
          ref={imageInputRef}
          className="chat-image-input"
          type="file"
          accept="image/*"
          onChange={sendImage}
        />
        <input
          ref={textInputRef}
          disabled={!canChat}
          value={text}
          onChange={(event) => setText(event.target.value)}
          onFocus={() => {
            setEmojiOpen(false);
            setMoreOpen(false);
          }}
          onKeyDown={(event) => {
            if (event.key !== 'Enter' || event.shiftKey || event.nativeEvent.isComposing) return;
            event.preventDefault();
            send();
          }}
          enterKeyHint="send"
          placeholder={
            canChat
              ? '说点什么…'
              : inquiryStatus === 'pending'
                ? '接受后即可开始聊天'
                : '本次交流已经结束'
          }
        />
        <button
          type="button"
          disabled={!canChat}
          aria-label="选择表情"
          onClick={() => {
            if (emojiOpen) {
              setEmojiOpen(false);
              window.setTimeout(() => textInputRef.current?.focus(), 0);
              return;
            }
            setMoreOpen(false);
            setEmojiOpen(true);
            textInputRef.current?.blur();
          }}
        >
          {emojiOpen ? <Keyboard size={21} /> : <Smile size={21} />}
        </button>
        <button
          type="button"
          disabled={!canChat}
          aria-label="更多"
          onClick={() => {
            setEmojiOpen(false);
            setMoreOpen((current) => !current);
            textInputRef.current?.blur();
          }}
        >
          <Plus size={23} />
        </button>
      </div>
      {emojiOpen && canChat && (
        <section className="emoji-panel">
          {EMOJIS.map((emoji) => (
            <button
              type="button"
              key={emoji}
              onClick={() => {
                setText((current) => `${current}${emoji}`);
              }}
            >
              {emoji}
            </button>
          ))}
        </section>
      )}
      {moreOpen && canChat && (
        <section className="chat-more-panel">
          <button
            type="button"
            onClick={() => {
              setMoreOpen(false);
              imageInputRef.current?.click();
            }}
          >
            <i><ImagePlus /></i>
            <span>照片</span>
          </button>
          <button
            type="button"
            onClick={() => {
              setMoreOpen(false);
              if (isIncoming) requestEnd();
              else setEndConfirmOpen(true);
            }}
          >
            <i><WalletCards /></i>
            <span>{isIncoming ? '申请结束' : '结束并结算'}</span>
          </button>
        </section>
      )}
    </div>
  );
}
