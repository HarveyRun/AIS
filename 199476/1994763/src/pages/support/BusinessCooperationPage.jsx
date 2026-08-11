import { useState } from 'react';
import Page from '../../components/layout/Page.jsx';
import './BusinessCooperationPage.css';

export default function BusinessCooperationPage({ go, notify }) {
  const [contact, setContact] = useState('');
  const [content, setContent] = useState('');
  const [submitted, setSubmitted] = useState(false);

  const submit = () => {
    if (!contact.trim() || !content.trim()) return;

    setSubmitted(true);
    notify('合作意向已提交');
  };

  return (
    <Page title="商务合作" back={() => go('profile')} className="business-page">
      {submitted ? (
        <section className="business-success">
          <i>✓</i>
          <h2>已收到你的合作意向</h2>
          <p>工作人员会通过你填写的联系方式与你联系。</p>
          <button type="button" onClick={() => go('profile')}>
            返回我的
          </button>
        </section>
      ) : (
        <section className="business-form">
          <div>
            <label htmlFor="business-contact">联系方式</label>
            <input
              id="business-contact"
              value={contact}
              onChange={(event) => setContact(event.target.value)}
              placeholder="手机号、微信或邮箱"
            />
          </div>
          <div>
            <label htmlFor="business-content">合作内容</label>
            <textarea
              id="business-content"
              value={content}
              maxLength={300}
              onChange={(event) => setContent(event.target.value)}
              placeholder="请简单说明合作内容"
            />
            <small>{content.length}/300</small>
          </div>
          <button
            className="business-submit"
            type="button"
            disabled={!contact.trim() || !content.trim()}
            onClick={submit}
          >
            提交
          </button>
        </section>
      )}
    </Page>
  );
}
