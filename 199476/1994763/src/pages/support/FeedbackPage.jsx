import { useState } from 'react';
import Page from '../../components/layout/Page.jsx';
import './SupportPages.css';

export default function FeedbackPage({ go, notify, conversations, records, setRecords }) {
  const [type, setType] = useState('feedback');
  const [target, setTarget] = useState('');
  const [text, setText] = useState('');
  const [complaintType, setComplaintType] = useState('服务态度问题');
  const targets = Array.from(
    new Map(
      conversations
        .filter((conversation) => conversation.partner?.uid)
        .map((conversation) => [conversation.partner.uid, conversation.partner]),
    ).values(),
  );
  const submit = () => {
    const targetUser = targets.find((item) => item.uid === target);
    const record = {
      id: `feedback-${Date.now()}`,
      type,
      category: type === 'complaint' ? complaintType : '产品反馈',
      target: targetUser ? targetUser.name || `UID ${targetUser.uid}` : '',
      content: text.trim(),
      status: '已提交',
      time: '刚刚',
    };
    setRecords((current) => [record, ...current]);
    notify(type === 'complaint' ? '投诉已提交' : '反馈已提交');
    setText('');
    setTarget('');
  };

  return (
    <Page title="投诉与反馈" back={() => go('profile', 'profile')}>
      <div className="feedback-tabs">
        <button
          type="button"
          className={type === 'feedback' ? 'active' : ''}
          onClick={() => setType('feedback')}
        >
          产品反馈
        </button>
        <button
          type="button"
          className={type === 'complaint' ? 'active' : ''}
          onClick={() => setType('complaint')}
        >
          投诉
        </button>
      </div>
      <section className="feedback-form">
        {type === 'complaint' && (
          <>
            <label>投诉对象</label>
            <select value={target} onChange={(event) => setTarget(event.target.value)}>
              <option value="">请选择与你交流过的人</option>
              {targets.map((item) => (
                <option value={item.uid} key={item.uid}>
                  {item.name?.trim() || `UID ${item.uid}`}（UID {item.uid}）
                </option>
              ))}
            </select>
            <label>投诉类型</label>
            <select
              value={complaintType}
              onChange={(event) => setComplaintType(event.target.value)}
            >
              <option>服务态度问题</option>
              <option>虚假能力信息</option>
              <option>违规收费</option>
              <option>骚扰或不当言论</option>
              <option>其它</option>
            </select>
          </>
        )}
        <label>{type === 'complaint' ? '详细说明' : '反馈内容'}</label>
        <textarea
          value={text}
          onChange={(event) => setText(event.target.value)}
          placeholder={
            type === 'complaint' ? '请说明发生的时间、经过和诉求' : '说说你希望事先问改进什么'
          }
        />
      </section>
      {records.length > 0 && (
        <section className="feedback-records">
          <h2>我的提交</h2>
          {records.slice(0, 5).map((record) => (
            <article key={record.id}>
              <div>
                <b>{record.category}</b>
                <small>{record.target || record.content}</small>
              </div>
              <span>{record.status}</span>
            </article>
          ))}
        </section>
      )}
      <button
        type="button"
        disabled={!text.trim() || (type === 'complaint' && !target)}
        className="sticky-primary"
        onClick={submit}
      >
        {type === 'complaint' ? '提交投诉' : '提交反馈'}
      </button>
    </Page>
  );
}
