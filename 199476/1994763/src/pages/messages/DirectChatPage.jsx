import { useEffect, useState } from 'react';
import {
  ArrowLeft,
  CheckCircle2,
  Clock3,
  MessageCircleMore,
  Send,
  WalletCards,
  XCircle,
} from 'lucide-react';
import { createConfirmationDeadline } from '../../hooks/useDirectInquirySettlement.js';
import UserAvatar from '../../components/profile/UserAvatar.jsx';
import './MessagePages.css';
import { api, request } from '../../api/http.js';

function formatDeadline(value) {
  if (!value) return '';
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
}

function currentMessageTime() {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date());
}

export default function DirectChatPage({
  go,
  conversation,
  setConversations,
  setSelectedConversation,
  currentUser = { name: '', uid: '', avatar: '' },
  canAnswer,
  acceptingInquiries,
  certifications,
  addNotice = () => {},
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
  const certifiedText = certifications
    .filter((item) => item.status === '已认证')
    .map((item) => `${item.name || ''}${item.title || ''}${item.detail || ''}`)
    .join('');
  const requiredCapability = conversation?.capability || '';
  const matchesCapability = !requiredCapability || certifiedText.includes(requiredCapability);
  const allowedToAnswer = Boolean(canAnswer && acceptingInquiries && matchesCapability);
  const canChat = inquiryStatus === 'active';
  const [text, setText] = useState('');
  const [moreActionsOpen, setMoreActionsOpen] = useState(false);
  const [messages, setMessages] = useState(conversation?.messages || []);
  const [fileError, setFileError] = useState('');
  const [endConfirmOpen, setEndConfirmOpen] = useState(false);
  const [cancelConfirmOpen, setCancelConfirmOpen] = useState(false);

  useEffect(() => {
    if (!conversation?.id || String(conversation.id).startsWith('direct-')) return;
    api.inquiry(conversation.id).then((detail) => {
      const loadedMessages = detail.messages.map((message) => ({
        id: message.id,
        name: message.senderName,
        text: message.content,
        avatar: message.senderAvatar,
        me: message.senderId === currentUser.id,
        time: new Date(message.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      }));
      setMessages(loadedMessages);
    }).catch(() => {});
  }, [conversation?.id, currentUser.id]);

  const updateConversation = (changes) => {
    const updatedConversation = {
      ...conversation,
      ...changes,
      time: '刚刚',
    };

    setConversations((current) =>
      current.map((item) => (item.id === updatedConversation.id ? updatedConversation : item)),
    );
    setSelectedConversation(updatedConversation);
  };

  const saveMessages = (nextMessages, preview) => {
    updateConversation({
      messages: nextMessages,
      desc: preview,
    });
  };

  const respondToInquiry = async (accepted) => {
    if (accepted && !allowedToAnswer) return;
    if (!accepted) {
      await request(`/inquiries/${conversation.id}/reject`, { method: 'POST' });
      updateConversation({
        inquiryStatus: 'rejected',
        status: 'rejected',
        statusText: '未接受',
        desc: '本次询问未接受',
      });
      addNotice({
        title: '询问未接受',
        content: `你没有接受${partnerLabel}的询问`,
        screen: 'inquiries',
      });
      return;
    }

    await request(`/inquiries/${conversation.id}/accept`, { method: 'POST' });
    const acceptedMessage = {
      id: `accepted-${Date.now()}`,
      name: '我',
      text: '我已接受这次询问，可以开始聊了。',
      color: '#c23b32',
      me: true,
      uid: currentUser.uid,
      time: currentMessageTime(),
    };
    const nextMessages = [...messages, acceptedMessage];
    setMessages(nextMessages);
    updateConversation({
      inquiryStatus: 'active',
      status: 'active',
      statusText: '交流中',
      desc: '已经接受，可以开始交流',
      messages: nextMessages,
    });
    addNotice({
      title: '已接受询问',
      content: `你和${partnerLabel}现在可以开始交流`,
      screen: 'inquiries',
    });
  };

  const requestEnd = async () => {
    await request(`/inquiries/${conversation.id}/request-end`, { method: 'POST' });
    updateConversation({
      inquiryStatus: 'awaiting_confirmation',
      status: 'awaiting',
      statusText: '待确认',
      desc: '已申请结束，等待提问者确认',
      endRequestCount: Number(conversation?.endRequestCount || 0) + 1,
      confirmationDeadline: createConfirmationDeadline(),
    });
  };

  const continueConversation = async () => {
    await request(`/inquiries/${conversation.id}/continue`, { method: 'POST' });
    updateConversation({
      inquiryStatus: 'active',
      status: 'active',
      statusText: '交流中',
      desc: '继续交流中',
      continueCount: Number(conversation?.continueCount || 0) + 1,
      confirmationDeadline: null,
    });
  };

  const confirmEnd = async () => {
    await request(`/inquiries/${conversation.id}/confirm-end`, { method: 'POST' });
    updateConversation({
      inquiryStatus: 'ended',
      settlementStatus: 'settled',
      status: 'ended',
      statusText: '已结束',
      desc: '本次交流已经结束',
      confirmationDeadline: null,
    });
  };

  const cancelInquiry = async () => {
    await request(`/inquiries/${conversation.id}/cancel`, { method: 'POST' });
    updateConversation({
      inquiryStatus: 'cancelled',
      settlementStatus: 'refunded',
      status: 'cancelled',
      statusText: '已撤销',
      desc: '你已撤销本次询问',
      responseDeadline: null,
    });
  };

  const send = async () => {
    const content = text.trim();
    if (!canChat || !content) return;

    const saved = await request(`/inquiries/${conversation.id}/messages`, {
      method: 'POST',
      body: JSON.stringify({ content }),
    });
    const nextMessages = [
      ...messages,
      {
        id: saved.id,
        name: '我',
        text: content,
        color: '#c23b32',
        me: true,
        uid: currentUser.uid,
        time: currentMessageTime(),
      },
    ];
    setMessages(nextMessages);
    saveMessages(nextMessages, content);
    setText('');
  };

  const sendFile = (event, kind) => {
    const file = event.target.files?.[0];
    if (!canChat || !file) return;

    if (file.size > 2 * 1024 * 1024) {
      setFileError('单个附件不能超过2MB');
      event.target.value = '';
      return;
    }

    const fileMessage = `${kind}：${file.name}`;
    const reader = new FileReader();
    reader.onload = () => {
      const nextMessages = [
        ...messages,
        {
          id: `file-${Date.now()}`,
          name: '我',
          text: fileMessage,
          color: '#c23b32',
          me: true,
          uid: currentUser.uid,
          time: currentMessageTime(),
          attachment: {
            name: file.name,
            size: file.size,
            type: file.type,
            url: reader.result,
          },
        },
      ];
      setMessages(nextMessages);
      saveMessages(nextMessages, fileMessage);
      setMoreActionsOpen(false);
      setFileError('');
    };
    reader.onerror = () => setFileError('附件读取失败，请重新选择');
    reader.readAsDataURL(file);
    event.target.value = '';
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
      text: `正在聊：${conversation?.topic || '亲身经历'}`,
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

      <section className={`direct-inquiry-card ${inquiryStatus}`}>
        <span>本次询问</span>
        <p>{conversation?.question || '询问亲身经历中的实际情况'}</p>
        <div>
          <WalletCards />
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
              disabled={!allowedToAnswer}
              onClick={() => respondToInquiry(true)}
            >
              接受询问
            </button>
          </footer>
        )}

        {inquiryStatus === 'pending' && isIncoming && !allowedToAnswer && (
          <small className="answer-blocked">
            {!acceptingInquiries ? '你已暂停接受询问' : '完成对应认证后才能接受这次询问'}
            <button
              type="button"
              onClick={() => go(!acceptingInquiries ? 'certs' : 'certExperience')}
            >
              {!acceptingInquiries ? '去开启' : '去认证'}
            </button>
          </small>
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
        {messages.map((message) => {
          const sender = message.me
            ? currentUser
            : {
                name: message.name || partner.name,
                uid: message.uid || partner.uid || '',
                color: message.color || partner.color,
                avatar: message.avatar || partner.avatar,
              };
          const senderLabel = sender.name?.trim() || (sender.uid ? `UID ${sender.uid}` : '用户');

          return (
            <div className={`message ${message.me ? 'me' : ''}`} key={message.id}>
              {!message.me && (
                <UserAvatar
                  src={sender.avatar}
                  uid={sender.uid}
                  name={sender.name}
                  className="mini-avatar"
                />
              )}
              <div className="message-content">
                <span className="message-sender">
                  <b>{senderLabel}</b>
                </span>
                <p>{message.text}</p>
                {message.attachment && (
                  <a
                    className="message-attachment"
                    href={message.attachment.url}
                    download={message.attachment.name}
                  >
                    {message.attachment.type?.startsWith('image/') && (
                      <img src={message.attachment.url} alt={message.attachment.name} />
                    )}
                    <span>{message.attachment.name}</span>
                  </a>
                )}
                <time>{message.time || ''}</time>
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
          );
        })}
      </div>

      {inquiryStatus === 'active' && (
        <button
          className="direct-request-end"
          type="button"
          onClick={isIncoming ? requestEnd : () => setEndConfirmOpen(true)}
        >
          {isIncoming ? '申请结束' : '结束并结算'}
        </button>
      )}

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

      {fileError && <small className="chat-file-error">{fileError}</small>}

      {moreActionsOpen && canChat && (
        <>
          <button className="sheet-mask" type="button" onClick={() => setMoreActionsOpen(false)} />
          <div className="chat-sheet direct-chat-sheet">
            <label>
              <input
                type="file"
                hidden
                accept="image/*"
                onChange={(event) => sendFile(event, '图片')}
              />
              <i>图</i>
              <span>图片</span>
            </label>
            <label>
              <input type="file" hidden onChange={(event) => sendFile(event, '文件')} />
              <i>文</i>
              <span>文件</span>
            </label>
          </div>
        </>
      )}

      <div className={`composer ${!canChat ? 'disabled' : ''}`}>
        <button
          type="button"
          disabled={!canChat}
          onClick={() => setMoreActionsOpen((current) => !current)}
        >
          ＋
        </button>
        <input
          disabled={!canChat}
          value={text}
          onChange={(event) => setText(event.target.value)}
          onKeyDown={(event) => event.key === 'Enter' && send()}
          placeholder={
            canChat
              ? '说点什么…'
              : inquiryStatus === 'pending'
                ? '接受后即可开始聊天'
                : '本次交流已经结束'
          }
        />
        <button type="button" disabled={!canChat} className="send" onClick={send}>
          <Send size={17} />
        </button>
      </div>
    </div>
  );
}
