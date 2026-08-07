import { useEffect, useState } from 'react';
import { Check } from 'lucide-react';
import BottomNav from '../components/navigation/BottomNav.jsx';
import useAppNavigation from '../hooks/useAppNavigation.js';
import useToast from '../hooks/useToast.js';
import { groupMessages, people } from '../data/mockData.js';
import AppRoutes from '../routes/AppRoutes.jsx';
import '../styles/index.css';

function App() {
  const { tab, screen, go } = useAppNavigation('login', 'home');
  const [isAuthenticated, setIsAuthenticated] = useState(
    () => sessionStorage.getItem('guangyi-authenticated') === 'true',
  );
  const [selected, setSelected] = useState([]);
  const [category, setCategory] = useState('生活');
  const { toast, notify } = useToast();
  const [problem, setProblem] = useState('');
  const [talent, setTalent] = useState(people[0]);
  const [matter, setMatter] = useState({
    title: '旧房装修协助',
    desc: '准备装修一套旧房，希望先把施工顺序、空间布局和水电风险梳理清楚。',
  });
  const [helpers, setHelpers] = useState([people[3], people[1]]);
  const [certType, setCertType] = useState('');
  const [groups, setGroups] = useState(groupMessages);
  const [selectedGroup, setSelectedGroup] = useState(groupMessages[0]);
  const [hourlyFee, setHourlyFee] = useState(60);
  const [balance, setBalance] = useState(2680);
  const [userSchedule, setUserSchedule] = useState({ days: ['周三'], sat: true, sun: false });
  const [ledger, setLedger] = useState([
    ['支出', '旧房装修群预付', '-¥120.00', '今天 19:02'],
    ['收入', '家庭网络改善服务收益', '+¥80.00', '昨天 22:30'],
    ['支出', '劳动争议材料梳理', '-¥180.00', '8月5日'],
    ['收入', '服务费结算', '+¥144.00', '8月3日'],
  ]);
  const [withdrawals, setWithdrawals] = useState([
    ['¥800.00', '处理中', '今天 10:30'],
    ['¥1,200.00', '已到账', '7月28日'],
    ['¥1,200.00', '已到账', '7月12日'],
  ]);
  const login = () => {
    sessionStorage.setItem('guangyi-authenticated', 'true');
    setIsAuthenticated(true);
    go('home');
  };
  const logout = () => {
    sessionStorage.removeItem('guangyi-authenticated');
    setIsAuthenticated(false);
    go('login');
  };
  useEffect(() => {
    const handler = (e) => {
      const button = e.target.closest?.('button.round');
      if (button?.querySelector('.lucide-bell')) go('notices');
    };
    document.addEventListener('click', handler);
    return () => document.removeEventListener('click', handler);
  }, []);
  useEffect(() => {
    if (!isAuthenticated && !['login', 'register'].includes(screen)) go('login');
  }, [go, isAuthenticated, screen]);
  const nav = (id) => go(id, id);
  const showNav = isAuthenticated && !['login', 'register'].includes(screen);
  const unreadCount = groups.reduce((total, message) => total + message.unread, 0);
  const routeProps = {
    go,
    notify,
    selected,
    setSelected,
    category,
    setCategory,
    problem,
    setProblem,
    talent,
    setTalent,
    matter,
    setMatter,
    helpers,
    setHelpers,
    certType,
    setCertType,
    selectedGroup,
    setSelectedGroup,
    groups,
    setGroups,
    hourlyFee,
    setHourlyFee,
    balance,
    setBalance,
    userSchedule,
    setUserSchedule,
    ledger,
    setLedger,
    withdrawals,
    setWithdrawals,
    isAuthenticated,
    login,
    logout,
  };
  return (
    <div className="app">
      <main>
        <AppRoutes {...routeProps} />
      </main>
      {showNav && <BottomNav active={tab} onChange={nav} unreadCount={unreadCount} />}
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
