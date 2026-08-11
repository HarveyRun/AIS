import { useEffect, useState } from 'react';
import { Check } from 'lucide-react';
import BottomNav from '../components/navigation/BottomNav.jsx';
import useAppNavigation from '../hooks/useAppNavigation.js';
import useDirectInquirySettlement from '../hooks/useDirectInquirySettlement.js';
import useToast from '../hooks/useToast.js';
import usePersistentState from '../hooks/usePersistentState.js';
import AppRoutes from '../routes/AppRoutes.jsx';
import { api, getAccessToken, setAccessToken } from '../api/http.js';
import '../styles/index.css';

const DATA_RESET_VERSION = '2026-08-11-clean-start-1';

if (localStorage.getItem('shixianwen-data-reset-version') !== DATA_RESET_VERSION) {
  Object.keys(localStorage)
    .filter((key) => key.startsWith('shixianwen-') || key.startsWith('guangyi-'))
    .forEach((key) => localStorage.removeItem(key));
  sessionStorage.clear();
  localStorage.setItem('shixianwen-data-reset-version', DATA_RESET_VERSION);
}

function App() {
  const { tab, screen, go } = useAppNavigation('login', 'home');
  const [isAuthenticated, setIsAuthenticated] = useState(() => Boolean(getAccessToken()));
  const [category, setCategory] = usePersistentState('shixianwen-category', '生活');
  const { toast, notify } = useToast();
  const [problem, setProblem] = usePersistentState('shixianwen-problem', '');
  const [experience, setExperience] = usePersistentState('shixianwen-experience', '');
  const [talent, setTalent] = usePersistentState('shixianwen-selected-talent', null);
  const [answerers, setAnswerers] = useState([]);
  const [certType, setCertType] = usePersistentState('shixianwen-cert-type', '');
  const [userProfile, setUserProfile] = usePersistentState('shixianwen-user-profile', {
    name: '',
    uid: '',
    phone: '',
    avatar: '',
  });
  const [certifications, setCertifications] = usePersistentState('shixianwen-certifications', []);
  const [conversations, setConversations] = usePersistentState(
    'shixianwen-conversations',
    [],
  );
  const inquiryUnreadCount = conversations.reduce(
    (total, conversation) => total + Math.max(0, Number(conversation.unread) || 0),
    0,
  );
  const [selectedConversation, setSelectedConversation] = usePersistentState(
    'shixianwen-selected-conversation',
    () => conversations[0] || null,
  );
  const [balance, setBalance] = usePersistentState('shixianwen-balance', 0);
  const [ledger, setLedger] = usePersistentState('shixianwen-ledger', []);
  const [withdrawals, setWithdrawals] = usePersistentState('shixianwen-withdrawals', []);
  const [accountStats, setAccountStats] = usePersistentState('shixianwen-account-stats', {
    totalWithdrawn: 0,
  });
  const [notices, setNotices] = usePersistentState('shixianwen-notices', []);
  const [feedbackRecords, setFeedbackRecords] = usePersistentState(
    'shixianwen-feedback-records',
    [],
  );
  const [acceptingInquiries, setAcceptingInquiries] = usePersistentState(
    'shixianwen-accepting-inquiries',
    true,
  );

  const frozenAmount = Number(
    conversations
      .filter(
        (conversation) =>
          conversation.direction === 'outgoing' &&
          ['pending', 'active', 'awaiting_confirmation'].includes(conversation.inquiryStatus) &&
          conversation.settlementStatus !== 'settled',
      )
      .reduce((total, conversation) => total + Number(conversation.amount || 0), 0)
      .toFixed(2),
  );

  useEffect(() => {
    setCertifications((current) => {
      const submittedItems = current.filter((item) => !(item.isNew && item.status === '填写中'));
      return submittedItems.length === current.length ? current : submittedItems;
    });
  }, [setCertifications]);

  useDirectInquirySettlement({
    conversations,
    setConversations,
    selectedConversation,
    setSelectedConversation,
    setBalance,
    setLedger,
    setNotices,
  });

  const login = ({ token, user }) => {
    setAccessToken(token);
    sessionStorage.setItem('shixianwen-authenticated', 'true');
    setUserProfile({
      id: user.id,
      name: user.nickname || '',
      uid: user.uid,
      phone: user.phone,
      avatar: user.avatarUrl || '',
    });
    setAcceptingInquiries(user.acceptingInquiries);
    setIsAuthenticated(true);
    go('home');
  };
  const logout = async () => {
    try {
      await api.logout();
    } catch {
      // 本地仍需退出，避免失效会话困住用户。
    }
    setAccessToken('');
    sessionStorage.removeItem('shixianwen-authenticated');
    sessionStorage.removeItem('guangyi-authenticated');
    setIsAuthenticated(false);
    go('login');
  };
  const deleteAccount = async () => {
    try {
      await api.deleteAccount();
    } catch (error) {
      notify(error.message);
      return;
    }
    setProblem('');
    setExperience('');
    setTalent(null);
    setCertType('');
    setUserProfile({
      name: '',
      uid: '',
      phone: '',
      avatar: '',
    });
    setCertifications([]);
    setConversations([]);
    setSelectedConversation(null);
    setBalance(0);
    setLedger([]);
    setWithdrawals([]);
    setAccountStats({ totalWithdrawn: 0 });
    setNotices([]);
    setFeedbackRecords([]);
    setAcceptingInquiries(true);
    sessionStorage.removeItem('shixianwen-authenticated');
    sessionStorage.removeItem('guangyi-authenticated');
    setAccessToken('');
    setIsAuthenticated(false);
    go('login');
  };

  useEffect(() => {
    if (!isAuthenticated) return undefined;
    let active = true;
    Promise.all([api.me(), api.wallet(), api.walletTransactions(), api.withdrawals(), api.notifications(), api.inquiries(), api.answerers(), api.bankCard()])
      .then(([user, wallet, transactions, withdrawalItems, notificationItems, inquiryItems, answererItems, bankCard]) => {
        if (!active) return;
        setUserProfile({ id: user.id, name: user.nickname || '', uid: user.uid, phone: user.phone, avatar: user.avatarUrl || '' });
        setAcceptingInquiries(user.acceptingInquiries);
        setBalance(Number(wallet.availableBalance));
        setAccountStats({ totalWithdrawn: Number(wallet.totalWithdrawn), bankCard: bankCard ? { holderName: bankCard.holderName, bankName: bankCard.bankName, cardNumber: bankCard.lastFour } : null });
        setLedger(transactions.map((item) => [item.direction === 'IN' ? '收入' : '支出', item.description, `${item.direction === 'IN' ? '+' : '-'}¥${item.amount}`, new Date(item.createdAt).toLocaleString()]));
        setWithdrawals(withdrawalItems.map((item) => [`¥${item.amount}`, item.status === 'COMPLETED' ? '已到账' : '处理中', new Date(item.createdAt).toLocaleString()]));
        setNotices(notificationItems.map((item) => ({ id: item.id, title: item.title, content: item.content, time: new Date(item.createdAt).toLocaleString(), screen: 'inquiries', read: item.read })));
        setConversations(inquiryItems.map((item) => ({ id: item.id, direction: item.role === 'QUESTIONER' ? 'outgoing' : 'incoming', name: item.otherName, avatar: item.otherAvatar, title: item.topic || item.question, question: item.question, amount: Number(item.amount), inquiryStatus: item.status.toLowerCase(), settlementStatus: item.fundsStatus === 'SETTLED' ? 'settled' : item.fundsStatus.toLowerCase(), unread: 0, partner: { id: item.otherUserId, name: item.otherName, avatar: item.otherAvatar }, messages: [] })));
        setAnswerers(answererItems.filter((item) => item.id !== user.id).map((item) => ({ id: item.id, uid: item.uid, name: item.nickname || `UID ${item.uid}`, avatar: item.avatarUrl || '', acceptingInquiries: item.acceptingInquiries, main: item.mainJob || '-', mainYears: item.mainJobYears || 0, venture: '-', ventureYears: 0, experiences: item.experiences.map((experienceItem) => experienceItem.title), experienceDetails: item.experiences })));
      })
      .catch((error) => {
        if (!active) return;
        if (!getAccessToken()) {
          setIsAuthenticated(false);
          go('login');
        } else notify(error.message);
      });
    return () => { active = false; };
  }, [isAuthenticated]);
  const addNotice = ({ title, content, screen = 'inquiries' }) => {
    setNotices((current) => [
      {
        id: `notice-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
        title,
        content,
        time: '刚刚',
        screen,
        read: false,
      },
      ...current,
    ]);
  };
  const canAnswer = certifications
    .filter((item) => item.required)
    .every((item) => item.status === '已认证');
  const unreadNoticeCount = notices.filter((notice) => !notice.read).length;
  useEffect(() => {
    if (!isAuthenticated && !['login', 'register', 'terms', 'privacy'].includes(screen))
      go('login');
  }, [go, isAuthenticated, screen]);
  const nav = (id) => {
    go(id);
  };
  const showNav = isAuthenticated && !['login', 'register', 'terms', 'privacy'].includes(screen);
  const routeProps = {
    go,
    notify,
    category,
    setCategory,
    problem,
    setProblem,
    experience,
    setExperience,
    talent,
    setTalent,
    certType,
    setCertType,
    certifications,
    setCertifications,
    selectedConversation,
    setSelectedConversation,
    conversations,
    setConversations,
    balance,
    setBalance,
    ledger,
    setLedger,
    withdrawals,
    setWithdrawals,
    accountStats,
    setAccountStats,
    userProfile,
    setUserProfile,
    notices,
    setNotices,
    unreadNoticeCount,
    addNotice,
    feedbackRecords,
    setFeedbackRecords,
    acceptingInquiries,
    setAcceptingInquiries,
    frozenAmount,
    canAnswer,
    isAuthenticated,
    login,
    logout,
    deleteAccount,
    answerers,
  };

  return (
    <div className="app">
      <main>
        <AppRoutes {...routeProps} />
      </main>
      {showNav && <BottomNav active={tab} onChange={nav} inquiryUnreadCount={inquiryUnreadCount} />}
      {toast && (
        <div className="toast">
          <Check size={16} />
          {toast}
        </div>
      )}
    </div>
  );
}

export default App;
