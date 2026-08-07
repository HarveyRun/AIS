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
import Page from '../../components/layout/Page.jsx';
import { people } from '../../data/mockData.js';
import TalentCard from '../../components/talent/TalentCard.jsx';
import './DiscoveryPages.css';

export default function FilterPage({
  go,
  setTalent,
  problem = '',
  title = '按类别筛选',
  backScreen = 'home',
}) {
  const [keyword, setKeyword] = useState('');
  const [services, setServices] = useState(['周六']);
  const [drawer, setDrawer] = useState(false);
  const [industry, setIndustry] = useState('全部行业');
  const [district, setDistrict] = useState('全部区县');
  const [years, setYears] = useState('不限工龄');
  const [certification, setCertification] = useState('全部认证');
  const toggle = (s) =>
    setServices(services.includes(s) ? services.filter((x) => x !== s) : [...services, s]);
  const filtered = people.filter((person) => {
    const searchableWork = [person.main, person.side, person.venture].join('');
    const problemKeywords = problem
      .replace(/经验|维修|设计|管理|使用|沟通|事务/g, '')
      .split(/[与和、]/)
      .filter(Boolean);
    const matchesProblem =
      !problem ||
      problemKeywords.some((word) => searchableWork.includes(word)) ||
      (['水电', '装修', '房屋', '家具', '收纳', '买房', '租房'].some((word) =>
        problem.includes(word),
      ) &&
        ['建筑与装修', '生活服务'].includes(person.industry)) ||
      (['程序', '产品', '软件', '设备', '网络'].some((word) => problem.includes(word)) &&
        person.industry === '互联网与软件');
    const matchesKeyword =
      !keyword ||
      [person.main, person.side, person.venture, person.name].join(' ').includes(keyword);
    const matchesService =
      !services.length || services.some((service) => person.serviceModes.includes(service));
    const matchesIndustry = industry === '全部行业' || person.industry === industry;
    const matchesDistrict = district === '全部区县' || person.district === district;
    const matchesYears = years === '不限工龄' || person.mainYears >= 8;
    const matchesCertification = certification === '全部认证' || Boolean(person.main);
    return (
      matchesProblem &&
      matchesKeyword &&
      matchesService &&
      matchesIndustry &&
      matchesDistrict &&
      matchesYears &&
      matchesCertification
    );
  });
  const resetFilters = () => {
    setIndustry('全部行业');
    setDistrict('全部区县');
    setYears('不限工龄');
    setCertification('全部认证');
    setServices(['周六']);
  };
  return (
    <Page title={title} back={() => go(backScreen)}>
      {problem && (
        <section className="result-title compact">
          <span>你想解决</span>
          <h2>{problem}</h2>
          <p>还可以继续按时间、行业和地区缩小范围</p>
        </section>
      )}
      <section className="filter-panel">
        <div className="searchbox">
          <Search size={18} />
          <input
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="搜索岗位或昵称"
          />
        </div>
        <div className="filter-label primary">
          <Clock3 />
          服务时间{' '}
          <small onClick={() => setDrawer(true)}>
            <SlidersHorizontal />
          </small>
        </div>
        <div className="visible-times">
          {['工作日晚间', '周六', '周日'].map((x) => (
            <button
              className={services.includes(x) ? 'active' : ''}
              onClick={() => toggle(x)}
              key={x}
            >
              <b>{x}</b>
              <small>{x === '工作日晚间' ? '19:00—23:00' : '10:00—23:00'}</small>
            </button>
          ))}
        </div>
      </section>
      {drawer && (
        <>
          <button className="drawer-mask" onClick={() => setDrawer(false)} />
          <aside className="filter-drawer">
            <header>
              <h2>更多选择</h2>
              <button onClick={() => setDrawer(false)}>
                <X />
              </button>
            </header>
            <label>行业</label>
            <select value={industry} onChange={(e) => setIndustry(e.target.value)}>
              <option>全部行业</option>
              <option>建筑与装修</option>
              <option>互联网与软件</option>
              <option>生活服务</option>
              <option>教育培训</option>
            </select>
            <label>省 / 市 / 区县</label>
            <div className="filter-chips three">
              <select>
                <option>浙江省</option>
              </select>
              <select>
                <option>杭州市</option>
              </select>
              <select value={district} onChange={(event) => setDistrict(event.target.value)}>
                <option>全部区县</option>
                <option>西湖区</option>
                <option>拱墅区</option>
                <option>滨江区</option>
                <option>余杭区</option>
                <option>上城区</option>
              </select>
            </div>
            <label>工龄与认证</label>
            <div className="filter-chips">
              <select value={years} onChange={(event) => setYears(event.target.value)}>
                <option>不限工龄</option>
                <option>8年以上</option>
              </select>
              <select
                value={certification}
                onChange={(event) => setCertification(event.target.value)}
              >
                <option>全部认证</option>
                <option>主职认证</option>
              </select>
            </div>
            <footer>
              <button onClick={resetFilters}>重置</button>
              <button onClick={() => setDrawer(false)}>查看 {filtered.length} 人</button>
            </footer>
          </aside>
        </>
      )}
      <section className="talent-section results">
        <div className="section-head simple">
          <h2>找到这些人</h2>
          <small>{filtered.length}人</small>
        </div>
        <div className="talent-list">
          {filtered.map((p) => (
            <TalentCard
              key={p.uid}
              p={p}
              onClick={() => {
                setTalent(p);
                go('talent');
              }}
            />
          ))}
        </div>
      </section>
    </Page>
  );
}
