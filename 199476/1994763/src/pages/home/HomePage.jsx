import { useEffect, useState } from 'react';
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
import { talentPool } from '../../data/mockData.js';
import TalentCard from '../../components/talent/TalentCard.jsx';
import './HomePage.css';

export default function HomePage({ go, setTalent }) {
  const [slide, setSlide] = useState(0);
  const [count, setCount] = useState(10);
  const banners = [
    ['家里突然出问题', '跳闸、漏水，不知道该先找谁？', '找做过相关工作的人问问，先把原因弄清楚'],
    ['准备装修房子', '水电、设计、施工，一堆事理不清', '把相关岗位的人叫到一个群里，一起聊明白'],
    ['遇到劳动纠纷', '材料怎么准备，下一步该做什么？', '问问有相关工作或亲身经历的人，少走弯路'],
  ];
  useEffect(() => {
    const t = setInterval(() => setSlide((x) => (x + 1) % banners.length), 10000);
    const scroll = () => {
      if (innerHeight + scrollY >= document.body.offsetHeight - 180)
        setCount((x) => Math.min(x + 10, talentPool.length));
    };
    addEventListener('scroll', scroll);
    return () => {
      clearInterval(t);
      removeEventListener('scroll', scroll);
    };
  }, []);
  return (
    <>
      <header className="brand-top">
        <div className="brand-logo">
          <HeartHandshake />
        </div>
        <strong>光忆</strong>
        <button className="round" onClick={() => go('notices')}>
          <Bell size={20} />
          <i className="notice-dot" />
        </button>
      </header>
      <section className="intro-section">
        <div className="pt15">
          <div className="intro-banner carousel">
            <div>
              <span>
                <Sparkles size={13} /> {banners[slide][0]}
              </span>
              <h2>{banners[slide][1]}</h2>
              <p>{banners[slide][2]}</p>
            </div>
            <HeartHandshake size={73} />
            <nav>
              {banners.map((_, i) => (
                <button
                  className={slide === i ? 'active' : ''}
                  onClick={() => setSlide(i)}
                  key={i}
                />
              ))}
            </nav>
          </div>
        </div>
      </section>
      <section className="home-actions">
        <button onClick={() => go('filter')}>
          <SlidersHorizontal size={19} />
          <div>
            <b>按类别</b>
            <small>按岗位和时间找人</small>
          </div>
          <ChevronRight />
        </button>
        <button onClick={() => go('knowledge')}>
          <Search size={19} />
          <div>
            <b>按问题</b>
            <small>先选择一个具体问题</small>
          </div>
          <ChevronRight />
        </button>
      </section>
      <section className="talent-section">
        <div className="section-head simple">
          <h2>可以帮你的人</h2>
        </div>
        <div className="talent-list">
          {talentPool.slice(0, count).map((p, i) => (
            <TalentCard
              key={p.uid}
              p={p}
              onClick={() => {
                setTalent(p);
                go('talent');
              }}
              index={i}
            />
          ))}
        </div>
      </section>
      <div className="endline">
        {count < talentPool.length ? '继续下滑，看看更多人' : '已经到底啦'}
      </div>
    </>
  );
}
