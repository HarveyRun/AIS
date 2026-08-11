import { useEffect, useState } from 'react';

const RESPONSE_TIMEOUT = 24 * 60 * 60 * 1000;
const CONFIRMATION_TIMEOUT = 48 * 60 * 60 * 1000;

export function createResponseDeadline(now = Date.now()) {
  return new Date(now + RESPONSE_TIMEOUT).toISOString();
}

export function createConfirmationDeadline(now = Date.now()) {
  return new Date(now + CONFIRMATION_TIMEOUT).toISOString();
}

export default function useDirectInquirySettlement({
  conversations,
  setConversations,
  selectedConversation,
  setSelectedConversation,
  setBalance,
  setLedger,
  setNotices,
}) {
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 30000);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    const applyTimeout = (conversation) => {
      if (conversation.type !== 'direct') return conversation;

      if (
        conversation.inquiryStatus === 'pending' &&
        conversation.responseDeadline &&
        new Date(conversation.responseDeadline).getTime() <= now
      ) {
        return {
          ...conversation,
          inquiryStatus: 'rejected',
          status: 'rejected',
          statusText: '未接受',
          desc: '对方未在24小时内接受',
        };
      }

      if (
        conversation.inquiryStatus === 'awaiting_confirmation' &&
        conversation.confirmationDeadline &&
        new Date(conversation.confirmationDeadline).getTime() <= now
      ) {
        return {
          ...conversation,
          inquiryStatus: 'ended',
          settlementStatus: 'settled',
          status: 'ended',
          statusText: '已结束',
          desc: '本次交流已经结束',
        };
      }

      return conversation;
    };

    const nextConversations = conversations.map(applyTimeout);
    const changed = nextConversations.some(
      (conversation, index) => conversation !== conversations[index],
    );
    if (!changed) return;

    setConversations(nextConversations);
    if (selectedConversation) {
      const nextSelectedConversation = nextConversations.find(
        (item) => item.id === selectedConversation.id,
      );
      if (nextSelectedConversation) setSelectedConversation(nextSelectedConversation);
    }
  }, [conversations, now, selectedConversation, setConversations, setSelectedConversation]);

  useEffect(() => {
    const outcomes = conversations.filter(
      (conversation) =>
        conversation.type === 'direct' &&
        !conversation.financeProcessed &&
        (['rejected', 'cancelled'].includes(conversation.inquiryStatus) ||
          conversation.settlementStatus === 'settled'),
    );
    if (outcomes.length === 0) return;

    let balanceDelta = 0;
    const records = [];
    const newNotices = [];

    outcomes.forEach((conversation) => {
      const amount = Number(conversation.amount || 0);

      if (
        ['rejected', 'cancelled'].includes(conversation.inquiryStatus) &&
        conversation.direction === 'outgoing'
      ) {
        balanceDelta += amount;
        records.push([
          '收入',
          conversation.inquiryStatus === 'cancelled'
            ? `${conversation.title}询问撤销退款`
            : `${conversation.title}询问退款`,
          `+¥${amount.toFixed(2)}`,
          '刚刚',
        ]);
        newNotices.push({
          id: `notice-refund-${conversation.id}`,
          title: '询问金额已退回',
          content:
            conversation.inquiryStatus === 'cancelled'
              ? `询问已撤销，¥${amount.toFixed(2)}已退回余额`
              : `${conversation.title}未接受询问，¥${amount.toFixed(2)}已退回余额`,
          time: '刚刚',
          screen: 'wallet',
          read: false,
        });
      }

      if (conversation.settlementStatus === 'settled') {
        if (conversation.direction === 'incoming') {
          balanceDelta += amount;
          records.push(['收入', `${conversation.title}交流收入`, `+¥${amount.toFixed(2)}`, '刚刚']);
          newNotices.push({
            id: `notice-income-${conversation.id}`,
            title: '询问费用已到账',
            content: `¥${amount.toFixed(2)}已转入账户余额`,
            time: '刚刚',
            screen: 'wallet',
            read: false,
          });
        } else {
          records.push(['支出', `${conversation.title}经历询问`, `-¥${amount.toFixed(2)}`, '刚刚']);
          newNotices.push({
            id: `notice-settlement-${conversation.id}`,
            title: '询问已经结算',
            content: `¥${amount.toFixed(2)}已结算给回答者`,
            time: '刚刚',
            screen: 'wallet',
            read: false,
          });
        }
      }
    });

    if (balanceDelta !== 0) {
      setBalance((current) => Number((current + balanceDelta).toFixed(2)));
    }
    if (records.length > 0) {
      setLedger((current) => [...records, ...current]);
    }
    if (newNotices.length > 0) {
      setNotices((current) => [...newNotices, ...current]);
    }

    const processedIds = new Set(outcomes.map((conversation) => conversation.id));
    const nextConversations = conversations.map((conversation) =>
      processedIds.has(conversation.id)
        ? { ...conversation, financeProcessed: true }
        : conversation,
    );
    setConversations(nextConversations);

    if (selectedConversation && processedIds.has(selectedConversation.id)) {
      setSelectedConversation({ ...selectedConversation, financeProcessed: true });
    }
  }, [
    conversations,
    selectedConversation,
    setBalance,
    setConversations,
    setLedger,
    setSelectedConversation,
    setNotices,
  ]);
}
