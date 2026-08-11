import { useEffect, useState } from 'react';
import { Bell, ChevronRight, HeartHandshake, Search, Sparkles } from 'lucide-react';
import { talentPool } from '../../data/mockData.js';
import CanvasLogo from '../../components/brand/CanvasLogo.jsx';
import TalentCard from '../../components/talent/TalentCard.jsx';
import './HomePage.css';

export default function HomePage({ go, setTalent, unreadNoticeCount = 0 }) {
  const [slide, setSlide] = useState(0);
  const [count, setCount] = useState(10);
  const banners = [
    [
      '大多数人都会遇到',
      '买房、装修，是生活里绕不开的一环',
      '先问问做过和经历过的人，别稀里糊涂花钱',
    ],
    ['上班总会遇到点糟心事', '离职、裁员，遇到容易慌？', '找经历过的人聊聊，心里就有底了'],
    ['家庭的担子', '照顾老人、孩子成长，没人天生就会', '问问过来人，听听日常里管用的经验'],
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
          <CanvasLogo size={39} />
        </div>
        <div className="brand-name">
          <strong>事先问</strong>
          <small>有事先问问过来人</small>
        </div>
        <button className="round" onClick={() => go('notices')}>
          <Bell size={20} />
          {unreadNoticeCount > 0 && <i className="notice-dot" />}
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
        <button onClick={() => go('knowledge')}>
          <Search size={19} />
          <div>
            <b>按事情</b>
            <small>先看看应该问哪些人</small>
          </div>
          <ChevronRight />
        </button>
        <button onClick={() => go('experiences')}>
          <HeartHandshake size={19} />
          <div>
            <b>按经历</b>
            <small>找真正经历过的人聊聊</small>
          </div>
          <ChevronRight />
        </button>
      </section>
      <section className="talent-section">
        <div className="section-head simple">
          <h2>可以帮你的人</h2>
        </div>
        <div className="talent-list">
          {talentPool.slice(0, count).map((p) => (
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
      <div className="endline">
        {count < talentPool.length ? '继续下滑，看看更多人' : '已经到底啦'}
      </div>
    </>
  );
}
