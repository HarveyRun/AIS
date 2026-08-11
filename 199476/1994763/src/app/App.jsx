import { useEffect, useState } from 'react';
import { Check } from 'lucide-react';
import BottomNav from '../components/navigation/BottomNav.jsx';
import useAppNavigation from '../hooks/useAppNavigation.js';
import useDirectInquirySettlement from '../hooks/useDirectInquirySettlement.js';
import useToast from '../hooks/useToast.js';
import usePersistentState from '../hooks/usePersistentState.js';
import { conversationMessages, people } from '../data/mockData.js';
import AppRoutes from '../routes/AppRoutes.jsx';
import '../styles/index.css';

function App() {
  const { tab, screen, go } = useAppNavigation('login', 'home');
  const [isAuthenticated, setIsAuthenticated] = useState(
    () =>
      sessionStorage.getItem('shixianwen-authenticated') === 'true' ||
      sessionStorage.getItem('guangyi-authenticated') === 'true',
  );
  const [category, setCategory] = usePersistentState('shixianwen-category', '生活');
  const { toast, notify } = useToast();
  const [problem, setProblem] = usePersistentState('shixianwen-problem', '');
  const [experience, setExperience] = usePersistentState('shixianwen-experience', '');
  const [talent, setTalent] = usePersistentState('shixianwen-selected-talent', people[0]);
  const [certType, setCertType] = usePersistentState('shixianwen-cert-type', '');
  const [userProfile, setUserProfile] = usePersistentState('shixianwen-user-profile', {
    name: '',
    uid: '1000286',
    phone: '',
    avatar: '/images/avatar1.jpg',
  });
  const [certifications, setCertifications] = usePersistentState('shixianwen-certifications', [
    {
      id: 'identity',
      type: '实名认证',
      title: '实名认证',
      description: '身份证正面、反面及手持身份证',
      required: true,
      status: '已认证',
      name: '安然',
      detail: '本人身份认证',
      materials: [
        { kind: 'image', name: '身份证正面.png', size: 56205, url: '/images/img.png' },
        { kind: 'image', name: '身份证反面.png', size: 56205, url: '/images/img.png' },
        { kind: 'image', name: '手持身份证.png', size: 56205, url: '/images/img.png' },
      ],
    },
    {
      id: 'main-job',
      type: '岗位认证',
      title: '我的岗位',
      description: '软件工程师 · 11年经历',
      required: true,
      status: '已认证',
      name: '软件工程师',
      detail: '从事软件开发工作11年，主要负责应用开发。',
      materials: [
        { kind: 'image', name: '在职证明.png', size: 56205, url: '/images/img.png' },
        { kind: 'image', name: '工作年限证明.png', size: 56205, url: '/images/img.png' },
        { kind: 'image', name: '岗位证明.png', size: 56205, url: '/images/img.png' },
      ],
    },
    {
      id: 'experience-startup',
      type: '其它经历认证',
      title: '创过业',
      description: '个人创业经历',
      required: false,
      status: '已认证',
      name: '创过业',
      detail: '曾独立经营项目。',
      materials: [
        {
          kind: 'archive',
          name: '创业经历.zip',
          size: 22,
          url: 'data:application/zip;base64,UEsFBgAAAAAAAAAAAAAAAAAAAAAAAA==',
        },
        {
          kind: 'video',
          name: '创业经历录像.mp4',
          size: 947184,
          url: '/images/video.mp4',
        },
        {
          kind: 'image',
          name: '创业经历照片.png',
          size: 56205,
          url: '/images/img.png',
        },
      ],
    },
    {
      id: 'experience-arbitration',
      type: '其它经历认证',
      title: '经历过劳动仲裁',
      description: '亲自处理过劳动仲裁申请',
      required: false,
      status: '已认证',
      name: '经历过劳动仲裁',
      detail: '亲自准备材料并完成劳动仲裁流程。',
      materials: [
        {
          kind: 'archive',
          name: '劳动仲裁经历.zip',
          size: 194,
          url: '/images/ysb.zip',
        },
      ],
    },
  ]);
  const [conversations, setConversations] = usePersistentState(
    'shixianwen-conversations',
    conversationMessages,
  );
  const inquiryUnreadCount = conversations.reduce(
    (total, conversation) => total + Math.max(0, Number(conversation.unread) || 0),
    0,
  );
  const [selectedConversation, setSelectedConversation] = usePersistentState(
    'shixianwen-selected-conversation',
    () => conversations[0] || null,
  );
  const [balance, setBalance] = usePersistentState('shixianwen-balance', 2680);
  const [ledger, setLedger] = usePersistentState('shixianwen-ledger', [
    ['支出', '二手房经历询问', '-¥120.00', '今天 19:02'],
    ['收入', '家庭网络经历解答', '+¥80.00', '昨天 22:30'],
    ['支出', '劳动仲裁经历询问', '-¥180.00', '8月5日'],
    ['收入', '询问费用结算', '+¥144.00', '8月3日'],
  ]);
  const [withdrawals, setWithdrawals] = usePersistentState('shixianwen-withdrawals', [
    ['¥800.00', '处理中', '今天 10:30'],
    ['¥1,200.00', '已到账', '7月28日'],
    ['¥1,200.00', '已到账', '7月12日'],
  ]);
  const [accountStats, setAccountStats] = usePersistentState('shixianwen-account-stats', {
    totalWithdrawn: 3200,
  });
  const [notices, setNotices] = usePersistentState('shixianwen-notices', [
    {
      id: 'notice-inquiry',
      title: '新的询问',
      content: '有人向你发起了一次经历询问',
      time: '刚刚',
      screen: 'inquiries',
      read: false,
    },
    {
      id: 'notice-accepted',
      title: '询问已接受',
      content: '许知行接受了你的询问，现在可以开始聊了',
      time: '20分钟前',
      screen: 'inquiries',
      read: false,
    },
    {
      id: 'notice-certification',
      title: '认证消息',
      content: '你提交的人生经历材料正在核对中',
      time: '昨天',
      screen: 'certs',
      read: false,
    },
  ]);
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

  const login = (profile = {}) => {
    sessionStorage.setItem('shixianwen-authenticated', 'true');
    if (profile.phone || profile.name !== undefined) {
      setUserProfile((current) => ({
        ...current,
        ...profile,
        name:
          Object.prototype.hasOwnProperty.call(profile, 'name') && profile.name !== undefined
            ? profile.name.trim()
            : current.name || '',
      }));
    }
    setIsAuthenticated(true);
    go('home');
  };
  const logout = () => {
    sessionStorage.removeItem('shixianwen-authenticated');
    sessionStorage.removeItem('guangyi-authenticated');
    setIsAuthenticated(false);
    go('login');
  };
  const deleteAccount = () => {
    setProblem('');
    setExperience('');
    setTalent(people[0]);
    setCertType('');
    setUserProfile({
      name: '',
      uid: '1000286',
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
    setIsAuthenticated(false);
    go('login');
  };
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
