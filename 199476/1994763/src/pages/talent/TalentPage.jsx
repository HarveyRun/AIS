import { useState } from 'react';
import { MessageCircleMore, ShieldCheck, WalletCards, X } from 'lucide-react';
import Page from '../../components/layout/Page.jsx';
import UserAvatar from '../../components/profile/UserAvatar.jsx';
import { Career } from '../../components/talent/TalentCard.jsx';
import './TalentPage.css';
import { api } from '../../api/http.js';

export default function TalentPage({
  go,
  talent: p,
  conversations,
  setConversations,
  setSelectedConversation,
  balance,
  refreshWallet,
  problem,
  experience,
  notify,
}) {
  const [inquiryOpen, setInquiryOpen] = useState(false);
  const [inquiryQuestion, setInquiryQuestion] = useState('');
  const [inquiryAmount, setInquiryAmount] = useState('');
  const inquiryContext = experience || problem || p.main;
  const openInquiry = async () => {
    const existingConversation = conversations.find(
      (item) =>
        item.direction === 'outgoing' &&
        item.partner?.uid === p.uid &&
        !['ended', 'rejected', 'cancelled'].includes(item.inquiryStatus),
    );

    if (
      existingConversation &&
      !['ended', 'rejected', 'cancelled'].includes(existingConversation.inquiryStatus)
    ) {
      try {
        const current = await api.inquiry(existingConversation.id);
        if (['PENDING', 'ACTIVE', 'AWAITING_CONFIRMATION', 'DISPUTED'].includes(current.inquiry.status)) {
          setSelectedConversation(existingConversation);
          go('directChat');
          return;
        }
      } catch (error) {
        notify(error.message, 'error');
        return;
      }
    }

    setInquiryQuestion('');
    setInquiryAmount('');
    setInquiryOpen(true);
  };

  const submitExperienceInquiry = async () => {
    const question = inquiryQuestion.trim();
    const amount = Number(inquiryAmount);

    if (!question) {
      notify('请先写下你想问的事情', 'warning');
      return;
    }
    if (!Number.isFinite(amount) || amount <= 0) {
      notify('请输入有效金额', 'warning');
      return;
    }
    if (!p.id) {
      notify('档案数据尚未加载完成，请稍后再试', 'warning');
      return;
    }

    try {
      const created = await api.createInquiry({
        answererId: p.id,
        topic: inquiryContext,
        sourceType: experience ? 'EXPERIENCE' : problem ? 'PROBLEM' : 'PROFILE',
        question,
        amount: Number(amount.toFixed(2)),
      });
      const detail = await api.inquiry(created.id);
      const confirmed = detail.inquiry;
      const conversationId = confirmed.id;
      const conversation = {
        id: conversationId,
        type: 'direct',
        direction: 'outgoing',
        title: p.name?.trim() || `UID ${p.uid}`,
        desc: question,
        members: 2,
        time: '刚刚',
        unread: 0,
        color: p.color,
        status: 'pending',
        statusText: '待接受',
        inquiryStatus: 'pending',
        settlementStatus: 'unsettled',
        financeProcessed: false,
        amount: Number(confirmed.amount),
        question,
        responseDeadline: confirmed.responseDeadline,
        continueCount: 0,
        endRequestCount: 0,
        topic: inquiryContext,
        sourceType: experience ? 'experience' : problem ? 'problem' : 'profile',
        capability: experience || p.main,
        partner: {
          uid: p.uid,
          name: p.name,
          role: p.main,
          color: p.color,
          avatar: p.avatar,
        },
        messages: [],
      };

      setConversations((current) => [conversation, ...current]);
      setSelectedConversation(conversation);
      await refreshWallet();
      setInquiryOpen(false);
      notify('询问已发出，等待对方回应', 'success');
      go('directChat');
    } catch (requestError) {
      notify(requestError.message, 'error');
    }
  };

  return (
    <Page title="个人档案" back={() => window.history.back()}>
      <section className="profile-hero">
        <UserAvatar src={p.avatar} uid={p.uid} name={p.name} className="avatar large" verified />
        <h1>{p.name}</h1>
        <span>UID {p.uid} · 信息已经核实</span>
      </section>
      <section className="fact-box">
        <h2>
          <ShieldCheck size={19} /> 做过的工作
        </h2>
        <Career label="主职" name={p.main} years={p.mainYears} />
        <Career label="个人事业" name={p.venture} years={p.ventureYears} />
      </section>
      <section className="story-box">
        <h2>亲身经历过的事</h2>
        <div className="story-tags big">
          {(p.experiences || []).map((x) => (
            <span key={x}>{x}</span>
          ))}
          {(p.experiences || []).length === 0 && <span>暂未填写亲身经历</span>}
        </div>
        <p>这些经历基础材料已核实。</p>
      </section>
      <button
        className="profile-inquiry-button"
        type="button"
        onClick={openInquiry}
      >
        <MessageCircleMore />
        询问
      </button>
      {inquiryOpen && (
        <>
          <button className="sheet-mask" type="button" onClick={() => setInquiryOpen(false)} />
          <section className="experience-inquiry-sheet">
            <header>
              <div>
                <h2>发起询问</h2>
                <p>对方接受后即可开始私聊</p>
              </div>
              <button type="button" onClick={() => setInquiryOpen(false)}>
                <X />
              </button>
            </header>

            <label>
              <span>你想问什么</span>
              <textarea
                autoFocus
                value={inquiryQuestion}
                onChange={(event) => {
                  setInquiryQuestion(event.target.value);
                }}
                maxLength={120}
                placeholder="把想了解的事情简单说清楚"
              />
              <small>{inquiryQuestion.length}/120</small>
            </label>

            <label>
              <span>你打算给多少钱</span>
              <div className="inquiry-amount-input">
                <b>¥</b>
                <input
                  inputMode="decimal"
                  value={inquiryAmount}
                  onChange={(event) => {
                    const value = event.target.value;
                    if (/^\d*(\.\d{0,2})?$/.test(value)) {
                      setInquiryAmount(value);
                    }
                  }}
                  placeholder="0.00"
                />
              </div>
            </label>

            <div className="inquiry-balance">
              <WalletCards />
              <span>
                本次冻结 ¥{Number(inquiryAmount || 0).toFixed(2)} · 余额 ¥{balance.toFixed(2)}
              </span>
            </div>
            <p className="inquiry-payment-note">发起后金额暂时冻结，对方未接受会自动退回。</p>
            <button className="inquiry-submit" type="button" onClick={submitExperienceInquiry}>
              确认发起
            </button>
          </section>
        </>
      )}
    </Page>
  );
}
