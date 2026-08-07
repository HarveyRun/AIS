import { useState } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  BadgeCheck,
  Banknote,
  Bell,
  BriefcaseBusiness,
  Building2,
  CalendarDays,
  Camera,
  Check,
  CheckCircle2,
  ChevronRight,
  CircleUserRound,
  Clock3,
  Coins,
  FileCheck2,
  HeartHandshake,
  Info,
  Landmark,
  LockKeyhole,
  MessageCircleMore,
  MessageSquareWarning,
  MoreHorizontal,
  PlusCircle,
  Search,
  Send,
  ShieldCheck,
  SlidersHorizontal,
  Smartphone,
  Sparkles,
  Star,
  UsersRound,
  WalletCards,
  X,
  Zap,
} from 'lucide-react';
import { groupMessages } from '../../data/mockData.js';
import './MessagePages.css';

export default function GroupChat({ go, notify, group }) {
  const [text, setText] = useState('');
  const [plus, setPlus] = useState(false);
  const [members, setMembers] = useState(false);
  const chatState = group?.status || 'active';
  const canChat = chatState === 'active';
  const roles = (group?.desc || '装修监理、室内设计、水电维修').split('、');
  const roleColors = ['#698b9b', '#c27b62', '#6f9584', '#8b7aa0'];
  const [msgs, setMsgs] = useState(() =>
    roles.map((role, index) => ({
      name: role,
      text: `我是${role}，有需要可以直接在群里问我。`,
      color: roleColors[index % roleColors.length],
    })),
  );
  const send = () => {
    if (!canChat || !text.trim()) return;
    setMsgs([...msgs, { name: '发起人', text, color: '#e68b59', me: true }]);
    setText('');
  };
  const action = (s) => {
    setPlus(false);
    setMsgs((current) => [
      ...current,
      { name: '发起人', text: `分享了${s}`, color: '#e68b59', me: true },
    ]);
  };
  const sendFile = (event, kind) => {
    const file = event.target.files?.[0];
    if (!file) return;
    setPlus(false);
    setMsgs((current) => [
      ...current,
      { name: '发起人', text: `${kind}：${file.name}`, color: '#e68b59', me: true },
    ]);
    event.target.value = '';
  };
  return (
    <div className="chat-screen">
      <header className="chat-head">
        <button onClick={() => go('messages', 'messages')}>
          <ArrowLeft />
        </button>
        <div>
          <h3>{group?.title || '旧房装修协作群'}</h3>
          <span>{group?.members || 5}人 · 3类岗位</span>
        </div>
        <button onClick={() => setMembers(true)}>
          <MoreHorizontal />
        </button>
      </header>
      <div className={`chat-topic ${chatState}`}>
        {chatState === 'active' && <CalendarDays />}
        {chatState === 'locked' && <LockKeyhole />}
        {chatState === 'ended' && <CheckCircle2 />}
        <span>
          {chatState === 'active' && '服务时间段 周三 19:00 ~ 2300'}
          {chatState === 'locked' && '现在是休息时间，到了约定时间再来吧'}
          {chatState === 'ended' && '该事项已经结束'}
        </span>
      </div>
      <div className="messages">
        {msgs.map((m, i) => (
          <div className={`message ${m.me ? 'me' : ''}`} key={i}>
            {!m.me && (
              <div className="mini-avatar" style={{ background: m.color }}>
                {m.name[0]}
              </div>
            )}
            <div>
              <span>{m.name}</span>
              <p>{m.text}</p>
            </div>
          </div>
        ))}
      </div>
      {plus && canChat && (
        <>
          <button className="sheet-mask" onClick={() => setPlus(false)} />
          <div className="chat-sheet">
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
            <button onClick={() => action('事项资料')}>
              <i>事</i>
              <span>事项资料</span>
            </button>
            <button onClick={() => action('服务记录')}>
              <i>记</i>
              <span>服务记录</span>
            </button>
          </div>
        </>
      )}
      {members && (
        <>
          <button className="sheet-mask" onClick={() => setMembers(false)} />
          <div className="member-sheet">
            <h2>群组成员（{roles.length + 1}）</h2>
            {['发起人', ...roles].map((x, i) => (
              <div key={x}>
                <i>{x[0]}</i>
                <b>{x}</b>
                <span className={i === roles.length ? 'offline' : ''}>
                  {i === roles.length ? '离线' : '在线'}
                </span>
              </div>
            ))}
          </div>
        </>
      )}
      <div className={`composer ${!canChat ? 'disabled' : ''}`}>
        <button disabled={!canChat} onClick={() => setPlus(!plus)}>
          ＋
        </button>
        <input
          disabled={!canChat}
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && send()}
          placeholder={
            chatState === 'active'
              ? '在群里说点什么…'
              : chatState === 'ended'
                ? '这件事已经结束了'
                : '到了约定时间就能继续聊'
          }
        />
        <button disabled={!canChat} className="send" onClick={send}>
          <Send size={17} />
        </button>
      </div>
    </div>
  );
}
