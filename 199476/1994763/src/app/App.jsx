import { useCallback, useEffect, useRef, useState } from 'react';
import BottomNav from '../components/navigation/BottomNav.jsx';
import GlobalLoading from '../components/feedback/GlobalLoading.jsx';
import Toast from '../components/feedback/Toast.jsx';
import useAppNavigation from '../hooks/useAppNavigation.js';
import useToast from '../hooks/useToast.js';
import useRealtimeConnection from '../hooks/useRealtimeConnection.js';
import AppRoutes from '../routes/AppRoutes.jsx';
import { api, getAccessToken, setAccessToken } from '../api/http.js';
import '../styles/index.css';

const FIXED_BASIC_CERTIFICATIONS = [
  {
    id: 'identity',
    type: '实名认证',
    title: '身份信息',
    description: '身份证正面、反面及手持身份证',
    required: true,
    status: '填写中',
    materials: [],
  },
  {
    id: 'main-job',
    type: '岗位认证',
    title: '我的岗位',
    description: '在职或从业证明、工作年限证明及岗位证明',
    required: true,
    status: '填写中',
    materials: [],
  },
];

function restoreFixedBasicCertifications(items) {
  const currentItems = Array.isArray(items) ? items : [];
  const fixedItems = FIXED_BASIC_CERTIFICATIONS.map((fixedItem) => {
    const existingItem = currentItems.find((item) => item.id === fixedItem.id);
    return existingItem ? { ...fixedItem, ...existingItem, required: true } : fixedItem;
  });
  const experienceItems = currentItems.filter(
    (item) => !FIXED_BASIC_CERTIFICATIONS.some((fixedItem) => fixedItem.id === item.id),
  );
  return [...fixedItems, ...experienceItems];
}

const CERTIFICATION_STATUS = {
  PENDING: '审核中',
  APPROVED: '已认证',
  REJECTED: '退回修改',
};

function certificationFromApi(item) {
  return {
    id: item.type === 'IDENTITY' ? 'identity' : item.type === 'MAIN_JOB' ? 'main-job' : item.id,
    serverId: item.id,
    type: item.type === 'IDENTITY' ? '实名认证' : item.type === 'MAIN_JOB' ? '岗位认证' : '其它经历认证',
    title: item.type === 'IDENTITY'
      ? '身份信息'
      : item.type === 'MAIN_JOB' && item.status !== 'APPROVED'
        ? '我的岗位'
        : item.title,
    name: item.title,
    description: item.description || '',
    detail: item.description || '',
    required: item.required,
    status: CERTIFICATION_STATUS[item.status] || item.status,
    feedback: item.rejectionReason || '',
    materials: (item.materials || []).map((material) => ({
      kind: String(material.kind || '').toLowerCase(),
      name: material.name,
      size: material.size,
      url: material.url,
    })),
  };
}

function inquiryFromApi(item) {
  const latestTime = item.lastMessageAt;
  return {
    id: item.id,
    direction: item.role === 'QUESTIONER' ? 'outgoing' : 'incoming',
    name: item.otherName,
    avatar: item.otherAvatar,
    title: item.topic || item.question,
    question: item.question,
    amount: Number(item.amount),
    inquiryStatus: item.status === 'COMPLETED' ? 'ended' : item.status.toLowerCase(),
    settlementStatus: item.fundsStatus === 'SETTLED' ? 'settled' : item.fundsStatus.toLowerCase(),
    responseDeadline: item.responseDeadline,
    confirmationDeadline: item.confirmationDeadline,
    unread: Number(item.unreadCount || 0),
    lastMessageAt: latestTime,
    time: latestTime ? new Date(latestTime).toLocaleString('zh-CN', {
      month: 'numeric',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }) : '尚未聊天',
    partner: { id: item.otherUserId, name: item.otherName, avatar: item.otherAvatar },
    messages: [],
  };
}

function notificationFromApi(item) {
  const inquiryMatch = String(item.targetPath || '').match(/^\/inquiries\/(\d+)$/);
  return {
    id: item.id,
    title: item.title,
    content: item.content,
    time: item.createdAt ? new Date(item.createdAt).toLocaleString() : '刚刚',
    screen: inquiryMatch ? 'inquiries' : item.targetPath || 'inquiries',
    inquiryId: inquiryMatch ? Number(inquiryMatch[1]) : null,
    read: Boolean(item.read),
  };
}

function mergeInquiryItems(current, items) {
  return items.map((item) => {
    const next = inquiryFromApi(item);
    const existing = current.find((entry) => entry.id === next.id);
    return existing
      ? {
          ...existing,
          ...next,
          messages: existing.messages || [],
          partner: { ...existing.partner, ...next.partner },
        }
      : next;
  });
}

function answererFromApi(item) {
  return {
    id: item.id,
    uid: item.uid,
    name: item.nickname || `UID ${item.uid}`,
    avatar: item.avatarUrl || '',
    acceptingInquiries: item.acceptingInquiries,
    main: item.mainJob || '-',
    mainYears: item.mainJobYears || 0,
    capabilityDescription: item.capabilityDescription || '',
    venture: '-',
    ventureYears: 0,
    experiences: item.experiences.map((experienceItem) => experienceItem.title),
    experienceDetails: item.experiences,
  };
}

Object.keys(localStorage)
  .filter(
    (key) =>
      (key.startsWith('shixianwen-') && key !== 'shixianwen-access-token') ||
      key.startsWith('guangyi-'),
  )
  .forEach((key) => localStorage.removeItem(key));

function App() {
  const { tab, screen, go } = useAppNavigation('login', 'home');
  const [isAuthenticated, setIsAuthenticated] = useState(() => Boolean(getAccessToken()));
  const [category, setCategory] = useState('生活');
  const { toast, notify } = useToast();
  const [problem, setProblem] = useState('');
  const [matterId, setMatterId] = useState(null);
  const [experience, setExperience] = useState('');
  const [experienceCategoryId, setExperienceCategoryId] = useState(null);
  const [discoveryCatalog, setDiscoveryCatalog] = useState({
    categories: [
      { code: 'LIFE', name: '生活', subcategories: [] },
      { code: 'WORK', name: '工作', subcategories: [] },
      { code: 'ENTERTAINMENT', name: '娱乐', subcategories: [] },
    ],
  });
  const [talent, setTalent] = useState(null);
  const [answerers, setAnswerers] = useState([]);
  const [answererPage, setAnswererPage] = useState(0);
  const [answerersHaveMore, setAnswerersHaveMore] = useState(true);
  const loadingAnswerersRef = useRef(false);
  const [certType, setCertType] = useState('');
  const [userProfile, setUserProfile] = useState({
    name: '',
    uid: '',
    phone: '',
    avatar: '',
  });
  const [certifications, setCertifications] = useState(FIXED_BASIC_CERTIFICATIONS);
  const [conversations, setConversations] = useState([]);
  const inquiryUnreadCount = conversations.reduce(
    (total, conversation) => total + Math.max(0, Number(conversation.unread) || 0),
    0,
  );
  const [selectedConversation, setSelectedConversation] = useState(null);
  const [balance, setBalance] = useState(0);
  const [frozenAmount, setFrozenAmount] = useState(0);
  const [ledger, setLedger] = useState([]);
  const [withdrawals, setWithdrawals] = useState([]);
  const [accountStats, setAccountStats] = useState({
    totalWithdrawn: 0,
  });
  const [notices, setNotices] = useState([]);
  const [feedbackRecords, setFeedbackRecords] = useState([]);
  const [acceptingInquiries, setAcceptingInquiries] = useState(false);

  const synchronizeMessaging = useCallback(async () => {
    const [inquiryItems, notificationItems] = await Promise.all([
      api.inquiries(),
      api.notifications(),
    ]);
    setConversations((current) => mergeInquiryItems(current, inquiryItems));
    setNotices(notificationItems.map(notificationFromApi));
  }, []);

  const handleRealtimeEvent = useCallback(async (event) => {
    const payload = event?.payload || {};
    try {
      if (event.type === 'CONNECTED') {
        await synchronizeMessaging();
        return;
      }

      if (event.type === 'NOTIFICATION_CREATED') {
        const inquiryMatch = String(payload.targetPath || '').match(/^\/inquiries\/(\d+)$/);
        const notice = {
          id: payload.id,
          title: payload.title,
          content: payload.content,
          time: '刚刚',
          screen: inquiryMatch ? 'inquiries' : payload.targetPath || 'inquiries',
          inquiryId: inquiryMatch ? Number(inquiryMatch[1]) : null,
          read: false,
        };
        setNotices((current) => [
          notice,
          ...current.filter((item) => item.id !== notice.id),
        ]);
        notify(payload.title, 'default');
        return;
      }

      if (event.type === 'NOTIFICATION_READ') {
        setNotices((current) => current.map((item) => (
          item.id === payload.id ? { ...item, read: true } : item
        )));
        return;
      }

      if (event.type === 'NOTIFICATIONS_READ_ALL') {
        setNotices((current) => current.map((item) => ({ ...item, read: true })));
        return;
      }

      if (event.type === 'INQUIRY_READ') {
        setConversations((current) => current.map((item) => (
          item.id === payload.inquiryId ? { ...item, unread: 0 } : item
        )));
        return;
      }

      if (event.type === 'INQUIRY_UPDATED') {
        const inquiryItems = await api.inquiries();
        setConversations((current) => mergeInquiryItems(current, inquiryItems));
        const selectedItem = inquiryItems.find((item) => item.id === selectedConversation?.id);
        if (selectedItem) {
          const nextSelected = inquiryFromApi(selectedItem);
          setSelectedConversation((current) => ({
            ...current,
            ...nextSelected,
            messages: current?.messages || [],
            partner: { ...current?.partner, ...nextSelected.partner },
          }));
        }
        return;
      }

      if (event.type === 'INQUIRY_MESSAGE') {
        const incomingMessage = {
          id: payload.message.id,
          name: payload.message.senderName,
          text: payload.message.content,
          avatar: payload.message.senderAvatar,
          me: false,
          createdAt: payload.message.createdAt,
          time: payload.message.createdAt
            ? new Date(payload.message.createdAt).toLocaleTimeString([], {
                hour: '2-digit',
                minute: '2-digit',
              })
            : '刚刚',
          attachment: payload.message.attachmentUrl ? {
            url: payload.message.attachmentUrl,
            name: payload.message.attachmentName || '照片',
            type: payload.message.type === 'IMAGE' ? 'image/*' : '',
          } : null,
        };
        const isOpenConversation = screen === 'directChat'
          && selectedConversation?.id === payload.inquiryId;

        setConversations((current) => current.map((item) => {
          if (item.id !== payload.inquiryId) return item;
          const messageExists = (item.messages || []).some((entry) => entry.id === incomingMessage.id);
          return {
            ...item,
            time: '刚刚',
            lastMessageAt: payload.message.createdAt,
            unread: isOpenConversation ? 0 : Number(payload.unreadCount || 0),
            messages: messageExists
              ? item.messages
              : [...(item.messages || []), incomingMessage],
          };
        }));
        setSelectedConversation((current) => {
          if (current?.id !== payload.inquiryId) return current;
          const messageExists = (current.messages || []).some((entry) => entry.id === incomingMessage.id);
          return {
            ...current,
            unread: isOpenConversation ? 0 : Number(payload.unreadCount || 0),
            messages: messageExists
              ? current.messages
              : [...(current.messages || []), incomingMessage],
          };
        });

        if (isOpenConversation) {
          await api.markInquiryRead(payload.inquiryId);
        }
      }
    } catch (error) {
      notify(error.message, 'error');
    }
  }, [notify, screen, selectedConversation?.id, synchronizeMessaging]);

  useRealtimeConnection(isAuthenticated, handleRealtimeEvent);

  useEffect(() => {
    const handleUnauthorized = () => {
      setIsAuthenticated(false);
      go('login');
    };

    window.addEventListener('shixianwen-unauthorized', handleUnauthorized);
    return () => window.removeEventListener('shixianwen-unauthorized', handleUnauthorized);
  }, [go]);

  useEffect(() => {
    setCertifications((current) => {
      const submittedItems = current.filter((item) => !(item.isNew && item.status === '填写中'));
      const restoredItems = restoreFixedBasicCertifications(submittedItems);
      return JSON.stringify(restoredItems) === JSON.stringify(current) ? current : restoredItems;
    });
  }, [setCertifications]);

  const login = ({ token, user }) => {
    setAccessToken(token);
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
    setIsAuthenticated(false);
    go('login');
  };
  const deleteAccount = async () => {
    try {
      await api.deleteAccount();
    } catch (error) {
      notify(error.message, 'error');
      return;
    }
    setProblem('');
    setMatterId(null);
    setExperience('');
    setExperienceCategoryId(null);
    setTalent(null);
    setCertType('');
    setUserProfile({
      name: '',
      uid: '',
      phone: '',
      avatar: '',
    });
    setCertifications(FIXED_BASIC_CERTIFICATIONS);
    setConversations([]);
    setSelectedConversation(null);
    setBalance(0);
    setFrozenAmount(0);
    setLedger([]);
    setWithdrawals([]);
    setAccountStats({ totalWithdrawn: 0 });
    setNotices([]);
    setFeedbackRecords([]);
    setAcceptingInquiries(false);
    setAccessToken('');
    setIsAuthenticated(false);
    go('login');
  };

  const refreshDiscoveryCatalog = useCallback(async (mainCategory, content) => {
    if (!mainCategory || !content) return null;
    try {
      setDiscoveryCatalog((current) => ({
        categories: current.categories.map((item) =>
          item.code === mainCategory ? { ...item, subcategories: [] } : item,
        ),
      }));
      const loadedCategory = content === 'MATTERS'
        ? await api.matterCategories(mainCategory)
        : await api.experienceCategories(mainCategory);
      if (loadedCategory) {
        setDiscoveryCatalog((current) => ({
          categories: current.categories.map((item) =>
            item.code === loadedCategory.code ? loadedCategory : item,
          ),
        }));
      }
      return loadedCategory;
    } catch (error) {
      notify(error.message, 'error');
      return null;
    }
  }, [notify]);

  useEffect(() => {
    if (!isAuthenticated) return undefined;
    let active = true;
    api.me()
      .then((user) => {
        if (!active) return;
        setUserProfile({ id: user.id, name: user.nickname || '', uid: user.uid, phone: user.phone, avatar: user.avatarUrl || '' });
        setAcceptingInquiries(user.acceptingInquiries);
      })
      .catch((error) => {
        if (!active) return;
        if (!getAccessToken()) {
          setIsAuthenticated(false);
          go('login');
        } else notify(error.message, 'error');
      });
    return () => { active = false; };
  }, [isAuthenticated]);
  const refreshWallet = useCallback(async () => {
    const wallet = await api.wallet();
    setBalance(Number(wallet.availableBalance));
    setFrozenAmount(Number(wallet.frozenBalance));
    setAccountStats((current) => ({
      ...current,
      totalWithdrawn: Number(wallet.totalWithdrawn),
    }));
  }, []);
  const refreshCurrentScreen = useCallback(async () => {
    if (!getAccessToken()) return;
    if (screen === 'home') {
      const [answererResult, notificationItems] = await Promise.all([
        api.answerers(0, 10), api.notifications(),
      ]);
      setAnswerers(answererResult.items.map(answererFromApi));
      setAnswererPage(0);
      setAnswerersHaveMore(answererResult.hasMore);
      setNotices(notificationItems.map(notificationFromApi));
      return;
    }
    if (['certs', 'certWork', 'certExperience', 'certBasicApply', 'certExperienceApply'].includes(screen)) {
      const items = await api.certifications();
      setCertifications(restoreFixedBasicCertifications(items.map(certificationFromApi)));
      return;
    }
    if (screen === 'wallet') {
      const [wallet, transactions, withdrawalItems, bankCard] = await Promise.all([
        api.wallet(), api.walletTransactions(), api.withdrawals(), api.bankCard(),
      ]);
      setBalance(Number(wallet.availableBalance));
      setFrozenAmount(Number(wallet.frozenBalance));
      setAccountStats({ totalWithdrawn: Number(wallet.totalWithdrawn), bankCard: bankCard ? { holderName: bankCard.holderName, bankName: bankCard.bankName, cardNumber: bankCard.lastFour } : null });
      setLedger(transactions.map((item) => [item.direction === 'IN' ? '收入' : '支出', item.description, `${item.direction === 'IN' ? '+' : '-'}¥${item.amount}`, new Date(item.createdAt).toLocaleString()]));
      setWithdrawals(withdrawalItems.map((item) => [`¥${item.amount}`, item.status === 'COMPLETED' ? '已到账' : '处理中', new Date(item.createdAt).toLocaleString()]));
      return;
    }
    if (screen === 'inquiries') {
      setConversations((await api.inquiries()).map(inquiryFromApi));
      return;
    }
    if (screen === 'talent') {
      const [inquiryItems, wallet] = await Promise.all([api.inquiries(), api.wallet()]);
      setConversations(inquiryItems.map(inquiryFromApi));
      setBalance(Number(wallet.availableBalance));
      setFrozenAmount(Number(wallet.frozenBalance));
      return;
    }
    if (screen === 'feedback') {
      const [feedbackItems, inquiryItems] = await Promise.all([api.feedbackRecords(), api.inquiries()]);
      setFeedbackRecords(feedbackItems.map((item) => ({ id: item.id, type: item.type, category: item.category, content: item.content, status: item.status, time: new Date(item.createdAt).toLocaleString() })));
      setConversations(inquiryItems.map(inquiryFromApi));
      return;
    }
    if (screen === 'notices') {
      setNotices((await api.notifications()).map(notificationFromApi));
      return;
    }
    if (screen === 'accountSettings') {
      const [user, wallet, inquiryItems] = await Promise.all([api.me(), api.wallet(), api.inquiries()]);
      setUserProfile({ id: user.id, name: user.nickname || '', uid: user.uid, phone: user.phone, avatar: user.avatarUrl || '' });
      setAcceptingInquiries(user.acceptingInquiries);
      setBalance(Number(wallet.availableBalance));
      setFrozenAmount(Number(wallet.frozenBalance));
      setConversations(inquiryItems.map(inquiryFromApi));
      return;
    }
    if (screen === 'profile') {
      const user = await api.me();
      setUserProfile({ id: user.id, name: user.nickname || '', uid: user.uid, phone: user.phone, avatar: user.avatarUrl || '' });
      setAcceptingInquiries(user.acceptingInquiries);
    }
  }, [screen]);
  const loadMoreAnswerers = useCallback(async () => {
    if (!answerersHaveMore || loadingAnswerersRef.current) return;
    loadingAnswerersRef.current = true;
    const nextPage = answererPage + 1;
    try {
      const result = await api.answerers(nextPage, 10);
      setAnswerers((current) => {
        const known = new Set(current.map((item) => item.id));
        return [...current, ...result.items.filter((item) => !known.has(item.id)).map(answererFromApi)];
      });
      setAnswererPage(nextPage);
      setAnswerersHaveMore(result.hasMore);
    } finally {
      loadingAnswerersRef.current = false;
    }
  }, [answererPage, answerersHaveMore]);

  useEffect(() => {
    if (!isAuthenticated) return undefined;
    refreshCurrentScreen().catch((error) => notify(error.message, 'error'));
    return undefined;
  }, [isAuthenticated, notify, refreshCurrentScreen, screen]);
  const canAnswer = certifications
    .filter((item) => item.required)
    .every((item) => item.status === '已认证');
  const changeAcceptingInquiries = async (valueOrUpdater) => {
    const next = typeof valueOrUpdater === 'function'
      ? valueOrUpdater(acceptingInquiries)
      : Boolean(valueOrUpdater);
    if (!canAnswer) {
      notify('完成基础信息认证后才能接受询问', 'warning');
      return;
    }
    try {
      await api.setAcceptingInquiries(next);
      const user = await api.me();
      setAcceptingInquiries(user.acceptingInquiries);
    } catch (error) {
      notify(error.message, 'error');
    }
  };
  const unreadNoticeCount = notices.filter((notice) => !notice.read).length;
  useEffect(() => {
    if (!isAuthenticated && !['login', 'register', 'terms', 'privacy'].includes(screen))
      go('login');
  }, [go, isAuthenticated, screen]);
  const nav = (id) => {
    go(id);
  };
  const showNav = isAuthenticated
    && !['login', 'register', 'terms', 'privacy', 'directChat'].includes(screen);
  const routeProps = {
    go,
    notify,
    category,
    setCategory,
    problem,
    setProblem,
    matterId,
    setMatterId,
    experience,
    setExperience,
    experienceCategoryId,
    setExperienceCategoryId,
    discoveryCatalog,
    refreshDiscoveryCatalog,
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
    feedbackRecords,
    setFeedbackRecords,
    acceptingInquiries,
    setAcceptingInquiries: changeAcceptingInquiries,
    frozenAmount,
    refreshWallet,
    refreshCurrentScreen,
    canAnswer,
    isAuthenticated,
    login,
    logout,
    deleteAccount,
    answerers,
    answerersHaveMore,
    loadMoreAnswerers,
  };

  return (
    <div className={`app ${showNav ? 'has-bottom-nav' : ''}`}>
      <main>
        <AppRoutes {...routeProps} />
      </main>
      {showNav && <BottomNav active={tab} onChange={nav} inquiryUnreadCount={inquiryUnreadCount} />}
      <Toast content={toast} />
      <GlobalLoading />
    </div>
  );
}

export default App;
